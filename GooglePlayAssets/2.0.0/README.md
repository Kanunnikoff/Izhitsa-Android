# Материалы для Google Play

Готовые изображения находятся в каталоге `final`:

- `01-pre-revolutionary-keyboard.png`;
- `02-historical-stickers.png`;
- `03-pre-revolutionary-alphabet.png`;
- `feature-graphic.png` — картинка для описания 1024×500 пикселей.

Снимки экрана имеют размер 1080×1920 пикселей и соотношение сторон 9:16.
Картинка для описания имеет размер 1024×500 пикселей. Все материалы сохранены
в PNG. Тексты и оформление повторяют материалы iOS-версии, а интерфейс взят
из настоящих снимков Android-приложения.

Для повторной сборки:

```shell
swift scripts/compose_screenshots.swift
swift scripts/compose_feature_graphic.swift
```

Исходные Android-снимки и образец оформления iOS находятся в каталоге
`sources`.
