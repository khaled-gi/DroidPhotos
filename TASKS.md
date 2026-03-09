# TASKS.md — Build Task List
# DroidPhotos for Android

## How to Use This File
Tasks are ordered sequentially. Each task must be completed and confirmed before the next begins. The agent must explain what it is going to do before writing any code, and must not proceed until the user approves. Tasks marked `[MVP]` are required for v1. Tasks marked `[POST-MVP]` are deferred to later versions.

---

## Phase 1 — Project Setup

### TASK-001 [MVP] — Initialize Android Project
- Create a new Android project in Kotlin
- Package name: `com.droidphotos`
- Minimum SDK: API 29 (Android 10)
- Target SDK: API 34 (Android 14)
- Enable Jetpack Compose
- Set up Hilt for dependency injection
- **Explain:** Describe each library being added and why before adding it
- **Confirm:** Show `build.gradle` (app and project level) before applying

### TASK-002 [MVP] — Add Core Dependencies
Add to `build.gradle`:
- Room (local database)
- WorkManager (background sync)
- Retrofit + OkHttp (API calls)
- Google Sign-In SDK
- Google Auth Library
- Hilt
- Kotlin Coroutines + Flow
- Material Design 3 (Compose)
- **Confirm:** Show full dependency list with version numbers before writing

### TASK-003 [MVP] — Add google-services.json
- Provide instructions for user to obtain `google-services.json` from Google Cloud Console
- Add placeholder location: `/app/google-services.json`
- Apply `google-services` plugin in build files
- **Pause:** Do not proceed until user confirms they have placed the file

### TASK-004 [MVP] — Declare Android Permissions
Add to `AndroidManifest.xml`:
- `INTERNET`
- `READ_MEDIA_IMAGES` (API 33+)
- `READ_MEDIA_VIDEO` (API 33+)
- `READ_EXTERNAL_STORAGE` (API 29–32 fallback)
- `POST_NOTIFICATIONS`
- `FOREGROUND_SERVICE`
- `RECEIVE_BOOT_COMPLETED`
- **Explain:** Why each permission is needed before adding

---

## Phase 2 — Authentication

### TASK-005 [MVP] — Build Sign-In Screen (UI)
- Create `SignInScreen.kt` using Jetpack Compose
- Show app name, tagline, and Google Sign-In button
- No functionality yet — UI skeleton only
- **Confirm:** Show layout plan before writing

### TASK-006 [MVP] — Implement Google Sign-In Flow
- Wire Google Sign-In SDK to the Sign-In button
- Handle success: navigate to Dashboard
- Handle failure: show error message
- Persist signed-in state so app skips Sign-In on relaunch
- **Explain:** Walk through the OAuth flow step by step before coding

### TASK-007 [MVP] — Runtime Permission Requests
- After sign-in, request `READ_MEDIA_IMAGES` / `READ_MEDIA_VIDEO` (or `READ_EXTERNAL_STORAGE` on older APIs)
- Request `POST_NOTIFICATIONS` permission (API 33+)
- Handle denial gracefully with explanation dialog
- **Confirm:** Show permission request flow before coding

---

## Phase 3 — Local Database

### TASK-008 [MVP] — Set Up Room Database
- Create `SyncDatabase.kt` with three tables:
  - `synced_files` (file_path, folder_name, media_item_id, status, error_message, synced_at)
  - `albums` (folder_name, google_album_id, created_at)
  - `sync_log` (event, detail, timestamp)
- Create DAOs for each table
- Create `SyncRepository.kt` as the single access point for all DB operations
- **Explain:** Describe the schema and why each column exists before coding

### TASK-009 [MVP] — Write Unit Tests for SyncDatabase
- Test insert and query for `synced_files`
- Test insert and query for `albums`
- Test status update (PENDING → SYNCED, PENDING → FAILED)
- Use in-memory Room database for tests

---

## Phase 4 — Folder Scanning

### TASK-010 [MVP] — Build FolderScanner
- Create `FolderScanner.kt`
- Use MediaStore API to enumerate all image/video files
- Group files by parent folder name
- Filter out blacklisted folders (read from SettingsManager)
- Return only files not already marked SYNCED in SyncDatabase
- Support file types: JPG, JPEG, PNG, DNG, CR2, MP4, MOV, GIF
- Handle API 29–32 vs API 33+ permission differences
- **Explain:** Walk through MediaStore query approach before coding

### TASK-011 [MVP] — Build SettingsManager
- Create `SettingsManager.kt` using SharedPreferences
- Store and retrieve: blacklisted folders (Set\<String\>), mobile data sync toggle
- Provide default values for all settings
- **Confirm:** Show settings schema before coding

### TASK-012 [MVP] — Write Unit Tests for FolderScanner
- Test blacklist filtering (blacklisted folders excluded)
- Test file type filtering (unsupported types excluded)
- Test deduplication (already-synced files not returned)
- Use mock MediaStore data

---

## Phase 5 — Google Photos API

### TASK-013 [MVP] — Build Retrofit API Client
- Create `GooglePhotosApiService.kt` using Retrofit
- Define endpoints:
  - `POST /v1/albums` (create album)
  - `POST /v1/uploads` (upload bytes)
  - `POST /v1/mediaItems:batchCreate` (create media items)
- Add OkHttp interceptor to inject `Authorization: Bearer {token}` header
- Add logging interceptor (debug builds only)
- **Explain:** Show the two-step upload process before coding

### TASK-014 [MVP] — Build AlbumManager
- Create `AlbumManager.kt`
- On first encounter of a folder: call API to create album, store ID in `albums` table
- On subsequent encounters: look up cached album ID, skip API call
- Album naming rules: use folder name directly; flatten nested paths with dashes
- Never touch albums the app did not create
- **Confirm:** Show album creation logic before coding

