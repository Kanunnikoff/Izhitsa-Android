#!/usr/bin/env swift

import AppKit
import Foundation

private let canvasSize = NSSize(width: 1_080, height: 1_920)
private let panelWidth: CGFloat = 972
private let maximumFileSize = 8 * 1_024 * 1_024

private let darkBlueColor = NSColor(
    calibratedRed: 4.0 / 255.0,
    green: 16.0 / 255.0,
    blue: 28.0 / 255.0,
    alpha: 1.0
)
private let goldColor = NSColor(
    calibratedRed: 221.0 / 255.0,
    green: 166.0 / 255.0,
    blue: 99.0 / 255.0,
    alpha: 1.0
)

private struct SourceCrop {
    let top: CGFloat
    let height: CGFloat
}

private struct ScreenshotSpecification {
    let androidSourceFileName: String
    let styleReferenceFileName: String
    let outputFileName: String
    let badge: String
    let panelTop: CGFloat
    let crop: SourceCrop
    let showsBottomFrame: Bool
}

private enum CompositionError: LocalizedError {
    case cannotLoadImage(URL)
    case cannotCreateBitmap
    case cannotEncodePNG
    case invalidOutputSize(URL, Int)

    var errorDescription: String? {
        switch self {
        case let .cannotLoadImage(url):
            return "Не удалось загрузить изображение: \(url.path)"
        case .cannotCreateBitmap:
            return "Не удалось создать растровое изображение."
        case .cannotEncodePNG:
            return "Не удалось закодировать итоговое изображение в PNG."
        case let .invalidOutputSize(url, size):
            return "Файл \(url.lastPathComponent) занимает \(size) байт, что превышает допустимый размер."
        }
    }
}

private let specifications = [
    ScreenshotSpecification(
        androidSourceFileName: "01-keyboard.png",
        styleReferenceFileName: "ios-style-reference.png",
        outputFileName: "01-pre-revolutionary-keyboard.png",
        badge: "ѣ · ѳ · і · ѵ",
        panelTop: 670,
        crop: SourceCrop(top: 1_280, height: 1_120),
        showsBottomFrame: true
    ),
    ScreenshotSpecification(
        androidSourceFileName: "02-stickers.png",
        styleReferenceFileName: "ios-style-stickers.png",
        outputFileName: "02-historical-stickers.png",
        badge: "СТИКЕРЫ",
        panelTop: 620,
        crop: SourceCrop(top: 1_120, height: 1_280),
        showsBottomFrame: true
    ),
    ScreenshotSpecification(
        androidSourceFileName: "03-alphabet.png",
        styleReferenceFileName: "ios-style-alphabet.png",
        outputFileName: "03-pre-revolutionary-alphabet.png",
        badge: "35 БУКВ",
        panelTop: 570,
        crop: SourceCrop(top: 75, height: 1_535),
        showsBottomFrame: false
    )
]

private func rectangleFromTop(
    x: CGFloat,
    y: CGFloat,
    width: CGFloat,
    height: CGFloat
) -> NSRect {
    NSRect(
        x: x,
        y: canvasSize.height - y - height,
        width: width,
        height: height
    )
}

private func sourceRectangle(
    image: NSImage,
    crop: SourceCrop
) -> NSRect {
    /*
     Положение фрагмента удобнее задавать от верхнего края, как в редакторе
     изображений. AppKit отсчитывает координаты снизу, поэтому ось преобразуется.
     */
    NSRect(
        x: 0,
        y: image.size.height - crop.top - crop.height,
        width: image.size.width,
        height: crop.height
    )
}

