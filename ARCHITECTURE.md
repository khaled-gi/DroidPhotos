# ARCHITECTURE.md — DroidPhotos

## System Overview

DroidPhotos has four major layers:

```
[ Android File System ]
        ↓
[ FolderScanner ] — reads local folders, filters blacklist, detects new files
        ↓
[ SyncEngine / WorkManager ] — background orchestration, retry logic
        ↓
[ GooglePhotosUploader + AlbumManager ] — Google Photos Library API calls
        ↓
[ SyncDatabase (Room) ] — local state: what's been uploaded, album IDs, logs
```

---

## Component Breakdown

### 1. FolderScanner
- Uses Android `MediaStore` API to enumerate photos/videos across all storage locations
- Scans: `DCIM/Camera`, user-created folders, third-party app folders (WhatsApp, Screenshots, etc.)
- Filters out blacklisted folders (stored in SettingsManager)
- Supported file types: `.jpg`, `.jpeg`, `.png`, `.dng`, `.cr2`, `.mp4`, `.mov`, `.gif`
- Returns a list of `MediaFile` objects: `{ filePath, folderName, fileSize, dateModified, mimeType }`
- Compares results against SyncDatabase to identify only NEW or UNSYNCED files

### 2. SyncEngine (WorkManager)
- Implemented as an Android `CoroutineWorker` via WorkManager
- Runs as a periodic background task (15-minute minimum interval, Android OS enforced)
- On each cycle:
  1. Calls FolderScanner to get unsynced files
  2. Groups files by folder name
  3. For each folder group: ensures album exists (AlbumManager), then uploads files (GooglePhotosUploader)
  4. Updates SyncDatabase with results
  5. On completion, fires a system notification
- Retry behavior: failed files are marked `PENDING_RETRY` in DB; retried on next cycle
- Constraint: runs on any network (Wi-Fi or mobile). Can be toggled in settings.

### 3. AlbumManager
- Maintains a local map of `folderName → googleAlbumId` in SyncDatabase
- On first encounter of a folder: calls Google Photos API to create a new album
- Album naming: uses folder name directly for one-level folders. For nested paths, flattens with dashes (e.g., `Trips - Europe - Paris`)
- Caches album IDs locally to avoid redundant API calls
- API endpoint used: `POST https://photoslibrary.googleapis.com/v1/albums`

### 4. GooglePhotosUploader
- Handles the two-step Google Photos upload process:
  - **Step 1**: Upload raw bytes → receive an `uploadToken`
    - `POST https://photoslibrary.googleapis.com/v1/uploads`
  - **Step 2**: Create media item using the token and assign to album
    - `POST https://photoslibrary.googleapis.com/v1/mediaItems:batchCreate`
- Batches uploads in groups of up to 50 (API limit per batch call)
- Respects API rate limits with exponential backoff on 429 responses
- On success: marks file as `SYNCED` in SyncDatabase, stores `mediaItemId`
- On failure: marks file as `FAILED`, logs error message with timestamp

### 5. SyncDatabase (Room)
Three tables:

**`synced_files`**
| Column | Type | Notes |
|---|---|---|
| id | INTEGER PK | |
| file_path | TEXT | Local absolute path |
| folder_name | TEXT | Parent folder |
| media_item_id | TEXT | Google Photos media item ID |
| status | TEXT | PENDING / SYNCED / FAILED / PENDING_RETRY |
| error_message | TEXT | Null if successful |
| synced_at | INTEGER | Unix timestamp |

**`albums`**
| Column | Type | Notes |
|---|---|---|
| id | INTEGER PK | |
| folder_name | TEXT | Local folder name |
| google_album_id | TEXT | Google Photos album ID |
| created_at | INTEGER | Unix timestamp |

**`sync_log`**
| Column | Type | Notes |
|---|---|---|
| id | INTEGER PK | |
| event | TEXT | e.g., "SYNC_STARTED", "UPLOAD_FAILED" |
| detail | TEXT | File path or error description |
| timestamp | INTEGER | Unix timestamp |

### 6. SettingsManager
- Stores user preferences in Android `SharedPreferences`
- Manages:
  - Blacklisted folder list (Set\<String\>)
  - Per-folder sync toggle (enabled/disabled)
  - Sync-over-mobile-data toggle
  - Google OAuth token (delegated to Google Sign-In SDK)

---

## Authentication Flow

1. User signs in via **Google Sign-In SDK** on first launch
2. App requests OAuth 2.0 scope: `https://www.googleapis.com/auth/photoslibrary`
3. Access token is managed by the Google Auth library (auto-refresh)
4. Token is passed as `Authorization: Bearer {token}` header on all API calls

---

## Background Sync Lifecycle

```
App installed
    → User signs in & grants permissions
    → WorkManager registers PeriodicWorkRequest (every 15 min)
    → SyncWorker runs:
        → FolderScanner finds new files
        → AlbumManager ensures albums exist
        → GooglePhotosUploader uploads in batches
        → SyncDatabase updated
        → Notification fired if files were synced
    → Repeat
```

---

## Android Permissions Required

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
<uses-permission android:name="android.permission.READ_MEDIA_VIDEO" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
```

> Note: `READ_MEDIA_IMAGES` and `READ_MEDIA_VIDEO` replace `READ_EXTERNAL_STORAGE` on Android 13+. Handle both for backward compatibility.

---

## Technology Stack

| Layer | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose |
| Background work | WorkManager (CoroutineWorker) |
| Local DB | Room (SQLite) |
| Networking | Retrofit + OkHttp |
| Auth | Google Sign-In SDK + Google Auth Library |
| API | Google Photos Library API v1 |
| DI | Hilt |
| Async | Kotlin Coroutines + Flow |
