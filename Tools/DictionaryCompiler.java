import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.zip.Deflater;

public final class DictionaryCompiler {
    private static final byte[] MAGIC = "IZHDICT2".getBytes(StandardCharsets.US_ASCII);
    private static final int VERSION = 1;
    private static final int WORDS_PER_BLOCK = 256;
    private static final int CODEC_RAW = 0;
    private static final int CODEC_DEFLATE = 1;
    private static final Locale RUSSIAN_LOCALE = Locale.forLanguageTag("ru-RU");

    private DictionaryCompiler() {
    }

    public static void main(String[] arguments) throws IOException {
        if (arguments.length != 3) {
            throw new IllegalArgumentException(
                "Использование: DictionaryCompiler <russian.txt> "
                    + "<russian_surnames.txt> <russian.bin>"
            );
        }

        List<Section> sections = List.of(
            compileSection(0, Path.of(arguments[0])),
            compileSection(1, Path.of(arguments[1]))
        );
        byte[] output = makeFile(sections);

        Files.write(Path.of(arguments[2]), output);

        long wordCount = sections.stream()
            .mapToLong(section -> section.wordCount)
            .sum();
        System.out.println("Записано слов: " + wordCount);
        System.out.println("Размер двоичного словаря: " + output.length + " байт");
    }

    private static Section compileSection(
        int identifier,
        Path sourcePath
    ) throws IOException {
        List<Block> blocks = new ArrayList<>();
        List<byte[]> currentWords = new ArrayList<>(WORDS_PER_BLOCK);
        byte[] previousWord = null;
        int wordCount = 0;

        try (BufferedReader reader = Files.newBufferedReader(
            sourcePath,
            StandardCharsets.UTF_8
        )) {
            String line;

            while ((line = reader.readLine()) != null) {
                byte[] word = line
                    .toLowerCase(RUSSIAN_LOCALE)
                    .getBytes(StandardCharsets.UTF_8);

                if (word.length == 0) {
                    continue;
                }

                if (previousWord != null) {
                    int comparison = compareUnsigned(previousWord, word);

                    if (comparison == 0) {
                        continue;
                    }

                    if (comparison > 0) {
                        throw new IllegalArgumentException(
                            "Файл не упорядочен: "
                                + new String(previousWord, StandardCharsets.UTF_8)
                                + " находится перед "
                                + new String(word, StandardCharsets.UTF_8)
                        );
                    }
                }

                currentWords.add(word);
                previousWord = word;
                wordCount += 1;

                if (currentWords.size() == WORDS_PER_BLOCK) {
                    blocks.add(compileBlock(currentWords));
                    currentWords = new ArrayList<>(WORDS_PER_BLOCK);
                }
            }
        }

        if (!currentWords.isEmpty()) {
            blocks.add(compileBlock(currentWords));
        }

        return new Section(identifier, blocks, wordCount);
    }

    private static Block compileBlock(List<byte[]> words) throws IOException {
        ByteArrayOutputStream encoded = new ByteArrayOutputStream();
        byte[] previousWord = new byte[0];

        for (byte[] word : words) {
            int prefixLength = commonPrefixLength(previousWord, word);
            int suffixLength = word.length - prefixLength;

            writeVariableLengthInteger(encoded, prefixLength);
            writeVariableLengthInteger(encoded, suffixLength);
            encoded.write(word, prefixLength, suffixLength);
            previousWord = word;
        }

        byte[] rawPayload = encoded.toByteArray();
        byte[] compressedPayload = deflate(rawPayload);
        boolean usesCompression = compressedPayload.length < rawPayload.length;

        return new Block(
            words.get(0),
            usesCompression ? compressedPayload : rawPayload,
            rawPayload.length,
            words.size(),
            usesCompression ? CODEC_DEFLATE : CODEC_RAW
        );
    }

    private static byte[] deflate(byte[] source) {
        Deflater deflater = new Deflater(Deflater.BEST_COMPRESSION);
        deflater.setInput(source);
        deflater.finish();

        byte[] buffer = new byte[Math.max(source.length, 64)];
        int length = deflater.deflate(buffer);
        deflater.end();

        return Arrays.copyOf(buffer, length);
    }

