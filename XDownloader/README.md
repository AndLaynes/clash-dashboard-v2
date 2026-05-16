# XDownloader

Android app to download X (Twitter) videos in MP4 at the best available quality.
Functional equivalent of the `yt-dlp` Python script, implemented natively without yt-dlp.

## Requirements

- Android Studio Hedgehog or later
- JDK 17
- Android device / emulator — API 26 (Android 8.0) or higher

## How to Build

1. Open the `XDownloader/` directory as the project root in Android Studio.
2. Let Gradle sync finish.
3. Run on device or emulator with `Run > Run 'app'`.

## How to Import Cookies

1. Log in to X (Twitter) in a desktop browser.
2. Export your session cookies in **Netscape format** using a browser extension such as *Get cookies.txt LOCALLY*.
3. In the app, open **Settings** (bottom nav).
4. Tap **Import Cookie File** and select the `cookies.txt` file, **or** paste the file contents in the text field and tap **Import Pasted Cookie**.
5. The app extracts `auth_token` and `ct0`, which are stored in `EncryptedSharedPreferences`.

## How to Download

1. Copy a tweet URL from X (`https://x.com/user/status/...` or `https://twitter.com/...`).
2. Open **Home** and paste the URL.
3. Tap **Add to Queue**.
4. The video appears in the **Queue** tab. Once downloaded it is saved to `Downloads/XDownloader/` on the device.

## File Naming

`X_{tweetId}_{timestamp}.mp4`

## Permissions Used

| Permission | Reason |
|---|---|
| INTERNET | API calls and downloads |
| WRITE_EXTERNAL_STORAGE (≤ API 28) | Save files on older Android |
| READ_EXTERNAL_STORAGE (≤ API 32) | File picker on older Android |
| FOREGROUND_SERVICE | WorkManager long-running task |
| POST_NOTIFICATIONS | Download progress notification |

## Architecture

- **MVVM + Clean Architecture**: domain use cases, repository pattern, ViewModels
- **Room** — persists download queue with statuses: PENDING, DOWNLOADING, DONE, FAILED
- **WorkManager** — background processing with 5–12 s random cooldown between downloads
- **EncryptedSharedPreferences** — secure cookie storage
- **Hilt** — dependency injection
- **Jetpack Compose + Material 3** — UI
- **OkHttp 4** — HTTP (no Retrofit)
- **Android DownloadManager** — actual file transfer
