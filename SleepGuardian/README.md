# SleepGuardian 睡眠引導 APP

Android 原生 APP，在使用者設定的睡覺時間後，自動透過系統層級控制逐漸降低亮度並套用灰階效果，引導使用者放下手機入睡。

---

## 系統架構

```
MainActivity          ← 使用者介面、權限申請
    │
    └── SleepService  ← 前景背景服務（跨 App 持續執行）
            ├── 系統亮度控制  (Settings.System.SCREEN_BRIGHTNESS)
            ├── 灰階 Overlay  (WindowManager TYPE_APPLICATION_OVERLAY)
            └── 系統灰階切換 (Settings.Secure，需 ADB 授權)
```

---

## 必要權限

| 權限 | 用途 | 授予方式 |
|------|------|----------|
| `SYSTEM_ALERT_WINDOW` | 在所有 App 上方顯示灰階 Overlay | App 內引導使用者至系統設定授予 |
| `WRITE_SETTINGS` | 直接修改系統螢幕亮度 | App 內引導使用者至系統設定授予 |
| `FOREGROUND_SERVICE` | 讓背景服務不被系統殺掉 | 安裝時自動授予 |
| `POST_NOTIFICATIONS` | 顯示前景服務常駐通知 | Android 13+ 需使用者同意 |
| `WRITE_SECURE_SETTINGS` | 切換真正的系統灰階模式 | **需手動執行 ADB 指令**（選用） |

---

## 時間軸邏輯

睡覺時間設為 `T`，起床時間設為 `W`（預設 05:00），服務啟動後每 **30 秒** tick 一次。

```
T - ∞  ～  T + 0 min   等待中        通知：顯示倒數時間
T + 0  ～  T + 3 min   就寢時刻      通知：「睡覺時間到了」
T + 3  min             ← 開始降亮
T + 3  ～  T + 15 min  漸暗模式      每 3 分鐘降低亮度 3%
T + 15 min             ← 開始灰階
T + 15 ～  W           灰階模式      亮度繼續降 + 灰階逐步加深
W                      ← 自動關閉    恢復亮度、移除灰階，早安 ☀️
```

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

### 預設模式（不需額外授權）
在所有 App 上方疊加半透明灰色 Overlay（`WindowManager TYPE_APPLICATION_OVERLAY`）。

```
T+15 min → 灰階 Overlay 20%
T+18 min → 灰階 Overlay 40%
T+21 min → 灰階 Overlay 60%
T+24 min → 灰階 Overlay 80%
T+27 min → 灰階 Overlay 100%（最大 75% 不透明度，視覺上接近全灰）
```

### 升級模式（真正系統灰階）
透過 `Settings.Secure.accessibility_display_daltonizer` 直接切換系統無障礙灰階，效果與手機內建「數位健康 → 就寢模式」相同。

啟用方式（一次性，需 USB 連接電腦）：
```bash
adb shell pm grant com.sleepguardian android.permission.WRITE_SECURE_SETTINGS
```

授權後 APP 會自動偵測並優先使用系統灰階，同時移除灰色 Overlay。

---

## 專案結構

```
SleepGuardian/
├── app/
│   └── src/main/
│       ├── AndroidManifest.xml          # 權限宣告、元件註冊
│       ├── java/com/sleepguardian/
│       │   ├── MainActivity.kt          # UI、權限申請流程、服務啟停
│       │   └── SleepService.kt          # 前景服務、亮度控制、Overlay 管理
│       └── res/
│           ├── layout/activity_main.xml # 主畫面 XML 佈局
│           └── values/
│               ├── strings.xml
│               ├── colors.xml
│               └── themes.xml
├── build.gradle                         # 根層 Gradle 設定
├── settings.gradle                      # 模組設定
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
- **停止時**：顯示「早安 ☀️」通知 2 秒後服務停止，亮度恢復、灰階關閉
- **設定持久化**：記錄在 SharedPreferences，重新開 App 或服務重啟均保留設定

---

## 開發計畫 / 待處理

- [ ] 支援 Accessibility Service 版本（不需 SYSTEM_ALERT_WINDOW 即可套用灰階）
- [ ] 新增「快速測試模式」（加速時間軸，方便 Demo 效果）
- [ ] 新增每日排程（不用每天手動按開始）
- [ ] 停止後是否恢復亮度由使用者選擇
- [ ] 小工具（Widget）顯示距離睡覺時間