private func drawStyleReference(_ reference: NSImage) -> CGFloat {
    /*
     Единый масштаб по ширине сохраняет пропорции рамки, шрифта и украшений.
     Нижняя часть временно выходит за холст и затем восстанавливается отдельным
     фрагментом, чтобы новый формат 9:16 не сплющивал исходное оформление.
     */
    let scale = canvasSize.width / reference.size.width
    let scaledHeight = reference.size.height * scale
    let destination = NSRect(
        x: 0,
        y: canvasSize.height - scaledHeight,
        width: canvasSize.width,
        height: scaledHeight
    )

    reference.draw(
        in: destination,
        from: NSRect(origin: .zero, size: reference.size),
        operation: .sourceOver,
        fraction: 1.0,
        respectFlipped: true,
        hints: [.interpolation: NSImageInterpolation.high]
    )

    return scale
}

private func drawBottomFrame(
    reference: NSImage,
    scale: CGFloat,
    top: CGFloat
) {
    let destinationHeight = canvasSize.height - top
    guard destinationHeight > 0 else {
        return
    }

    let sourceHeight = destinationHeight / scale
    let source = NSRect(
        x: 0,
        y: 0,
        width: reference.size.width,
        height: sourceHeight
    )
    let destination = rectangleFromTop(
        x: 0,
        y: top,
        width: canvasSize.width,
        height: destinationHeight
    )

    reference.draw(
        in: destination,
        from: source,
        operation: .sourceOver,
        fraction: 1.0,
        respectFlipped: true,
        hints: [.interpolation: NSImageInterpolation.high]
    )
}

private func drawApplicationPanel(
    screenshot: NSImage,
    specification: ScreenshotSpecification
) {
    let panelHeight = panelWidth
        * specification.crop.height
        / screenshot.size.width
    let panel = rectangleFromTop(
        x: (canvasSize.width - panelWidth) / 2,
        y: specification.panelTop,
        width: panelWidth,
        height: panelHeight
    )
    let panelPath = NSBezierPath(
        roundedRect: panel,
        xRadius: 56,
        yRadius: 56
    )

    NSGraphicsContext.saveGraphicsState()

    let shadow = NSShadow()
    shadow.shadowColor = NSColor.black.withAlphaComponent(0.34)
    shadow.shadowBlurRadius = 24
    shadow.shadowOffset = NSSize(width: 0, height: -12)
    shadow.set()

    NSColor.white.setFill()
    panelPath.fill()

    NSGraphicsContext.restoreGraphicsState()

    NSGraphicsContext.saveGraphicsState()
    panelPath.addClip()

    screenshot.draw(
        in: panel,
        from: sourceRectangle(
            image: screenshot,
            crop: specification.crop
        ),
        operation: .sourceOver,
        fraction: 1.0,
        respectFlipped: true,
        hints: [.interpolation: NSImageInterpolation.high]
    )

    NSGraphicsContext.restoreGraphicsState()

    goldColor.withAlphaComponent(0.82).setStroke()
    panelPath.lineWidth = 1.5
    panelPath.stroke()

    drawBadge(
        text: specification.badge,
        panelTop: specification.panelTop
    )
}

private func drawBadge(
    text: String,
    panelTop: CGFloat
) {
    let font = NSFont.systemFont(ofSize: 19, weight: .heavy)
    let tracking: CGFloat = 1.1
    let attributes: [NSAttributedString.Key: Any] = [
        .font: font,
        .foregroundColor: goldColor,
        .kern: tracking
    ]
    let textSize = NSString(string: text).size(withAttributes: attributes)
    let horizontalPadding: CGFloat = 28
    let badgeWidth = max(150, textSize.width + horizontalPadding * 2)
    let badgeHeight: CGFloat = 54
    let badgeLeft: CGFloat = 82
    let badgeTop = panelTop - badgeHeight / 2
    let badge = rectangleFromTop(
        x: badgeLeft,
        y: badgeTop,
        width: badgeWidth,
        height: badgeHeight
    )

    darkBlueColor.setFill()
    NSBezierPath(
        roundedRect: badge,
        xRadius: badgeHeight / 2,
        yRadius: badgeHeight / 2
    ).fill()

    let textRectangle = rectangleFromTop(
        x: badgeLeft + horizontalPadding,
        y: badgeTop + 15,
        width: badgeWidth - horizontalPadding * 2,
        height: 25
    )
    NSString(string: text).draw(
        in: textRectangle,
        withAttributes: attributes
    )
}

