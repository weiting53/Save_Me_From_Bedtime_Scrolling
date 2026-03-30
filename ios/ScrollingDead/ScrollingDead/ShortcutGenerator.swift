import Foundation

/// Generates a Shortcuts-compatible .shortcut plist file.
/// Randomness is baked in at generation time so the shortcut
/// runs differently every time it's imported fresh.
struct ShortcutGenerator {

    struct Config {
        var waitMinutes: Int        // delay before anything happens (randomised)
        var brightnessStep1: Float  // first brightness drop  (0–1)
        var brightnessStep2: Float  // second brightness drop (0–1)
        var nightShiftOn: Bool      // warm colour shift
        var notifTitle: String
        var notifBody: String
    }

    static func randomConfig() -> Config {
        Config(
            waitMinutes:    Int.random(in: 3...10),
            brightnessStep1: Float.random(in: 0.30...0.50),
            brightnessStep2: Float.random(in: 0.10...0.25),
            nightShiftOn:   Bool.random(),
            notifTitle:     L.shortcutNotifTitle,
            notifBody:      L.shortcutNotifBody
        )
    }

    // MARK: - Public entry point

    /// Writes a .shortcut file to a temp location and returns the URL.
    /// Returns nil if serialisation fails.
    static func generate() -> URL? {
        let config = randomConfig()
        let plist  = buildPlist(config: config)
        return writePlist(plist)
    }

    // MARK: - Plist construction

    private static func buildPlist(config: Config) -> [String: Any] {
        var actions: [[String: Any]] = []

        // 1. Wait (baked-in random delay)
        actions.append(waitAction(minutes: config.waitMinutes))

        // 2. Set brightness — first drop
        actions.append(brightnessAction(value: config.brightnessStep1))

        // 3. Wait 2 more minutes
        actions.append(waitAction(minutes: 2))

        // 4. Grayscale on  (requires user to have Accessibility Shortcut set to Color Filters)
        actions.append(grayscaleAction(enabled: true))

        // 5. Optional Night Shift warm colour
        if config.nightShiftOn {
            actions.append(nightShiftAction(enabled: true))
        }

        // 6. Wait 3 more minutes
        actions.append(waitAction(minutes: 3))

        // 7. Second brightness drop
        actions.append(brightnessAction(value: config.brightnessStep2))

        // 8. Show notification
        actions.append(notificationAction(title: config.notifTitle, body: config.notifBody))

        return [
            "WFWorkflowActions":                    actions,
            "WFWorkflowClientVersion":              "1300",
            "WFWorkflowHasShortcutInputVariables":  false,
            "WFWorkflowIcon": [
                "WFWorkflowIconGlyphNumber":  59511,     // moon glyph
                "WFWorkflowIconStartColor":   2071128575 // dark purple
            ] as [String: Any],
            "WFWorkflowImportQuestions":            [] as [Any],
            "WFWorkflowInputContentItemClasses":    [] as [Any],
            "WFWorkflowMinimumClientVersion":       900,
            "WFWorkflowName":                       "Scrolling Dead",
            "WFWorkflowNoInputBehavior": [
                "Name":       "RunImmediately",
                "Parameters": [:] as [String: Any]
            ] as [String: Any],
            "WFWorkflowOutputContentItemClasses":   [] as [Any],
            "WFWorkflowTypes":                      [] as [Any]
        ]
    }

    // MARK: - Individual actions

    private static func waitAction(minutes: Int) -> [String: Any] {
        [
            "WFWorkflowActionIdentifier": "is.workflow.actions.wait",
            "WFWorkflowActionParameters": [
                "WFDuration": [
                    "Value": [
                        "Unit":  "Minutes",
                        "Value": Double(minutes)
                    ],
                    "WFSerializationType": "WFQuantityFieldValue"
                ] as [String: Any]
            ] as [String: Any]
        ]
    }

    private static func brightnessAction(value: Float) -> [String: Any] {
        [
            "WFWorkflowActionIdentifier": "is.workflow.actions.setbrightness",
            "WFWorkflowActionParameters": [
                "WFBrightness": Double(value)
            ] as [String: Any]
        ]
    }

    /// Toggles the system Color Filters (grayscale) via Accessibility.
    /// NOTE: The user must have set "Accessibility Shortcut → Color Filters"
    /// in Settings → Accessibility → Accessibility Shortcut.
    /// The onboarding guide prompts the user to do this once.
    private static func grayscaleAction(enabled: Bool) -> [String: Any] {
        [
            "WFWorkflowActionIdentifier": "is.workflow.actions.setvalueforaccessibilityfeature",
            "WFWorkflowActionParameters": [
                "WFAccessibilityFeature": "Color Filters",
                "Enabled":                enabled
            ] as [String: Any]
        ]
    }

    private static func nightShiftAction(enabled: Bool) -> [String: Any] {
        [
            "WFWorkflowActionIdentifier": "is.workflow.actions.nightshift.set",
            "WFWorkflowActionParameters": [
                "OnValue": enabled
            ] as [String: Any]
        ]
    }

    private static func notificationAction(title: String, body: String) -> [String: Any] {
        [
            "WFWorkflowActionIdentifier": "is.workflow.actions.shownotification",
            "WFWorkflowActionParameters": [
                "WFNotificationActionTitle":      title,
                "WFNotificationActionBody":       body,
                "WFNotificationActionPlaySound":  false
            ] as [String: Any]
        ]
    }

    // MARK: - File writing

    private static func writePlist(_ plist: [String: Any]) -> URL? {
        do {
            let data = try PropertyListSerialization.data(
                fromPropertyList: plist,
                format: .binary,
                options: 0
            )
            let url = FileManager.default.temporaryDirectory
                .appendingPathComponent("ScrollingDead.shortcut")
            try data.write(to: url, options: .atomic)
            return url
        } catch {
            print("[ShortcutGenerator] Failed to write plist: \(error)")
            return nil
        }
    }
}
