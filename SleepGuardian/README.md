# SleepGuardian 睡眠引導 APP

Android 原生 APP，在使用者設定的睡覺時間後，於背景依時間軸漸進調整亮度、灰階、可選的網路節流與螢幕更新率，引導使用者放下手機入睡。主畫面可選擇「組合包」或「各自功能」，並可開啟**每日自動開始**；各能力以獨立函式封裝，方便之後擴充。

---

## 系統架構

```
MainActivity                 ← UI、權限與 VPN 準備、服務啟停、每日開關、Demo 入口
    │                           ADB 提示對話框（一鍵複製指令）
    │
    ├── SleepService         ← 前景服務（時間軸 tick / Demo 快轉）
    │       ├── 系統亮度控制     (Settings.System.SCREEN_BRIGHTNESS)
    │       ├── 灰階 Overlay     (WindowManager TYPE_APPLICATION_OVERLAY)
    │       ├── 系統灰階切換     (Settings.Secure，需 ADB 授權，選用)
    │       ├── 螢幕更新率       (Settings.System peak/min_refresh_rate，需 WRITE_SETTINGS)
    │       ├── 降網速指令       → PulseThrottleVpnService
    │       └── 手動還原偵測     每 30 秒比對系統亮度／灰階狀態；偵測到覆蓋後暫停 5 分鐘再恢復
    │
    ├── PulseThrottleVpnService  ← VpnService：脈衝式節流（近似降速，無 root）
    │
    ├── SleepScheduleHelper      ← AlarmManager 每日排程
    │       ├── scheduleNextOccurrence    ← 排定「下一次」（今天已過則明天）
    │       └── scheduleNextDaySameTime   ← 鬧鐘觸發後排「隔天同一時刻」
    ├── SleepAlarmReceiver       ← 到點檢查權限；齊全則啟動服務，缺少則發通知提醒
    └── BootReceiver             ← 開機後若每日排程開啟則重新排程
```

---

## 勸睡模式（主畫面選單）


| 模式             | 說明                         |
| -------------- | -------------------------- |
| **組合包**        | 亮度＋灰階＋降網速＋降更新率（依下方時間軸）     |
| **各自功能：亮度＋灰階** | 僅原有漸暗與灰階                   |
| **各自功能：僅降網速**  | 僅在 T+10 起啟用 VPN 節流         |
| **各自功能：僅降更新率** | 僅在 T+10 起依曲線降低更新率（最低 60Hz） |


模式會寫入 `SharedPreferences`，與睡覺／起床時間一併持久化。

---

## 每日自動開始

- 主畫面 **Switch「每日自動開始」**（`SleepScheduleHelper.KEY_DAILY_SCHEDULE`）。
- 使用 `**AlarmManager.setAlarmClock`** 排定「下一次」預計睡覺時間（今天已過則明天，透過 `scheduleNextOccurrence`）；鬧鐘圖示可點回 App。
- `**SleepAlarmReceiver**`：到點後先檢查必要權限（Overlay、WRITE_SETTINGS、通知）；
  - 若**權限齊全**：依偏好啟動 `SleepService`。
  - 若**缺少權限**：改發一則高優先度的「缺少權限」通知，提示使用者點擊開 App 補授權；**下一晚的鬧鐘仍照常排定**。
  - 無論成功與否，`**finally` 內都呼叫 `scheduleNextDaySameTime`**（固定排在隔天同一時刻），避免鏈條中斷。
- `**BootReceiver**`（`RECEIVE_BOOT_COMPLETED`）：開機後若每日排程為開啟，會重新排定下一次。
- 修改「預計睡覺時間」且每日排程為開啟時，會 **重新排程**；手動「開始引導」成功且每日開啟時，也會 **同步排程**。

---

## Demo 模式（內部展示用）

- 主畫面底部低調文字 **「demo模式」**（半透明小字，不搶主流程）。
- 約 **60 秒**內依序模擬四種勸睡差異（每段各 **15 秒**）：
  1. 組合包
  2. 亮度＋灰階
  3. 僅降網速
  4. 僅降更新率
- 每段內以 **虛擬時間快轉**（約 0～32 分鐘效果壓縮在 15 秒內），並在 **段與段之間** 還原亮度、網速、更新率、灰階，避免上一段殘留。
- 前景通知顯示當前段落（`Demo 1/4・組合包・漸暗模式` 等）與剩餘秒數。
- 需與正式流程相同之權限；**含 VPN**（才能展示降網速段落）。
- 若一般睡眠引導已在執行中，需先停止再跑 Demo；`SleepService.isDemoModeActive` 供 UI 判斷。

---

## 必要權限


