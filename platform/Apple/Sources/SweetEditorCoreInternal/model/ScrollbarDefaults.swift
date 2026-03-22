import Foundation

enum ScrollbarDefaults {
    static let transientRefreshInterval: TimeInterval = 1.0 / 60.0

    static func defaultConfig() -> SweetEditorCore.ScrollbarConfig {
        SweetEditorCore.ScrollbarConfig(
            thickness: 8.0,
            minThumb: 48.0,
            thumbHitPadding: 16.0,
            mode: .TRANSIENT,
            thumbDraggable: true,
            trackTapMode: .DISABLED,
            fadeDelayMs: 700,
            fadeDurationMs: 300
        )
    }

    static func scheduleTransientRefreshTimer(_ timer: inout Timer?, action: @escaping () -> Void) {
        timer?.invalidate()
        let newTimer = Timer(timeInterval: transientRefreshInterval, repeats: false) { _ in
            action()
        }
        RunLoop.main.add(newTimer, forMode: .common)
        timer = newTimer
    }
}