    private static byte[] makeFile(List<Section> sections) throws IOException {
        int fixedHeaderSize = MAGIC.length + Integer.BYTES + Integer.BYTES;
        int descriptorSize = Integer.BYTES + Integer.BYTES + Long.BYTES;
        int headerSize = fixedHeaderSize + sections.size() * descriptorSize;
        List<Integer> indexOffsets = new ArrayList<>(sections.size());
        int nextIndexOffset = headerSize;

        for (Section section : sections) {
            indexOffsets.add(nextIndexOffset);
            nextIndexOffset += section.indexSize();
        }

        long nextPayloadOffset = nextIndexOffset;
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        LittleEndianWriter writer = new LittleEndianWriter(bytes);

        bytes.write(MAGIC);
        writer.writeInt(VERSION);
        writer.writeInt(sections.size());

        for (int index = 0; index < sections.size(); index += 1) {
            Section section = sections.get(index);
            writer.writeInt(section.identifier);
            writer.writeInt(section.blocks.size());
            writer.writeLong(indexOffsets.get(index));
        }

        for (Section section : sections) {
            for (Block block : section.blocks) {
                writer.writeShort(block.firstWord.length);
                bytes.write(block.firstWord);
                writer.writeLong(nextPayloadOffset);
                writer.writeInt(block.payload.length);
                writer.writeInt(block.uncompressedLength);
                writer.writeShort(block.wordCount);
                bytes.write(block.codec);
                nextPayloadOffset += block.payload.length;
            }
        }

        for (Section section : sections) {
            for (Block block : section.blocks) {
                bytes.write(block.payload);
            }
        }

        return bytes.toByteArray();
    }

    private static int commonPrefixLength(byte[] first, byte[] second) {
        int maximumLength = Math.min(first.length, second.length);
        int length = 0;

        while (length < maximumLength && first[length] == second[length]) {
            length += 1;
        }

        return length;
    }

    private static int compareUnsigned(byte[] first, byte[] second) {
        int maximumLength = Math.min(first.length, second.length);

        for (int index = 0; index < maximumLength; index += 1) {
            int difference = Byte.toUnsignedInt(first[index])
                - Byte.toUnsignedInt(second[index]);

            if (difference != 0) {
                return difference;
            }
        }

        return first.length - second.length;
    }

    private static void writeVariableLengthInteger(
        ByteArrayOutputStream output,
        int value
    ) {
        int remainingValue = value;

        do {
            int currentByte = remainingValue & 0x7F;
            remainingValue >>>= 7;

            if (remainingValue > 0) {
                currentByte |= 0x80;
            }

            output.write(currentByte);
        } while (remainingValue > 0);
    }

    private record Section(
        int identifier,
        List<Block> blocks,
        int wordCount
    ) {
        int indexSize() {
            return blocks.stream()
                .mapToInt(block ->
                    Short.BYTES
                        + block.firstWord.length
                        + Long.BYTES
                        + Integer.BYTES
                        + Integer.BYTES
                        + Short.BYTES
                        + Byte.BYTES
                )
                .sum();
        }
    }

    private record Block(
        byte[] firstWord,
        byte[] payload,
        int uncompressedLength,
        int wordCount,
        int codec
    ) {
    }

    private static final class LittleEndianWriter {
        private final DataOutputStream output;

        private LittleEndianWriter(ByteArrayOutputStream output) {
            this.output = new DataOutputStream(output);
        }

        private void writeShort(int value) throws IOException {
            output.write(
                ByteBuffer.allocate(Short.BYTES)
                    .order(ByteOrder.LITTLE_ENDIAN)
                    .putShort((short) value)
                    .array()
            );
        }

        private void writeInt(int value) throws IOException {
            output.write(
                ByteBuffer.allocate(Integer.BYTES)
                    .order(ByteOrder.LITTLE_ENDIAN)
                    .putInt(value)
                    .array()
            );
        }

        private void writeLong(long value) throws IOException {
            output.write(
                ByteBuffer.allocate(Long.BYTES)
                    .order(ByteOrder.LITTLE_ENDIAN)
                    .putLong(value)
                    .array()
            );
        }
    }
}
