# DroidPhotos — Android App
## Project Overview

DroidPhotos is an Android app that acts as a sync bridge between a user's local Android folder structure and Google Photos albums. It reads local folders on the device, creates matching albums in Google Photos, and uploads photos into those albums — preserving the folder organization that Google Photos' native backup ignores.

---

## Problem Being Solved

Android's native Google Photos backup uploads all photos into a single undifferentiated stream. Even if the user has organized photos into local folders (e.g., `Trips`, `Family`, `Work`), that structure is lost on Google Photos. DroidPhotos fixes this by taking ownership of the backup process and replicating the local folder structure as Google Photos albums.

---

## Key Constraints & Design Decisions

1. **Google Photos Library API limitation**: The API can only manage photos uploaded through this app. Photos already backed up by the native Google Photos app cannot be moved or re-organized. DroidPhotos ignores pre-existing backups and only manages what it uploads itself.

2. **Google Photos flat album structure**: Google Photos does not support nested albums. Nested local folders are flattened using dashes (e.g., `Trips/Europe/Paris` → `Trips - Europe - Paris`). Since the user's current folder depth is one level, this is mainly a future-proofing rule.

3. **Background sync**: The app runs a persistent background service using Android WorkManager to continuously monitor folders and upload new photos without user intervention.

---

## Requirements Summary

| Requirement | Decision |
|---|---|
| Google accounts | Single account |
| Folder depth | One level (e.g., Trips, Family, Work) |
| Nested folder naming | Flatten with dashes |
| Photo sources | DCIM/Camera, custom folders, third-party app folders |
| Sync trigger | Automatic background sync (always on) |
| Sync condition | Wi-Fi and mobile data both acceptable |
| Pre-existing backups | Ignored — only manage app-uploaded photos |
| Local copy after upload | Always kept on device |
| File types | JPG/JPEG, PNG, RAW (DNG/CR2), MP4/MOV, GIF |
| Folder exclusions | Blacklist model — user marks folders to exclude |
| Failed uploads | Log silently, retry on next sync cycle |
| Photo volume | Thousands (1,000–10,000 range) |

---

## File Structure for This Project

```
/app
  /src/main
    /java/com/droidphotos
      MainActivity.kt
      SyncService.kt
      FolderScanner.kt
      GooglePhotosUploader.kt
      AlbumManager.kt
      SyncDatabase.kt
      SyncRepository.kt
      SettingsManager.kt
    /res
      /layout
      /drawable
  build.gradle
/docs
  ARCHITECTURE.md
  API_SETUP.md
  UI_SPEC.md
```

---

## Getting Started

1. Create a new Android project in **Android Studio** (Kotlin, package `com.droidphotos`, min SDK API 29)
2. Copy all 8 `.md` files into the project root folder
3. Add your Google OAuth credentials (see `API_SETUP.md`)
4. Follow `ARCHITECTURE.md` for component wiring
5. Follow `UI_SPEC.md` for screen layouts
6. Work through tasks in order using `TASKS.md`
