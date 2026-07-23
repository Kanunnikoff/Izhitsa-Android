# Проверка соответствия Android- и iOS-версий

## Объём проверки

- Эталон: актуальная iOS-сборка из `/Users/DKanunnikov/XcodeProjects/Izhitsa-v2-iOS`.
- Android: отладочная сборка на эмуляторе Resizable Experimental, 1080 × 2400, Android 16 (API 36).
- Целевой уровень Android: API 37.
- Минимальный уровень Android: API 26.
- Проверены светлая и тёмная темы.
- Проверены главная, алфавит, настройки, сведения о приложении и клавиатура.

## Состав приложения

Навигация телефона повторяет актуальную iOS-версию и содержит ровно четыре раздела:

1. Главная.
2. Алфавит.
3. Настройки.
4. О программе.

Отдельного раздела стикеров в приложении нет. Стикеры доступны только на панели клавиатуры.

При выключенной дореволюционной орфографии используется название «Алфавит». При включённой настройке оно меняется на «Азбука», как в `TabNavigationView.swift`, `Sidebar.swift` и `AlphabetView.swift` iOS-версии.

## Сверка текстов и структуры

- Главная повторяет порядок iOS: заголовок и чаевые, руководство из трёх шагов, состояние клавиатуры, ограничение для отдельных полей, описание дополнительных букв и поле проверки ввода.
- Тексты руководства отличаются только системным путём: на Android указан путь Android, а не разделы настроек iOS.
- Алфавит содержит те же 35 букв, тот же порядок, заголовок, пояснения и ссылку на источник.
- «О программе» содержит те же группы, порядок действий и поясняющие тексты. Название магазина и ссылки заменены только на соответствующие варианты Google Play.
- В настройках повторён раздел приложения. Два переключателя iOS, меняющие шрифт клавиш и звуковой отклик, намеренно не перенесены: это изменило бы клавиши и их поведение вопреки ограничению задачи.

## Снимки текущей проверки

### iOS

- Главная: `/tmp/izhitsa-parity-audit/ios-home-light.jpg`
- Алфавит: `/tmp/izhitsa-parity-audit/ios-alphabet-light.jpg`
- Настройки: `/tmp/izhitsa-parity-audit/ios-settings-light.jpg`
- О программе: `/tmp/izhitsa-parity-audit/ios-about-light.jpg`

### Android

- Главная, светлая тема: `/tmp/izhitsa-parity-audit/01-android-home-light.png`
- Алфавит, светлая тема: `/tmp/izhitsa-parity-audit/02-android-alphabet-light.png`
- Настройки, светлая тема: `/tmp/izhitsa-parity-audit/03-android-settings-light.png`
- О программе, светлая тема: `/tmp/izhitsa-parity-audit/04-android-about-light.png`
- Клавиатура: `/tmp/izhitsa-parity-audit/05-android-keyboard.png`
- Главная, тёмная тема: `/tmp/izhitsa-parity-audit/06-android-home-dark.png`
- Настройки, тёмная тема: `/tmp/izhitsa-parity-audit/07-android-settings-dark.png`
- О программе, тёмная тема: `/tmp/izhitsa-parity-audit/08-android-about-dark.png`

### Попарные сравнения

В каждом файле слева находится iOS, справа — Android:

- Главная: `/tmp/izhitsa-parity-audit/compare-home.png`
- Алфавит: `/tmp/izhitsa-parity-audit/compare-alphabet.png`
- Настройки: `/tmp/izhitsa-parity-audit/compare-settings.png`
- О программе: `/tmp/izhitsa-parity-audit/compare-about.png`

## Проверки

- Отладочная и выпускная сборки: успешно.
- Модульные проверки: успешно.
- Инструментальные проверки на эмуляторе: успешно.
- Android Lint: успешно.
- Передача стикеров через `commitContent`: успешно.
- Появление клавиатуры после явного выбора: успешно.
- `KeyboardLayouts.kt`: изменений нет.
- Светлая и тёмная темы: видимых обрезаний, наложений и нечитаемых сочетаний не обнаружено.

Final result: passed
