import XCTest
import AppKit
@testable import SweetEditorMacOS
@testable import SweetEditorCoreInternal

final class SweetEditorMacOSTests: XCTestCase {
    func testModuleCompiles() {
        XCTAssertTrue(true)
    }

    func testInlayHintTextAlphaDefaultsAreDimmed() {
        let dark = EditorTheme.dark()
        let light = EditorTheme.light()

        XCTAssertLessThan(dark.inlayHintTextAlpha, 1.0)
        XCTAssertLessThan(light.inlayHintTextAlpha, 1.0)
        XCTAssertEqual(dark.inlayHintTextAlpha, 0.55, accuracy: 0.001)
        XCTAssertEqual(light.inlayHintTextAlpha, 0.55, accuracy: 0.001)
    }

    func testPhantomTextAlphaDefaultsAreDimmed() {
        let dark = EditorTheme.dark()
        let light = EditorTheme.light()

        XCTAssertLessThan(dark.phantomTextAlpha, 1.0)
        XCTAssertLessThan(light.phantomTextAlpha, 1.0)
        XCTAssertEqual(dark.phantomTextAlpha, 0.45, accuracy: 0.001)
        XCTAssertEqual(light.phantomTextAlpha, 0.45, accuracy: 0.001)
    }

    func testThemeIncludesSeparatorAndInlayIconColors() {
        let dark = EditorTheme.dark()
        let light = EditorTheme.light()

        XCTAssertGreaterThan(dark.separatorLineColor.alpha, 0)
        XCTAssertGreaterThan(light.separatorLineColor.alpha, 0)
        XCTAssertGreaterThan(dark.inlayHintIconColor.alpha, 0)
        XCTAssertGreaterThan(light.inlayHintIconColor.alpha, 0)
    }

    func testARGBColorConversionPreservesZeroAlpha() {
        let color = EditorRenderer.cgColorFromARGB(Int32(bitPattern: 0x00000000))

        XCTAssertEqual(color.alpha, 0, accuracy: 0.0001)
    }

    func testModelFoldRegionAPIAcceptsValueObjects() {
        let core = SweetEditorCore(fontSize: 14.0, fontName: "Menlo")
        core.setFoldRegions([
            SweetEditorCore.FoldRegion(startLine: 1, endLine: 3, collapsed: false),
            SweetEditorCore.FoldRegion(startLine: 10, endLine: 18, collapsed: true),
        ])
        XCTAssertTrue(true)
    }

    func testSetLineSpansWithEmptyArrayClearsPreviousLineHighlight() {
        let core = makeCoreWithSingleLineDocument("abc")
        let styleId: UInt32 = 101
        let markerColor = Int32(bitPattern: 0xFF00FF00)
        core.registerStyle(styleId: styleId, color: markerColor, fontStyle: 0)

        core.setLineSpans(
            line: 0,
            layer: 0,
            spans: [SweetEditorCore.StyleSpan(column: 0, length: 3, styleId: styleId)]
        )

        let before = core.buildRenderModel()
        XCTAssertTrue(containsStyleColor(markerColor, in: before))

        core.setLineSpans(line: 0, layer: 0, spans: [])
        let after = core.buildRenderModel()
        XCTAssertFalse(containsStyleColor(markerColor, in: after))
    }

    func testSetLineDiagnosticsWithEmptyArrayClearsPreviousLineDiagnostics() {
        let core = makeCoreWithSingleLineDocument("abc")

        core.setLineDiagnostics(
            line: 0,
            items: [SweetEditorCore.DiagnosticItem(column: 0, length: 2, severity: 0, color: 0)]
        )
        let before = core.buildRenderModel()
        XCTAssertGreaterThan(before?.diagnostic_decorations.count ?? 0, 0)

        core.setLineDiagnostics(line: 0, items: [])
        let after = core.buildRenderModel()
        XCTAssertEqual(after?.diagnostic_decorations.count ?? 0, 0)
    }

