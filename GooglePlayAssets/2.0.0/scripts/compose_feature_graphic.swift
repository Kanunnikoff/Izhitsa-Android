#!/usr/bin/env swift

import AppKit
import Foundation

private let canvasSize = NSSize(width: 1_024, height: 500)
private let styleCropHeight: CGFloat = 606
private let maximumFileSize = 15 * 1_024 * 1_024

private let goldColor = NSColor(
    calibratedRed: 203.0 / 255.0,
    green: 143.0 / 255.0,
    blue: 83.0 / 255.0,
    alpha: 1.0
)

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

private func drawStyleBackground(_ reference: NSImage) {
    /*
     Верхний фрагмент оригинального iOS-макета почти точно совпадает
     с пропорциями 1024×500. Поэтому он переносится целиком: шрифт,
     интервалы, фактура и орнамент сохраняются без перерисовки.
     */
    let source = NSRect(
        x: 0,
        y: reference.size.height - styleCropHeight,
        width: reference.size.width,
        height: styleCropHeight
    )

    reference.draw(
        in: NSRect(origin: .zero, size: canvasSize),
        from: source,
        operation: .sourceOver,
        fraction: 1.0,
        respectFlipped: true,
        hints: [.interpolation: NSImageInterpolation.high]
    )
}

private func drawApplicationIcon(_ icon: NSImage) {
    let iconRectangle = rectangleFromTop(
        x: 805,
        y: 286,
        width: 180,
        height: 180
    )

    NSGraphicsContext.saveGraphicsState()

    let shadow = NSShadow()
    shadow.shadowColor = NSColor.black.withAlphaComponent(0.58)
    shadow.shadowBlurRadius = 24
    shadow.shadowOffset = NSSize(width: 0, height: -10)
    shadow.set()

    icon.draw(
        in: iconRectangle,
        from: NSRect(origin: .zero, size: icon.size),
        operation: .sourceOver,
        fraction: 1.0,
        respectFlipped: true,
        hints: [.interpolation: NSImageInterpolation.high]
    )

    NSGraphicsContext.restoreGraphicsState()
}

private func drawBottomDecoration() {
    let lineY = canvasSize.height - 18
    let sideInset: CGFloat = 26
    let centerX = canvasSize.width / 2
    let diamondSize: CGFloat = 8

    goldColor.withAlphaComponent(0.72).setStroke()

    let line = NSBezierPath()
    line.lineWidth = 1
    line.move(to: NSPoint(x: sideInset, y: lineY))
    line.line(to: NSPoint(x: centerX - 14, y: lineY))
    line.move(to: NSPoint(x: centerX + 14, y: lineY))
    line.line(to: NSPoint(x: canvasSize.width - sideInset, y: lineY))
    line.stroke()

    let diamond = NSBezierPath()
    diamond.move(to: NSPoint(x: centerX, y: lineY + diamondSize))
    diamond.line(to: NSPoint(x: centerX + diamondSize, y: lineY))
    diamond.line(to: NSPoint(x: centerX, y: lineY - diamondSize))
    diamond.line(to: NSPoint(x: centerX - diamondSize, y: lineY))
    diamond.close()
    diamond.lineWidth = 1
    diamond.stroke()
}

private func main() throws {
    let scriptURL = URL(fileURLWithPath: CommandLine.arguments[0])
        .standardizedFileURL
    let assetDirectory = scriptURL
        .deletingLastPathComponent()
        .deletingLastPathComponent()
    let sourceDirectory = assetDirectory.appendingPathComponent("sources")
    let outputDirectory = assetDirectory.appendingPathComponent("final")
    let referenceURL = sourceDirectory.appendingPathComponent(
        "ios-style-reference.png"
    )
    let iconURL = sourceDirectory.appendingPathComponent(
        "android-app-icon.png"
    )
    let outputURL = outputDirectory.appendingPathComponent(
        "feature-graphic.png"
    )

    guard let reference = NSImage(contentsOf: referenceURL) else {
        throw CompositionError.cannotLoadImage(referenceURL)
    }
    guard let icon = NSImage(contentsOf: iconURL) else {
        throw CompositionError.cannotLoadImage(iconURL)
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

    try FileManager.default.createDirectory(
        at: outputDirectory,
        withIntermediateDirectories: true
    )

    NSGraphicsContext.saveGraphicsState()
    NSGraphicsContext.current = graphicsContext
    graphicsContext.imageInterpolation = .high

    drawStyleBackground(reference)
    drawApplicationIcon(icon)
    drawBottomDecoration()

    graphicsContext.flushGraphics()
    NSGraphicsContext.restoreGraphicsState()

    guard let data = bitmap.representation(
        using: .png,
        properties: [.compressionFactor: 0.86]
    ) else {
        throw CompositionError.cannotEncodePNG
    }

    try data.write(to: outputURL, options: .atomic)

    if data.count > maximumFileSize {
        throw CompositionError.invalidOutputSize(outputURL, data.count)
    }

    print(
        "\(outputURL.lastPathComponent): "
            + "\(Int(canvasSize.width))×\(Int(canvasSize.height)), "
            + "\(data.count) байт"
    )
}

do {
    try main()
} catch {
    FileHandle.standardError.write(
        Data("Ошибка: \(error.localizedDescription)\n".utf8)
    )
    exit(EXIT_FAILURE)
}
