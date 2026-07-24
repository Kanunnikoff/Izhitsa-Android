package software.kanunnikoff.izhitsa

import android.content.Context
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.nio.charset.StandardCharsets
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.zip.Inflater

/** Источник словарных подсказок, пригодный для замены в модульных проверках. */
internal fun interface SuggestionDictionary {
    /**
     * Возвращает не более [limit] слов, начинающихся с [prefix].
     *
     * Результат должен сохранять приоритет исходного словаря.
     *
     * @param prefix начало искомого слова.
     * @param limit наибольшее число результатов.
     */
    fun suggestions(
        prefix: String,
        limit: Int
    ): List<String>
}

/**
 * Асинхронно загружает встроенный русский словарь и обслуживает запросы подсказок.
 *
 * До завершения загрузки запросы намеренно возвращают пустой список и не
 * задерживают поток метода ввода.
 *
 * @property context контекст приложения для доступа к встроенному файлу.
 */
internal class RussianDictionary(
    private val context: Context
) : SuggestionDictionary {
    private val loadingStarted = AtomicBoolean(false)

    @Volatile
    private var storage: RussianDictionaryStorage? = null

    /** Однократно запускает чтение словаря в отдельном последовательном потоке. */
    fun prepare() {
        if (!loadingStarted.compareAndSet(false, true)) {
            return
        }

        LoadingExecutor.execute {
            storage = runCatching {
                loadBundledStorage()
            }.getOrNull()
        }
    }

    /**
     * Ищет варианты в уже загруженном хранилище, не блокируя вызывающий поток.
     *
     * @param prefix начало искомого слова.
     * @param limit наибольшее число результатов.
     */
    override fun suggestions(
        prefix: String,
        limit: Int
    ): List<String> {
        return storage?.suggestions(
            prefix = prefix,
            limit = limit
        ).orEmpty()
    }

    /** Отображает несжатый ресурс словаря в память без копирования всего файла в кучу. */
    private fun loadBundledStorage(): RussianDictionaryStorage {
        /*
         * Словарь не распаковывается целиком в кучу метода ввода. Android
         * отображает несжатый ресурс в память, а поиск читает только индекс и
         * один-два небольших блока, относящихся к введённому началу слова.
         */
        context.assets.openFd(DictionaryAssetName).use { asset ->
            FileInputStream(asset.fileDescriptor).channel.use { channel ->
                val buffer = channel.map(
                    FileChannel.MapMode.READ_ONLY,
                    asset.startOffset,
                    asset.length
                )

                return RussianDictionaryStorage.parse(buffer = buffer)
            }
        }
    }

    companion object {
        private const val DictionaryAssetName = "russian.bin"

        private val LoadingExecutor = Executors.newSingleThreadExecutor { task ->
            Thread(task, "izhitsa-dictionary-loader").apply {
                isDaemon = true
            }
        }
    }
}

/**
 * Выполняет поиск по двоичному словарю формата `IZHDICT2`.
 *
 * Индекс хранит первое слово каждого блока. Содержимое блока распаковывается
 * только тогда, когда оно требуется для текущего запроса.
 *
 * @property data неизменяемое содержимое двоичного файла.
 * @property dictionaryBlocks индекс раздела обычных слов.
 * @property surnameBlocks индекс раздела фамилий.
 */