    func testMacViewSupportsEditorIconProvider() {
        let view = SweetEditorViewMacOS(frame: NSRect(x: 0, y: 0, width: 320, height: 160))
        let provider = StubIconProvider()
        view.editorIconProvider = provider
        XCTAssertNotNil(view.editorIconProvider)
    }

    func testRendererDrawsScrollbarThumbAndRequestsTransientRefresh() {
        let core = SweetEditorCore(fontSize: 14.0, fontName: "Menlo")
        let context = makeBitmapContext(width: 120, height: 120)
        let model = makeRenderModel(
            viewportWidth: 120,
            viewportHeight: 120,
            verticalScrollbar: ScrollbarModel(
                visible: true,
                alpha: 0.5,
                track: ScrollbarRect(origin: PointData(x: 108, y: 0), width: 12, height: 108),
                thumb: ScrollbarRect(origin: PointData(x: 108, y: 16), width: 12, height: 32)
            ),
            horizontalScrollbar: ScrollbarModel(
                visible: true,
                alpha: 0.35,
                track: ScrollbarRect(origin: PointData(x: 0, y: 108), width: 108, height: 12),
                thumb: ScrollbarRect(origin: PointData(x: 20, y: 108), width: 36, height: 12)
            )
        )

        let needsRefresh = EditorRenderer.draw(
            context: context,
            model: model,
            core: core,
            viewHeight: 120,
            iconProvider: nil,
            isCursorBlinkVisible: false
        )

        XCTAssertTrue(needsRefresh)
        XCTAssertNotEqual(pixelARGB(context: context, x: 114, y: 24), pixelARGB(context: context, x: 4, y: 4))
        XCTAssertNotEqual(pixelARGB(context: context, x: 28, y: 114), pixelARGB(context: context, x: 4, y: 4))
        XCTAssertNotEqual(pixelARGB(context: context, x: 108, y: 24), pixelARGB(context: context, x: 114, y: 24))
    }

    func testCoreScrollbarConfigCanDisableScrollbars() {
        let core = SweetEditorCore(fontSize: 14.0, fontName: "Menlo")
        core.setViewport(width: 160, height: 80)
        core.setDocument(SweetDocument(text: Array(repeating: "long long line for scrolling", count: 60).joined(separator: "\n")))
        core.setScrollbarConfig(
            SweetEditorCore.ScrollbarConfig(
                thickness: 12.0,
                minThumb: 24.0,
                thumbHitPadding: 0.0,
                mode: .NEVER,
                thumbDraggable: true,
                trackTapMode: .JUMP,
                fadeDelayMs: 700,
                fadeDurationMs: 300
            )
        )

        let model = core.buildRenderModel()

        XCTAssertFalse(model?.vertical_scrollbar.visible ?? true)
        XCTAssertFalse(model?.horizontal_scrollbar.visible ?? true)
    }

    func testDefaultScrollbarConfigMatchesTransientParity() {
        let view = SweetEditorViewMacOS(frame: NSRect(x: 0, y: 0, width: 320, height: 160))
        let core = readPrivateValue(view, key: "editorCore", as: SweetEditorCore.self)
        let config = core?.scrollbarConfig

        XCTAssertEqual(Double(config?.thickness ?? .nan), 8.0, accuracy: 0.001)
        XCTAssertEqual(Double(config?.minThumb ?? .nan), 48.0, accuracy: 0.001)
        XCTAssertEqual(Double(config?.thumbHitPadding ?? .nan), 16.0, accuracy: 0.001)
        XCTAssertEqual(config?.mode, .TRANSIENT)
        XCTAssertEqual(config?.trackTapMode, .DISABLED)
        XCTAssertEqual(config?.fadeDelayMs, 700)
        XCTAssertEqual(config?.fadeDurationMs, 300)
    }