### TASK-015 [MVP] — Build GooglePhotosUploader
- Create `GooglePhotosUploader.kt`
- Implement two-step upload: bytes → uploadToken → mediaItem
- Batch up to 50 files per `batchCreate` call
- On success: mark file SYNCED in DB, store mediaItemId
- On failure: mark file FAILED, log error message and timestamp
- Implement exponential backoff on HTTP 429
- **Explain:** Walk through batching and retry logic before coding

### TASK-016 [MVP] — Write Unit Tests for AlbumManager and Uploader
- Test album creation and caching (no duplicate API calls)
- Test successful upload → SYNCED status in DB
- Test failed upload → FAILED status in DB with error message
- Test exponential backoff triggers on 429 response
- Use mock Retrofit responses

---

## Phase 6 — Sync Engine

### TASK-017 [MVP] — Build SyncWorker (WorkManager)
- Create `SyncWorker.kt` as a `CoroutineWorker`
- Sync cycle:
  1. Call FolderScanner → get unsynced files grouped by folder
  2. For each folder: call AlbumManager → ensure album exists
  3. Call GooglePhotosUploader → upload files in batches
  4. Update SyncDatabase with results
  5. Fire completion notification
- Register as `PeriodicWorkRequest` (15-minute interval)
- Re-register on device boot via `BootReceiver`
- **Explain:** Describe WorkManager lifecycle and constraints before coding

### TASK-018 [MVP] — Build BootReceiver
- Create `BootReceiver.kt` (BroadcastReceiver)
- On `BOOT_COMPLETED`: re-register the PeriodicWorkRequest
- Register in `AndroidManifest.xml`
- **Confirm:** Show manifest entry before adding

---

## Phase 7 — MVP UI

### TASK-019 [MVP] — Build Navigation Structure
- Set up Jetpack Compose Navigation (`NavHost`)
- Routes: `sign_in`, `dashboard`, `folders`, `log`, `settings`
- Bottom navigation bar with icons
- **Confirm:** Show navigation map before coding

### TASK-020 [MVP] — Build Dashboard Screen
- Overall sync status card (synced count, last sync time, status indicator)
- Per-folder sync status list (folder name, file count, last sync, status icon)
- Manual "Sync Now" button
- Pull-to-refresh
- **Confirm:** Show wireframe description before coding

### TASK-021 [POST-MVP] — Build Folder Manager Screen
- List of all detected folders with ON/OFF toggle
- Show mapped Google Photos album name per folder
- Rescan Folders button
- Excluded folders shown in muted style

### TASK-022 [POST-MVP] — Build Sync Log Screen
- Reverse-chronological list of sync events
- Filter bar: All / Synced / Failed / Skipped
- Failed entry detail view with error reason
- Export as CSV button

### TASK-023 [POST-MVP] — Build Settings Screen
- Signed-in account display
- Sign Out button
- Mobile data sync toggle
- Notification toggles
- Clear Sync History button
- App version display

---

## Phase 8 — Polish & Play Store Prep

### TASK-024 [POST-MVP] — Play Store Preparation
- Set up signing keystore
- Configure `build.gradle` for release build
- Write Play Store listing copy (short description, full description)
- Create privacy policy (required for apps using Google Sign-In)
- Submit OAuth consent screen for Google verification
- **Pause:** Review all steps with user before proceeding

### TASK-025 [POST-MVP] — Home Screen Widget
- Create `SyncStatusWidget.kt` using Glance (Jetpack Compose widgets)
- Show: last sync time, synced file count, status icon
- Tap to open Dashboard

### TASK-026 [POST-MVP] — Scheduled Sync Windows
- Add time window picker to Settings (start time / end time)
- Modify WorkManager constraints to only run within window
- Store schedule in SettingsManager

### TASK-027 [POST-MVP] — Multi-Account Support
- Refactor SyncDatabase to scope all records by account ID
- Update UI to show account switcher
- Independent sync state per account

### TASK-028 [POST-MVP] — Two-Way Sync
- Poll Google Photos API for album renames
- If album was created by this app and its title changed: update local `albums` table
- Surface rename suggestion to user (do not auto-rename local folder without confirmation)

---

## Task Status Tracker

| Task | Description | Status |
|---|---|---|
| TASK-001 | Initialize Android Project | ✅ Complete |
| TASK-002 | Add Core Dependencies | ✅ Complete |
| TASK-003 | Add google-services.json | ⬜ Not Started |
| TASK-004 | Declare Permissions | ⬜ Not Started |
| TASK-005 | Sign-In Screen UI | ⬜ Not Started |
| TASK-006 | Google Sign-In Flow | ⬜ Not Started |
| TASK-007 | Runtime Permission Requests | ⬜ Not Started |
| TASK-008 | Room Database Setup | ⬜ Not Started |
| TASK-009 | Unit Tests: SyncDatabase | ⬜ Not Started |
| TASK-010 | FolderScanner | ⬜ Not Started |
| TASK-011 | SettingsManager | ⬜ Not Started |
| TASK-012 | Unit Tests: FolderScanner | ⬜ Not Started |
| TASK-013 | Retrofit API Client | ⬜ Not Started |
| TASK-014 | AlbumManager | ⬜ Not Started |
| TASK-015 | GooglePhotosUploader | ⬜ Not Started |
| TASK-016 | Unit Tests: AlbumManager + Uploader | ⬜ Not Started |
| TASK-017 | SyncWorker | ⬜ Not Started |
| TASK-018 | BootReceiver | ⬜ Not Started |
| TASK-019 | Navigation Structure | ⬜ Not Started |
| TASK-020 | Dashboard Screen | ⬜ Not Started |
