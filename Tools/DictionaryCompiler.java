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

/**
 * Собирает исходные списки русских слов и фамилий в двоичный словарь Ижицы.
 *
 * <p>Слова должны быть заранее отсортированы. Компилятор делит их на блоки,
 * кодирует общие начала соседних слов и сохраняет сжатый вариант блока только
 * тогда, когда он действительно короче исходного.</p>
 */
public final class DictionaryCompiler {
    /** Сигнатура двоичного формата. */
    private static final byte[] MAGIC = "IZHDICT2".getBytes(StandardCharsets.US_ASCII);

    /** Текущая версия двоичного формата. */
    private static final int VERSION = 1;

    /** Наибольшее число слов в одном независимо декодируемом блоке. */
    private static final int WORDS_PER_BLOCK = 256;

    /** Код блока без сжатия. */
    private static final int CODEC_RAW = 0;

    /** Код блока, сжатого алгоритмом DEFLATE. */
    private static final int CODEC_DEFLATE = 1;

    /** Правила регистра, применяемые к русским словам. */
    private static final Locale RUSSIAN_LOCALE = Locale.forLanguageTag("ru-RU");

    /** Запрещает создание экземпляров служебного класса. */
    private DictionaryCompiler() {
    }

    /**
     * Читает два исходных файла и записывает готовый {@code russian.bin}.
     *
     * @param arguments пути к словам, фамилиям и итоговому файлу.
     * @throws IOException если исходные или итоговый файл недоступны.
     */
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

    /**
     * Проверяет и преобразует один отсортированный список в индексированные блоки.
     *
     * @param identifier номер раздела в двоичном файле.
     * @param sourcePath путь к исходному списку.
     * @return скомпилированный раздел.
     * @throws IOException если исходный файл недоступен.
     */
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

    /**
     * Кодирует общие начала слов и выбирает наиболее компактный вид блока.
     *
     * @param words отсортированные слова блока в UTF-8.
     * @return закодированный блок.
     * @throws IOException если промежуточный поток не смог принять данные.
     */
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

    /**
     * Сжимает содержимое блока алгоритмом DEFLATE с наилучшим уровнем сжатия.
     *
     * @param source исходные байты.
     * @return сжатые байты.
     */
    private static byte[] deflate(byte[] source) {
        Deflater deflater = new Deflater(Deflater.BEST_COMPRESSION);
        deflater.setInput(source);
        deflater.finish();

        byte[] buffer = new byte[Math.max(source.length, 64)];
        int length = deflater.deflate(buffer);
        deflater.end();

        return Arrays.copyOf(buffer, length);
    }

    /**
     * Собирает заголовок, индексы и содержимое всех разделов в один файл.
     *
     * @param sections разделы в порядке записи.
     * @return содержимое итогового файла.
     * @throws IOException если промежуточный поток не смог принять данные.
     */
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

    /**
     * Возвращает число одинаковых начальных байтов двух слов UTF-8.
     *
     * @param first первое слово.
     * @param second второе слово.
     * @return длина общего начала.
     */
    private static int commonPrefixLength(byte[] first, byte[] second) {
        int maximumLength = Math.min(first.length, second.length);
        int length = 0;

        while (length < maximumLength && first[length] == second[length]) {
            length += 1;
        }

        return length;
    }

    /**
     * Сравнивает байтовые слова как беззнаковые последовательности.
     *
     * @param first первое слово.
     * @param second второе слово.
     * @return отрицательное, нулевое или положительное значение сравнения.
     */
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

    /**
     * Записывает неотрицательное целое семибитными группами переменной длины.
     *
     * @param output поток-получатель.
     * @param value записываемое значение.
     */
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

    /**
     * Раздел словаря и его сводные данные.
     *
     * @param identifier номер раздела.
     * @param blocks закодированные блоки.
     * @param wordCount общее число слов.
     */
    private record Section(
        int identifier,
        List<Block> blocks,
        int wordCount
    ) {
        /**
         * Возвращает точный размер двоичного индекса раздела.
         *
         * @return размер индекса в байтах.
         */
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

    /**
     * Закодированный блок соседних слов.
     *
     * @param firstWord первое слово для двоичного поиска.
     * @param payload сохранённое содержимое.
     * @param uncompressedLength размер до сжатия.
     * @param wordCount число слов.
     * @param codec способ хранения.
     */
    private record Block(
        byte[] firstWord,
        byte[] payload,
        int uncompressedLength,
        int wordCount,
        int codec
    ) {
    }

    /** Записывает многобайтовые числа в порядке от младшего байта к старшему. */
    private static final class LittleEndianWriter {
        /** Поток, принимающий готовые байты. */
        private final DataOutputStream output;

        /**
         * Создаёт писатель поверх буфера результата.
         *
         * @param output поток-получатель.
         */
        private LittleEndianWriter(ByteArrayOutputStream output) {
            this.output = new DataOutputStream(output);
        }

        /**
         * Записывает 16-разрядное целое.
         *
         * @param value записываемое значение.
         * @throws IOException если поток не принял данные.
         */
        private void writeShort(int value) throws IOException {
            output.write(
                ByteBuffer.allocate(Short.BYTES)
                    .order(ByteOrder.LITTLE_ENDIAN)
                    .putShort((short) value)
                    .array()
            );
        }

        /**
         * Записывает 32-разрядное целое.
         *
         * @param value записываемое значение.
         * @throws IOException если поток не принял данные.
         */
        private void writeInt(int value) throws IOException {
            output.write(
                ByteBuffer.allocate(Integer.BYTES)
                    .order(ByteOrder.LITTLE_ENDIAN)
                    .putInt(value)
                    .array()
            );
        }

        /**
         * Записывает 64-разрядное целое.
         *
         * @param value записываемое значение.
         * @throws IOException если поток не принял данные.
         */
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