| 權限                       | 用途                          | 授予方式                    |
| ------------------------ | --------------------------- | ----------------------- |
| `SYSTEM_ALERT_WINDOW`    | 在所有 App 上方顯示灰階 Overlay      | App 內引導使用者至系統設定授予       |
| `WRITE_SETTINGS`         | 修改螢幕亮度、嘗試修改螢幕更新率            | App 內引導使用者至系統設定授予       |
| `FOREGROUND_SERVICE`     | 讓背景服務不被系統殺掉                 | 安裝時自動授予                 |
| `POST_NOTIFICATIONS`     | 顯示前景服務常駐通知                  | Android 13+ 需使用者同意      |
| `INTERNET`               | VpnService 相關行為所需           | 安裝時宣告                   |
| `RECEIVE_BOOT_COMPLETED` | 開機後恢復每日鬧鐘排程                 | 安裝時宣告                   |
| **VPN（系統對話框）**           | 啟用降網速時由系統詢問是否允許此 App 建立 VPN | 「組合包」或「僅降網速」、以及 Demo 模式 |
| `WRITE_SECURE_SETTINGS`  | 切換真正的系統灰階模式                 | **需手動執行 ADB 指令**（選用）    |


> **降網速說明**：無 root 環境下無法用一般 API 全系統精準限速；目前透過 `VpnService` 做**脈衝式節流**（週期阻斷／放行流量），屬**近似降速**，目標檔位以 kbps 表示；實際體感依網路與 App 而異。

---

## 時間軸邏輯

睡覺時間設為 `T`，起床時間設為 `W`（預設 05:00），服務啟動後每 **30 秒** tick 一次（**Demo 模式**改為較快間隔，僅在 Demo 期間）。

```
T - ∞  ～  T + 0 min   等待中           通知：顯示倒數時間
T + 0  ～  T + 3 min   就寢時刻         通知：「睡覺時間到了」
T + 3  min              ← 開始降亮（僅組合包或「亮度＋灰階」模式）
T + 3  ～  T + 15 min   漸暗模式         每 3 分鐘降低亮度 3%
T + 10 min              ← 開始降網速與更新率曲線（僅組合包或對應單獨模式）
T + 15 min              ← 開始灰階（同上，非「僅網速／僅更新率」時）
T + 15 ～  W            灰階模式         亮度繼續降 + 灰階逐步加深（適用模式）
W                       ← 自動關閉     恢復亮度、灰階、更新率；停止 VPN 節流
```

---

## 手動還原後自動恢復

若使用者在引導期間把亮度或灰階手動調回來，服務每 30 秒的 tick 都會偵測到還原，並進入 **5 分鐘寬限期**：

1. **立即暫停**所有亮度與灰階調整（網速／更新率調整不受影響）
2. **通知欄倒數**：「偵測到手動調整亮度／灰階，將在 N 分鐘後自動恢復 💤」（每 30 秒更新）
3. **寬限期結束後**：以使用者當下的亮度為新基準，重新開始漸暗與灰階調整

### 偵測條件

| 項目 | 偵測方式 |
| --- | --- |
| **亮度** | 系統亮度比服務上次寫入值高出超過 20（0–255 範圍，約 8%） |
| **系統灰階** | 透過 ADB 授權啟用系統灰階後，若讀到 `accessibility_display_daltonizer_enabled = 0` |

> 純降網速模式（`MODE_NETWORK_ONLY`）和純降更新率模式（`MODE_REFRESH_ONLY`）本來就不調整亮度與灰階，不觸發此偵測。Overlay 灰階由服務直接控制，使用者無法單獨關掉，所以 Overlay 模式只偵測亮度還原。

---

## 亮度控制細節

- **第一次觸發**：T+3 立即降低 3%（步數從 1 開始）
- **計算公式**：`步數 = floor((經過分鐘 - 3) / 3) + 1`
- **每步降幅**：原始亮度 × 3%
- **最大降幅**：原始亮度的 75%（最暗只降到原本的 25%，避免全黑）
- **實作方式**：寫入 `Settings.System.SCREEN_BRIGHTNESS`（0–255），同時強制關閉自動亮度
- **停止時**：自動恢復原始亮度，並重新開啟自動亮度

```
T+3  min → 亮度 -3%
T+6  min → 亮度 -6%
T+9  min → 亮度 -9%
T+12 min → 亮度 -12%
...（每 3 分鐘繼續）
上限    → 亮度 -75%（原本 25% 亮度）
```

---

## 灰階效果細節

App 有兩套灰階方案，每次套用時**優先嘗試系統灰階，失敗則自動退回 Overlay**：

### 方案 A：Overlay 灰階（預設，不需額外授權）

在所有 App 上方疊加半透明灰色 Overlay（`WindowManager TYPE_APPLICATION_OVERLAY`）。

```
T+15 min → 灰階 Overlay 20%
T+18 min → 灰階 Overlay 40%
T+21 min → 灰階 Overlay 60%
T+24 min → 灰階 Overlay 80%
T+27 min → 灰階 Overlay 100%（最大 75% 不透明度，視覺上接近全灰）
```

### 方案 B：真正系統灰階（需 ADB 一次性授權）

透過 `Settings.Secure.accessibility_display_daltonizer` 直接切換系統無障礙灰階，效果與手機內建「數位健康 → 就寢模式」相同。成功啟用後 App 會同時移除灰色 Overlay。

