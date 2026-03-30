import Foundation

struct L {

    // MARK: - Language detection
    static var isZh: Bool {
        let lang = Locale.current.language.languageCode?.identifier ?? ""
        return lang == "zh"
    }

    // MARK: - Onboarding
    static var appName:            String { isZh ? "Scrolling Dead"              : "Scrolling Dead" }
    static var bedtimeQuestion:    String { isZh ? "你希望幾點睡覺呢？"              : "When do you want to sleep?" }
    static var bedtimeSubtitle:    String { isZh ? "只需要設定一次，我們幫你做好其他的事"  : "Set it once. We handle the rest." }
    static var bedtimeLabel:       String { isZh ? "理想睡眠時間"                   : "Target sleep time" }
    static var installBtn:         String { isZh ? "取得捷徑 · 開始使用"             : "Get Shortcut · Start" }
    static var installingBtn:      String { isZh ? "安裝中…"                       : "Installing…" }
    static var installedBtn:       String { isZh ? "夜間模式已啟動 ✓"               : "Night mode active ✓" }
    static var installHint:        String { isZh ? "安裝後每晚全自動啟動\n不需要再設定任何東西"
                                                 : "Fully automatic every night\nNo further setup needed" }

    // MARK: - Setup guide (after install)
    static var guideTitle:         String { isZh ? "最後一步"                      : "One last step" }
    static var guideBody:          String { isZh ?
        "前往 捷徑 App → 自動化 → 新增個人自動化 → 時間\n\n設定時間為你睡覺前 15 分鐘（建議 \(reminderTimeString)），選擇剛匯入的「Scrolling Dead」捷徑，並關閉「執行前詢問」。"
        :
        "Open Shortcuts → Automation → New Personal Automation → Time of Day\n\nSet it to 15 min before your target (\(reminderTimeString) suggested), choose the \"Scrolling Dead\" shortcut, and turn off \"Ask Before Running\"."
    }
    static var guideDone:          String { isZh ? "我已設定完成"                   : "Done, all set" }

    // MARK: - Morning notification
    static var morningTitle:       String { isZh ? "早安 ☀️"                      : "Good morning ☀️" }
    static func morningBody(overMinutes: Int) -> String {
        if overMinutes <= 0 {
            return isZh ? "昨晚你準時放下手機，做得很好。" : "You put your phone down on time last night. Nice."
        }
        return isZh
            ? "昨晚比預計多滑了 \(overMinutes) 分鐘。今晚我們再試一次。"
            : "You scrolled \(overMinutes) min past your goal last night. Let's try again tonight."
    }

    // MARK: - Shortcut notification (shown by the Shortcut itself)
    static var shortcutNotifTitle: String { isZh ? "Scrolling Dead"              : "Scrolling Dead" }
    static var shortcutNotifBody:  String { isZh ? "夜間模式啟動中，手機正在幫你準備入睡。"
                                                 : "Night mode on. Winding down for sleep." }

    // MARK: - Helpers
    // Stored after user sets bedtime, used in guide copy
    static var reminderTimeString: String = "11:15 PM"
}