    func testMacViewExposesDecorationParityAPIs() {
        let view = SweetEditorViewMacOS(frame: NSRect(x: 0, y: 0, width: 320, height: 160))

        view.registerStyle(styleId: 1, color: Int32(bitPattern: 0xFF00FF00), fontStyle: 0)
        view.registerStyle(styleId: 2, color: Int32(bitPattern: 0xFF00FF00), backgroundColor: Int32(bitPattern: 0x20000000), fontStyle: 1)

        view.setLineSpans(line: 0, layer: .syntax, spans: [
            SweetEditorCore.StyleSpan(column: 0, length: 1, styleId: 1),
        ])
        view.setBatchLineSpans(layer: .semantic, spansByLine: [
            0: [SweetEditorCore.StyleSpan(column: 1, length: 1, styleId: 2)],
        ])

        view.setLineInlayHints(line: 0, hints: [.text(column: 0, text: "x")])
        view.setBatchLineInlayHints([0: [.color(column: 1, color: Int32(bitPattern: 0xFFFF0000))]])

        view.setLinePhantomTexts(line: 0, phantoms: [SweetEditorCore.PhantomTextPayload(column: 0, text: "ghost")])
        view.setBatchLinePhantomTexts([0: [SweetEditorCore.PhantomTextPayload(column: 1, text: "g")]])

        view.setLineGutterIcons(line: 0, icons: [SweetEditorCore.GutterIcon(iconId: 1)])
        view.setBatchLineGutterIcons([0: [SweetEditorCore.GutterIcon(iconId: 2)]])
        view.setMaxGutterIcons(2)

        view.setLineDiagnostics(line: 0, items: [SweetEditorCore.DiagnosticItem(column: 0, length: 1, severity: 0, color: 0)])
        view.setBatchLineDiagnostics([0: [SweetEditorCore.DiagnosticItem(column: 1, length: 1, severity: 1, color: 0)]])

        view.setIndentGuides([
            SweetEditorCore.IndentGuidePayload(startLine: 0, startColumn: 0, endLine: 1, endColumn: 0),
        ])
        view.setBracketGuides([
            SweetEditorCore.BracketGuidePayload(
                parentLine: 0,
                parentColumn: 0,
                endLine: 1,
                endColumn: 1,
                children: [(line: 0, column: 1)]
            ),
        ])
        view.setFlowGuides([
            SweetEditorCore.FlowGuidePayload(startLine: 0, startColumn: 0, endLine: 1, endColumn: 1),
        ])
        view.setSeparatorGuides([
            SweetEditorCore.SeparatorGuidePayload(line: 0, style: 0, count: 1, textEndColumn: 0),
        ])

        view.setFoldRegions([SweetEditorCore.FoldRegion(startLine: 0, endLine: 1, collapsed: false)])

        view.clearHighlights()
        view.clearHighlights(layer: .syntax)
        view.clearInlayHints()
        view.clearPhantomTexts()
        view.clearGutterIcons()
        view.clearGuides()
        view.clearDiagnostics()
        view.clearAllDecorations()

        XCTAssertTrue(true)
    }

    func testMacViewExposesInlayAndGutterCallbacks() {
        let view = SweetEditorViewMacOS(frame: NSRect(x: 0, y: 0, width: 320, height: 160))
        view.onInlayHintClick = { _ in }
        view.onGutterIconClick = { _ in }
        XCTAssertNotNil(view.onInlayHintClick)
        XCTAssertNotNil(view.onGutterIconClick)
    }

    func testMacViewInstallsMouseMovedTrackingForScrollbarHoverReveal() {
        let view = SweetEditorViewMacOS(frame: NSRect(x: 0, y: 0, width: 320, height: 160))

        view.updateTrackingAreas()

        XCTAssertTrue(
            view.trackingAreas.contains {
                $0.options.contains(.mouseMoved)
                    && $0.options.contains(.activeInKeyWindow)
                    && $0.options.contains(.inVisibleRect)
            }
        )
    }

