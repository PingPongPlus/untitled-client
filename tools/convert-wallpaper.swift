// macOS: swift -module-cache-path /tmp/wallpaper-swift-cache tools/convert-wallpaper.swift INPUT.mp4 OUTPUT_DIRECTORY
import AVFoundation
import ImageIO
import UniformTypeIdentifiers

let input = URL(fileURLWithPath: CommandLine.arguments[1])
let output = URL(fileURLWithPath: CommandLine.arguments[2], isDirectory: true)
try FileManager.default.createDirectory(at: output, withIntermediateDirectories: true)
let asset = AVURLAsset(url: input)
let duration = try await asset.load(.duration).seconds
let fps = 24
let count = Int(ceil(duration * Double(fps)))
let generator = AVAssetImageGenerator(asset: asset)
generator.appliesPreferredTrackTransform = true
generator.maximumSize = CGSize(width: 960, height: 540)
generator.requestedTimeToleranceBefore = .zero
generator.requestedTimeToleranceAfter = .zero
var width = 0
var height = 0
for index in 0..<count {
    let time = CMTime(seconds: Double(index) / Double(fps), preferredTimescale: 600)
    let (image, _) = try await generator.image(at: time)
    width = image.width
    height = image.height
    let file = output.appendingPathComponent(String(format: "frame_%04d.jpg", index))
    guard let destination = CGImageDestinationCreateWithURL(file as CFURL, UTType.jpeg.identifier as CFString, 1, nil) else {
        fatalError("Cannot create \(file)")
    }
    CGImageDestinationAddImage(destination, image, [kCGImageDestinationLossyCompressionQuality: 0.85] as CFDictionary)
    guard CGImageDestinationFinalize(destination) else { fatalError("Cannot save \(file)") }
    if index % fps == 0 { print("Converted \(index)/\(count) frames") }
}
let metadata = "width=\(width)\nheight=\(height)\nfps=\(fps)\nframes=\(count)\n"
try metadata.write(to: output.appendingPathComponent("animation.properties"), atomically: true, encoding: .utf8)
print("Finished: \(count) frames at \(width)x\(height), \(fps) fps")