internal class RussianDictionaryStorage private constructor(
    private val data: ByteBuffer,
    private val dictionaryBlocks: List<Block>,
    private val surnameBlocks: List<Block>
) {
    /**
     * Возвращает слова и фамилии с указанным началом.
     *
     * Фамилии получают начальную прописную, а повторения из двух разделов удаляются.
     *
     * @param prefix начало искомого слова.
     * @param limit наибольшее число результатов.
     */
    fun suggestions(
        prefix: String,
        limit: Int
    ): List<String> {
        if (prefix.isBlank() || limit <= 0) {
            return emptyList()
        }

        val normalizedPrefix = prefix.lowercase(RussianLocale)
        val query = normalizedPrefix.toByteArray(StandardCharsets.UTF_8)
        val dictionarySuggestions = suggestions(
            blocks = dictionaryBlocks,
            query = query,
            limit = limit
        ).map { word ->
            if (contains(word = word, blocks = surnameBlocks)) {
                word.replaceFirstChar { character ->
                    character.titlecase(RussianLocale)
                }
            } else {
                word
            }
        }
        val remainingCount = limit - dictionarySuggestions.size

        if (remainingCount <= 0) {
            return dictionarySuggestions
        }

        val normalizedExisting = dictionarySuggestions
            .mapTo(mutableSetOf()) { word ->
                word.lowercase(RussianLocale)
            }
        val surnameSuggestions = suggestions(
            blocks = surnameBlocks,
            query = query,
            limit = remainingCount
        )
            .filter { word ->
                normalizedExisting.add(word)
            }
            .map { word ->
                word.replaceFirstChar { character ->
                    character.titlecase(RussianLocale)
                }
            }

        return dictionarySuggestions + surnameSuggestions
    }

    /**
     * Последовательно читает подходящие слова, начиная с найденного индексом блока.
     *
     * @param blocks индекс выбранного раздела.
     * @param query начало слова в UTF-8.
     * @param limit наибольшее число результатов.
     */
    private fun suggestions(
        blocks: List<Block>,
        query: ByteArray,
        limit: Int
    ): List<String> {
        if (blocks.isEmpty()) {
            return emptyList()
        }

        val result = mutableListOf<String>()
        var blockIndex = blockIndexAtOrBefore(
            query = query,
            blocks = blocks
        )

        while (blockIndex < blocks.size && result.size < limit) {
            val words = words(block = blocks[blockIndex])

            for (word in words) {
                if (compareUnsigned(word, query) < 0) {
                    continue
                }

                if (!word.startsWith(query)) {
                    return result
                }

                result += String(word, StandardCharsets.UTF_8)

                if (result.size == limit) {
                    break
                }
            }

            blockIndex += 1
        }

        return result
    }

    /**
     * Проверяет точное наличие слова в отсортированном разделе.
     *
     * @param word проверяемое слово.
     * @param blocks индекс выбранного раздела.
     */
    private fun contains(
        word: String,
        blocks: List<Block>
    ): Boolean {
        if (blocks.isEmpty()) {
            return false
        }

        val query = word.toByteArray(StandardCharsets.UTF_8)
        val block = blocks[
            blockIndexAtOrBefore(
                query = query,
                blocks = blocks
            )
        ]

        for (candidate in words(block = block)) {
            val comparison = compareUnsigned(candidate, query)

            if (comparison == 0) {
                return true
            }

            if (comparison > 0) {
                return false
            }
        }

        return false
    }

    /**
     * Декодирует блок слов.
     *
     * Каждое слово хранит длину общего начала с предыдущим словом и собственный
     * остаток, благодаря чему частые русские приставки не повторяются в файле.
     *
     * @param block декодируемый блок.
     */
    private fun words(block: Block): List<ByteArray> {
        val payload = ByteArray(block.storedLength)
        val source = data.duplicate().order(ByteOrder.LITTLE_ENDIAN)
        source.position(block.payloadOffset)
        source.get(payload)

        val decodedPayload = when (block.codec) {
            CodecRaw -> payload
            CodecDeflate -> inflate(
                compressed = payload,
                expectedLength = block.uncompressedLength
            )

            else -> error("Неизвестный способ сжатия словаря: ${block.codec}")
        }
        val cursor = ByteArrayCursor(decodedPayload)
        val result = ArrayList<ByteArray>(block.wordCount)
        var previousWord = ByteArray(0)

        repeat(block.wordCount) {
            val prefixLength = cursor.readVariableLengthInteger()
            val suffixLength = cursor.readVariableLengthInteger()

            check(prefixLength <= previousWord.size) {
                "Повреждён блок словаря: неверная длина общего начала."
            }

            val word = ByteArray(prefixLength + suffixLength)
            previousWord.copyInto(
                destination = word,
                endIndex = prefixLength
            )
            cursor.readInto(
                destination = word,
                destinationOffset = prefixLength,
                length = suffixLength
            )
            result += word
            previousWord = word
        }

        return result
    }

    /**
     * Двоичным поиском находит последний блок, начинающийся не позже [query].
     *
     * @param query искомая байтовая строка.
     * @param blocks индекс выбранного раздела.
     */
    private fun blockIndexAtOrBefore(
        query: ByteArray,
        blocks: List<Block>
    ): Int {
        var lowerBound = 0
        var upperBound = blocks.size

        while (lowerBound < upperBound) {
            val middle = (lowerBound + upperBound) ushr 1

            if (compareUnsigned(blocks[middle].firstWord, query) <= 0) {
                lowerBound = middle + 1
            } else {
                upperBound = middle
            }
        }

        return (lowerBound - 1).coerceAtLeast(0)
    }

    /**
     * Распаковывает блок и проверяет ожидаемый размер результата.
     *
     * @param compressed сжатое содержимое.
     * @param expectedLength ожидаемый размер после распаковки.
     */
    private fun inflate(
        compressed: ByteArray,
        expectedLength: Int
    ): ByteArray {
        val inflater = Inflater()

        return try {
            inflater.setInput(compressed)
            val output = ByteArray(expectedLength)
            val decodedLength = inflater.inflate(output)

            check(decodedLength == expectedLength && inflater.finished()) {
                "Не удалось распаковать блок словаря."
            }

            output
        } finally {
            inflater.end()
        }
    }

    companion object {
        private val Magic = "IZHDICT2".toByteArray(StandardCharsets.US_ASCII)
        private val RussianLocale = Locale.forLanguageTag("ru-RU")

        private const val FormatVersion = 1
        private const val DictionarySectionIdentifier = 0
        private const val SurnameSectionIdentifier = 1
        private const val ExpectedSectionCount = 2
        private const val CodecRaw = 0
        private const val CodecDeflate = 1

        /**
         * Проверяет заголовок и строит представление словаря поверх [buffer].
         *
         * @param buffer содержимое двоичного словаря.
         */
        fun parse(buffer: ByteBuffer): RussianDictionaryStorage {
            val data = buffer
                .asReadOnlyBuffer()
                .order(ByteOrder.LITTLE_ENDIAN)
            val actualMagic = ByteArray(Magic.size)
            data.get(actualMagic)

            require(actualMagic.contentEquals(Magic)) {
                "Неверная сигнатура двоичного словаря."
            }
            require(data.int == FormatVersion) {
                "Версия двоичного словаря не поддерживается."
            }

            val sectionCount = data.int
            require(sectionCount == ExpectedSectionCount) {
                "В двоичном словаре отсутствует обязательный раздел."
            }

            val descriptors = List(sectionCount) {
                SectionDescriptor(
                    identifier = data.int,
                    blockCount = data.int,
                    indexOffset = data.long.toInt()
                )
            }
            val sections = descriptors.associate { descriptor ->
                descriptor.identifier to parseBlocks(
                    data = data,
                    descriptor = descriptor
                )
            }

            return RussianDictionaryStorage(
                data = data,
                dictionaryBlocks = sections[DictionarySectionIdentifier]
                    ?: error("Не найден основной раздел словаря."),
                surnameBlocks = sections[SurnameSectionIdentifier]
                    ?: error("Не найден раздел фамилий.")
            )
        }

        /**
         * Читает индекс блоков одного раздела, не затрагивая их содержимое.
         *
         * @param data содержимое двоичного словаря.
         * @param descriptor положение и размер раздела.
         */
        private fun parseBlocks(
            data: ByteBuffer,
            descriptor: SectionDescriptor
        ): List<Block> {
            val index = data
                .duplicate()
                .order(ByteOrder.LITTLE_ENDIAN)
            index.position(descriptor.indexOffset)

            return List(descriptor.blockCount) {
                val firstWordLength = index.short.toInt() and 0xFFFF
                val firstWord = ByteArray(firstWordLength)
                index.get(firstWord)

                Block(
                    firstWord = firstWord,
                    payloadOffset = index.long.toInt(),
                    storedLength = index.int,
                    uncompressedLength = index.int,
                    wordCount = index.short.toInt() and 0xFFFF,
                    codec = index.get().toInt() and 0xFF
                )
            }
        }

        /**
         * Сравнивает байтовые строки как беззнаковые последовательности UTF-8.
         *
         * @param first первая последовательность.
         * @param second вторая последовательность.
         */
        private fun compareUnsigned(
            first: ByteArray,
            second: ByteArray
        ): Int {
            val maximumLength = minOf(first.size, second.size)

            for (index in 0 until maximumLength) {
                val difference = first[index].toUByte().toInt() -
                    second[index].toUByte().toInt()

                if (difference != 0) {
                    return difference
                }
            }

            return first.size - second.size
        }

        /**
         * Проверяет байтовый префикс без промежуточного преобразования в строку.
         *
         * @receiver проверяемая последовательность.
         * @param prefix ожидаемое начало.
         */
        private fun ByteArray.startsWith(prefix: ByteArray): Boolean {
            if (size < prefix.size) {
                return false
            }

            return prefix.indices.all { index ->
                this[index] == prefix[index]
            }
        }
    }

    /**
     * Описывает положение индекса одного логического раздела файла.
     *
     * @property identifier номер раздела.
     * @property blockCount число блоков.
     * @property indexOffset смещение индекса в файле.
     */
    private data class SectionDescriptor(
        val identifier: Int,
        val blockCount: Int,
        val indexOffset: Int
    )

    /**
     * Описывает сжатое содержимое одного диапазона отсортированных слов.
     *
     * @property firstWord первое слово блока.
     * @property payloadOffset смещение содержимого в файле.
     * @property storedLength сохранённый размер.
     * @property uncompressedLength размер после распаковки.
     * @property wordCount число слов.
     * @property codec способ хранения.
     */
    private data class Block(
        val firstWord: ByteArray,
        val payloadOffset: Int,
        val storedLength: Int,
        val uncompressedLength: Int,
        val wordCount: Int,
        val codec: Int
    )
}