private func compose(
    specification: ScreenshotSpecification,
    sourceDirectory: URL,
    outputDirectory: URL
) throws {
    let androidSourceURL = sourceDirectory.appendingPathComponent(
        specification.androidSourceFileName
    )
    let styleReferenceURL = sourceDirectory.appendingPathComponent(
        specification.styleReferenceFileName
    )
    let bottomFrameReferenceURL = sourceDirectory.appendingPathComponent(
        "ios-style-reference.png"
    )

    guard let screenshot = NSImage(contentsOf: androidSourceURL) else {
        throw CompositionError.cannotLoadImage(androidSourceURL)
    }
    guard let reference = NSImage(contentsOf: styleReferenceURL) else {
        throw CompositionError.cannotLoadImage(styleReferenceURL)
    }
    guard let bottomFrameReference = NSImage(
        contentsOf: bottomFrameReferenceURL
    ) else {
        throw CompositionError.cannotLoadImage(bottomFrameReferenceURL)
    }

    guard let bitmap = NSBitmapImageRep(
        bitmapDataPlanes: nil,
        pixelsWide: Int(canvasSize.width),
        pixelsHigh: Int(canvasSize.height),
        bitsPerSample: 8,
        samplesPerPixel: 4,
        hasAlpha: true,
        isPlanar: false,
        colorSpaceName: .deviceRGB,
        bytesPerRow: 0,
        bitsPerPixel: 0
    ),
    let graphicsContext = NSGraphicsContext(bitmapImageRep: bitmap) else {
        throw CompositionError.cannotCreateBitmap
    }

    NSGraphicsContext.saveGraphicsState()
    NSGraphicsContext.current = graphicsContext
    graphicsContext.imageInterpolation = .high

    darkBlueColor.setFill()
    NSRect(origin: .zero, size: canvasSize).fill()

    _ = drawStyleReference(reference)
    let panelHeight = panelWidth
        * specification.crop.height
        / screenshot.size.width
    let panelBottom = specification.panelTop + panelHeight

    if specification.showsBottomFrame {
        drawBottomFrame(
            reference: bottomFrameReference,
            scale: canvasSize.width / bottomFrameReference.size.width,
            top: panelBottom
        )
    }

    drawApplicationPanel(
        screenshot: screenshot,
        specification: specification
    )

    graphicsContext.flushGraphics()
    NSGraphicsContext.restoreGraphicsState()

    guard let data = bitmap.representation(
        using: .png,
        properties: [.compressionFactor: 0.84]
    ) else {
        throw CompositionError.cannotEncodePNG
    }

    let outputURL = outputDirectory.appendingPathComponent(
        specification.outputFileName
    )
    try data.write(to: outputURL, options: .atomic)

    if data.count > maximumFileSize {
        throw CompositionError.invalidOutputSize(outputURL, data.count)
    }

    print(
        "\(specification.outputFileName): "
            + "\(Int(canvasSize.width))×\(Int(canvasSize.height)), "
            + "\(data.count) байт"
    )
}

private func main() throws {
    let scriptURL = URL(fileURLWithPath: CommandLine.arguments[0])
        .standardizedFileURL
    let assetDirectory = scriptURL
        .deletingLastPathComponent()
        .deletingLastPathComponent()
    let sourceDirectory = assetDirectory.appendingPathComponent("sources")
    let outputDirectory = assetDirectory.appendingPathComponent("final")

    try FileManager.default.createDirectory(
        at: outputDirectory,
        withIntermediateDirectories: true
    )

    for specification in specifications {
        try compose(
            specification: specification,
            sourceDirectory: sourceDirectory,
            outputDirectory: outputDirectory
        )
    }
}

do {
    try main()
} catch {
    FileHandle.standardError.write(
        Data("Ошибка: \(error.localizedDescription)\n".utf8)
    )
    exit(EXIT_FAILURE)
}