    func testScrollbarHoverRevealUsesLastVisibleTrackWhenCurrentModelIsHidden() {
        let visibleModel = makeRenderModel(
            viewportWidth: 120,
            viewportHeight: 120,
            verticalScrollbar: ScrollbarModel(
                visible: true,
                alpha: 1.0,
                track: ScrollbarRect(origin: PointData(x: 108, y: 0), width: 12, height: 108),
                thumb: ScrollbarRect(origin: PointData(x: 108, y: 16), width: 12, height: 32)
            ),
            horizontalScrollbar: ScrollbarModel(
                visible: false,
                alpha: 0,
                track: ScrollbarRect(origin: PointData(x: 0, y: 0), width: 0, height: 0),
                thumb: ScrollbarRect(origin: PointData(x: 0, y: 0), width: 0, height: 0)
            )
        )
        let hiddenModel = makeRenderModel(
            viewportWidth: 120,
            viewportHeight: 120,
            verticalScrollbar: ScrollbarModel(
                visible: false,
                alpha: 0,
                track: ScrollbarRect(origin: PointData(x: 0, y: 0), width: 0, height: 0),
                thumb: ScrollbarRect(origin: PointData(x: 0, y: 0), width: 0, height: 0)
            ),
            horizontalScrollbar: ScrollbarModel(
                visible: false,
                alpha: 0,
                track: ScrollbarRect(origin: PointData(x: 0, y: 0), width: 0, height: 0),
                thumb: ScrollbarRect(origin: PointData(x: 0, y: 0), width: 0, height: 0)
            )
        )

        let cachedRegions = ScrollbarHoverReveal.cachedRegions(from: nil, latestModel: visibleModel)

        XCTAssertTrue(
            ScrollbarHoverReveal.shouldReveal(
                at: CGPoint(x: 114, y: 24),
                currentModel: hiddenModel,
                cachedRegions: cachedRegions
            )
        )
    }

    func testScrollbarHoverRevealBuildsInitialRegionsFromScrollMetrics() {
        let metrics = SweetEditorCore.ScrollMetrics(
            scale: 1.0,
            scrollX: 0,
            scrollY: 0,
            maxScrollX: 0,
            maxScrollY: 400,
            contentWidth: 100,
            contentHeight: 800,
            viewportWidth: 120,
            viewportHeight: 120,
            textAreaX: 8,
            textAreaWidth: 112,
            canScrollX: false,
            canScrollY: true
        )

        let cachedRegions = ScrollbarHoverReveal.cachedRegions(
            from: nil,
            latestModel: nil,
            fallbackMetrics: metrics,
            scrollbarConfig: SweetEditorCore.ScrollbarConfig(
                thickness: 12,
                minThumb: 48,
                thumbHitPadding: 16,
                mode: .TRANSIENT,
                thumbDraggable: true,
                trackTapMode: .DISABLED,
                fadeDelayMs: 700,
                fadeDurationMs: 300
            )
        )

        XCTAssertTrue(
            ScrollbarHoverReveal.shouldReveal(
                at: CGPoint(x: 114, y: 24),
                currentModel: nil,
                cachedRegions: cachedRegions
            )
        )
    }

    func testCursorBlinkVisibilityFollowsResponderLifecycle() {
        let view = SweetEditorViewMacOS(frame: NSRect(x: 0, y: 0, width: 800, height: 600))

        XCTAssertTrue(readPrivateBool(view, key: "isCursorBlinkVisible"))

        _ = view.resignFirstResponder()
        XCTAssertFalse(readPrivateBool(view, key: "isCursorBlinkVisible"))

        _ = view.becomeFirstResponder()
        XCTAssertTrue(readPrivateBool(view, key: "isCursorBlinkVisible"))
    }

    private func readPrivateBool(_ object: Any, key: String) -> Bool {
        let mirror = Mirror(reflecting: object)
        return mirror.children.first(where: { $0.label == key })?.value as? Bool ?? false
    }

    private func readPrivateValue<T>(_ object: Any, key: String, as _: T.Type) -> T? {
        let mirror = Mirror(reflecting: object)
        return mirror.children.first(where: { $0.label == key })?.value as? T
    }

    private func makeCoreWithSingleLineDocument(_ text: String) -> SweetEditorCore {
        let core = SweetEditorCore(fontSize: 14.0, fontName: "Menlo")
        core.setViewport(width: 640, height: 480)
        core.setDocument(SweetDocument(text: text))
        return core
    }

    private func containsStyleColor(_ color: Int32, in model: EditorRenderModel?) -> Bool {
        guard let model else { return false }
        for line in model.lines {
            for run in line.runs where run.style.color == color {
                return true
            }
        }
        return false
    }