啟用方式（一次性，需 USB 連接電腦）：

```bash
adb shell pm grant com.sleepguardian android.permission.WRITE_SECURE_SETTINGS
```

> **主畫面 ADB 提示**：灰階設定區下方有「💡 灰階需一次性 ADB 授權（點我查看）」小字，點擊後跳出對話框顯示指令，並可一鍵複製到剪貼簿，方便在電腦端貼上執行。

### APK 分享給測試者的注意事項

`WRITE_SECURE_SETTINGS` 是系統敏感權限，**APK 安裝本身不會攜帶這個授權**，因為它是在裝置上以 ADB 個別授予的，和 APK 簽章無關。

| 情境 | 使用的灰階方案 |
| --- | --- |
| 開發者裝置（自行跑過 ADB 指令） | 方案 B（真正系統灰階） |
| 測試者裝置（只安裝 APK） | 方案 A（Overlay 灰階） |
| 測試者裝置（自行跑過 ADB 指令） | 方案 B（真正系統灰階） |

測試者若想體驗系統灰階，需在自己的電腦上連接裝置並執行同一道 ADB 指令。

---

## 降網速細節（PulseThrottleVpnService）

- **觸發條件**：組合包，或「各自功能：僅降網速」；自 **T+10 分鐘** 起隨時間軸升級檔位。
- **目標檔位（kbps，封頂最低約 500）**：`2500 → 1800 → 1300 → 900 → 700 → 500`（每 3 分鐘一階，與 `SleepService` 常數一致）。
- **實作**：`PulseThrottleVpnService` 前景服務，依目標調整阻斷比例；停止睡眠引導時會送出停止指令並關閉節流。

---

## 螢幕更新率細節

- **適用模式**：組合包，或「各自功能：僅降更新率」。
- **起始**：**T+10 分鐘**。
- **曲線**：每 **4 分鐘**一階，目標為 `110 → 100 → 90 → 80 → 72 → 60 Hz`；實際寫入前會對齊裝置 `Display.getSupportedModes()` 中 **≥60Hz** 的最接近支援值，**最低不低於 60Hz**。
- **寫入鍵**（需 `WRITE_SETTINGS`，且依廠商／Android 版本可能無效）：`peak_refresh_rate`、`min_refresh_rate`（後者至少鎖 60）。
- **停止時**：若曾成功讀取原始值，會嘗試還原 `peak_refresh_rate` / `min_refresh_rate`。

---

## 專案結構

```
SleepGuardian/
├── app/
│   └── src/main/
│       ├── AndroidManifest.xml          # 權限、VpnService、BroadcastReceiver
│       ├── java/com/sleepguardian/
│       │   ├── MainActivity.kt          # UI、權限鏈、每日開關、Demo 入口
│       │   ├── SleepService.kt          # 前景服務、時間軸、Demo 快轉
│       │   ├── PulseThrottleVpnService.kt
│       │   ├── SleepScheduleHelper.kt   # AlarmManager 每日排程
│       │   ├── SleepAlarmReceiver.kt    # 到點啟動服務
│       │   ├── BootReceiver.kt          # 開機重排
│       │   ├── SleepTimeCalculator.kt   # 下一個睡／醒時間（與手動開始共用）
│       │   └── StarfieldView.kt
│       └── res/
│           ├── layout/activity_main.xml # 勸睡模式、每日開關、demo 文字
│           └── values/
│               ├── strings.xml
│               ├── colors.xml
│               └── themes.xml
├── build.gradle
├── settings.gradle
└── gradle.properties
```

---

## 建置環境

- **Android Studio** Panda 3 / 2025.3.3 以上
- **compileSdk** 34
- **minSdk** 26（Android 8.0+）
- **targetSdk** 33
- **Kotlin** 1.9.22
- **AGP** 8.1.4

---

## 起床自動關閉細節

- **預設時間**：05:00
- **計算方式**：以睡覺時間為基準，找下一個出現的起床 HH:MM，確保一定在睡覺時間之後
  - 例：睡覺 23:00、起床 05:00 → 服務在隔天 05:00 自動停止
- **停止時**：顯示「早安 ☀️」通知 2 秒後服務停止；恢復亮度、關閉灰階、還原更新率設定、停止 VPN 節流
- **設定持久化**：記錄在 SharedPreferences，重新開 App 或服務重啟均保留設定
- **時間計算共用**：手動「開始引導」使用 `SleepTimeCalculator.computeNextSleepAndWakeMillis()`，與 README 前述「下一個睡／醒」邏輯一致

---

## 開發計畫 / 待處理

- 支援 Accessibility Service 版本（不需 SYSTEM_ALERT_WINDOW 即可套用灰階）
- 停止後是否恢復亮度由使用者選擇
- 小工具（Widget）顯示距離睡覺時間
- 降網速：可選更精準的封包級節流或使用者可調脈衝參數