/**
 * Последовательно читает целые числа и байтовые диапазоны из декодированного блока.
 *
 * @property bytes содержимое блока.
 */
private class ByteArrayCursor(
    private val bytes: ByteArray
) {
    private var offset = 0

    /** Читает беззнаковое целое в формате с семью полезными битами на байт. */
    fun readVariableLengthInteger(): Int {
        var value = 0
        var shift = 0

        while (true) {
            check(offset < bytes.size) {
                "Неожиданный конец блока словаря."
            }

            val currentByte = bytes[offset].toInt() and 0xFF
            offset += 1
            value = value or ((currentByte and 0x7F) shl shift)

            if ((currentByte and 0x80) == 0) {
                return value
            }

            shift += 7
            check(shift < Int.SIZE_BITS) {
                "Слишком большое число в блоке словаря."
            }
        }
    }

    /**
     * Копирует следующие [length] байт в [destination] и сдвигает курсор.
     *
     * @param destination массив-получатель.
     * @param destinationOffset начальная позиция записи.
     * @param length число копируемых байт.
     */
    fun readInto(
        destination: ByteArray,
        destinationOffset: Int,
        length: Int
    ) {
        check(offset + length <= bytes.size) {
            "Неожиданный конец блока словаря."
        }

        bytes.copyInto(
            destination = destination,
            destinationOffset = destinationOffset,
            startIndex = offset,
            endIndex = offset + length
        )
        offset += length
    }
}