    private func makeRenderModel(viewportWidth: Float,
                                 viewportHeight: Float,
                                 verticalScrollbar: ScrollbarModel,
                                 horizontalScrollbar: ScrollbarModel) -> EditorRenderModel {
        EditorRenderModel(
            split_x: 0,
            scroll_x: 0,
            scroll_y: 0,
            viewport_width: viewportWidth,
            viewport_height: viewportHeight,
            current_line: PointData(x: 0, y: 0),
            lines: [],
            cursor: Cursor(text_position: TextPositionData(line: 0, column: 0), position: PointData(x: 0, y: 0), height: 0, visible: false, show_dragger: false),
            selection_rects: [],
            selection_start_handle: SelectionHandle(position: PointData(x: 0, y: 0), height: 0, visible: false),
            selection_end_handle: SelectionHandle(position: PointData(x: 0, y: 0), height: 0, visible: false),
            composition_decoration: CompositionDecoration(active: false, origin: PointData(x: 0, y: 0), width: 0, height: 0),
            guide_segments: [],
            diagnostic_decorations: [],
            max_gutter_icons: 0,
            fold_arrow_x: 0,
            linked_editing_rects: [],
            bracket_highlight_rects: [],
            vertical_scrollbar: verticalScrollbar,
            horizontal_scrollbar: horizontalScrollbar
        )
    }

    private func makeBitmapContext(width: Int, height: Int) -> CGContext {
        guard let context = CGContext(
            data: nil,
            width: width,
            height: height,
            bitsPerComponent: 8,
            bytesPerRow: width * 4,
            space: CGColorSpaceCreateDeviceRGB(),
            bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue
        ) else {
            fatalError("Failed to create test bitmap context")
        }
        return context
    }

    private func pixelARGB(context: CGContext, x: Int, y: Int) -> UInt32 {
        guard let data = context.data else {
            return 0
        }
        let bytes = data.bindMemory(to: UInt8.self, capacity: context.bytesPerRow * context.height)
        let offset = y * context.bytesPerRow + x * 4
        let r = UInt32(bytes[offset])
        let g = UInt32(bytes[offset + 1])
        let b = UInt32(bytes[offset + 2])
        let a = UInt32(bytes[offset + 3])
        return (a << 24) | (r << 16) | (g << 8) | b
    }

    func testDecorationProviderDoesNotRefreshOnScrollWhenVisibleRangeUnchanged() {
        let core = SweetEditorCore(fontSize: 14.0, fontName: "Menlo")
        var visibleRange = (0, 10)
        let provider = CountingDecorationProvider()

        let firstCall = expectation(description: "initial provider call")
        provider.onProvide = { _ in
            if provider.callCount == 1 {
                firstCall.fulfill()
            }
        }

        let manager = DecorationProviderManager(
            core: core,
            visibleLineRangeProvider: { visibleRange },
            totalLineCountProvider: { 100 },
            languageConfigurationProvider: { nil },
            onApplied: {}
        )

        manager.addProvider(provider)
        wait(for: [firstCall], timeout: 1.0)
        let baselineCalls = provider.callCount

        visibleRange = (0, 10)
        manager.onScrollChanged()
        RunLoop.main.run(until: Date().addingTimeInterval(0.2))

        XCTAssertEqual(provider.callCount, baselineCalls)
    }

    func testDecorationProviderRefreshesOnScrollWhenVisibleRangeChanged() {
        let core = SweetEditorCore(fontSize: 14.0, fontName: "Menlo")
        var visibleRange = (0, 10)
        let provider = CountingDecorationProvider()

        let initialCall = expectation(description: "initial provider call")
        provider.onProvide = { _ in
            if provider.callCount == 1 {
                initialCall.fulfill()
            }
        }

        let manager = DecorationProviderManager(
            core: core,
            visibleLineRangeProvider: { visibleRange },
            totalLineCountProvider: { 100 },
            languageConfigurationProvider: { nil },
            onApplied: {}
        )

        manager.addProvider(provider)
        wait(for: [initialCall], timeout: 1.0)

        let scrollCall = expectation(description: "scroll-triggered provider call")
        provider.onProvide = { _ in
            if provider.callCount >= 2 {
                scrollCall.fulfill()
            }
        }

        visibleRange = (1, 11)
        manager.onScrollChanged()
        wait(for: [scrollCall], timeout: 1.0)
    }

    func testDecorationProviderRefreshesOnTextChangedEvenWhenVisibleRangeUnchanged() {
        let core = SweetEditorCore(fontSize: 14.0, fontName: "Menlo")
        let visibleRange = (0, 10)
        let provider = CountingDecorationProvider()

        let initialCall = expectation(description: "initial provider call")
        provider.onProvide = { _ in
            if provider.callCount == 1 {
                initialCall.fulfill()
            }
        }

        let manager = DecorationProviderManager(
            core: core,
            visibleLineRangeProvider: { visibleRange },
            totalLineCountProvider: { 100 },
            languageConfigurationProvider: { nil },
            onApplied: {}
        )

        manager.addProvider(provider)
        wait(for: [initialCall], timeout: 1.0)

        let textChangedCall = expectation(description: "text-change-triggered provider call")
        provider.onProvide = { _ in
            if provider.callCount >= 2 {
                textChangedCall.fulfill()
            }
        }

        manager.onTextChanged(changes: [])
        wait(for: [textChangedCall], timeout: 1.0)
    }

    func testDecorationContextAccumulatesTextChangesDuringDebounceWindow() {
        let core = SweetEditorCore(fontSize: 14.0, fontName: "Menlo")
        let visibleRange = (0, 10)
        let provider = CountingDecorationProvider()
        var capturedContext: DecorationContext?

        let initialCall = expectation(description: "initial provider call")
        provider.onProvide = { _ in
            if provider.callCount == 1 {
                initialCall.fulfill()
            }
        }

        let manager = DecorationProviderManager(
            core: core,
            visibleLineRangeProvider: { visibleRange },
            totalLineCountProvider: { 100 },
            languageConfigurationProvider: { nil },
            onApplied: {}
        )

        manager.addProvider(provider)
        wait(for: [initialCall], timeout: 1.0)

        let textChangedCall = expectation(description: "batched text-change provider call")
        provider.onProvide = { context in
            if provider.callCount == 2 {
                capturedContext = context
                textChangedCall.fulfill()
            }
        }

        let first = TextChange(
            range: SweetEditorCoreInternal.TextRange(
                start: SweetEditorCoreInternal.TextPosition(line: 1, column: 2),
                end: SweetEditorCoreInternal.TextPosition(line: 1, column: 2)
            ),
            newText: "A"
        )
        let second = TextChange(
            range: SweetEditorCoreInternal.TextRange(
                start: SweetEditorCoreInternal.TextPosition(line: 3, column: 4),
                end: SweetEditorCoreInternal.TextPosition(line: 3, column: 6)
            ),
            newText: "BC"
        )

        manager.onTextChanged(changes: [first])
        manager.onTextChanged(changes: [second])
        wait(for: [textChangedCall], timeout: 1.0)

        XCTAssertEqual(capturedContext?.textChanges.count, 2)
        XCTAssertEqual(capturedContext?.textChanges[0].newText, "A")
        XCTAssertEqual(capturedContext?.textChanges[1].newText, "BC")
        XCTAssertEqual(capturedContext?.textChanges[0].range.start.line, 1)
        XCTAssertEqual(capturedContext?.textChanges[1].range.end.column, 6)
    }
}

private final class StubIconProvider: EditorIconProvider {
    func iconImage(for iconId: Int32) -> CGImage? {
        nil
    }
}

private final class CountingDecorationProvider: DecorationProvider {
    var capabilities: DecorationType = []
    private(set) var callCount = 0
    var onProvide: ((DecorationContext) -> Void)?

    func provideDecorations(context: DecorationContext, receiver: DecorationReceiver) {
        callCount += 1
        onProvide?(context)
    }
}
