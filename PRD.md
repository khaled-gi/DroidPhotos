# PRD.md — Product Requirements Document
# DroidPhotos for Android

## Product Vision
DroidPhotos is an Android utility app that preserves a user's local folder structure when backing up photos to Google Photos. Where Android's native Google Photos backup flattens all photos into a single stream, DroidPhotos uploads photos folder-by-folder and creates matching Google Photos albums — giving the user the same organized cloud library that iPhone users get with iCloud.

---

## Target User
A single Android user who:
- Organizes photos into local folders on their device
- Takes thousands of photos (1,000–10,000 range)
- Is frustrated that Google Photos doesn't reflect their folder structure
- Is not a developer — needs the app to be self-managing and reliable
- Eventually wants to distribute this to others via the Play Store

---

## MVP Scope (v1)

The v1 is considered complete when the following three things work end-to-end:

1. **Google Sign-In** — User can authenticate with their Google account and grant the app permission to access Google Photos
2. **Folder Scan** — App reads all photo folders on the device (DCIM, custom folders, third-party app folders), excluding blacklisted folders
3. **Album Upload** — App creates a matching Google Photos album for each folder and uploads all photos/videos into the correct album

Everything else — background sync, full dashboard UI, log screen, settings — is post-MVP.

---

## Full Feature Requirements

### F1 — Authentication
- **F1.1** Sign in with a single Google account via Google Sign-In SDK
- **F1.2** Request OAuth scope: `photoslibrary`
- **F1.3** Persist authentication across app restarts
- **F1.4** Provide Sign Out option in Settings
- **F1.5** App must be built for Play Store distribution (OAuth consent screen, verified app)

### F2 — Folder Scanning
- **F2.1** Scan all media folders via Android MediaStore API
- **F2.2** Support file types: JPG, JPEG, PNG, DNG, CR2, MP4, MOV, GIF
- **F2.3** Support scan locations: DCIM/Camera, user-created folders, third-party app folders
- **F2.4** Respect blacklisted folders — never scan or upload from excluded folders
- **F2.5** Detect only new/unsynced files on subsequent scans (do not re-upload)
- **F2.6** Support Android 10 (API 29) as minimum, handle Android 13 (API 33) permissions model

### F3 — Album Management
- **F3.1** Create a Google Photos album for each active local folder
- **F3.2** Album names match folder names exactly for one-level folders
- **F3.3** Nested folders (future-proofing) flattened with dashes: `Trips - Europe - Paris`
- **F3.4** Cache album IDs locally — never create duplicate albums
- **F3.5** Never modify or delete albums the app did not create
- **F3.6** Never touch photos already backed up by the native Google Photos app

### F4 — Photo Upload
- **F4.1** Upload photos via Google Photos Library API two-step process (upload bytes → create media item)
- **F4.2** Assign each uploaded photo to its correct album
- **F4.3** Batch uploads in groups of up to 50 (API limit)
- **F4.4** Never delete local photos after upload — always keep on device
- **F4.5** On upload failure: log silently, mark for retry on next sync cycle
- **F4.6** Implement exponential backoff on HTTP 429 (rate limit) responses

### F5 — Background Sync
- **F5.1** Run automatic background sync via WorkManager (PeriodicWorkRequest)
- **F5.2** Sync runs approximately every 15 minutes (Android OS minimum)
- **F5.3** Sync runs on both Wi-Fi and mobile data by default
- **F5.4** Mobile data sync can be disabled by user in Settings (requires explicit permission)
- **F5.5** Sync survives device restarts (WorkManager handles re-registration)

### F6 — UI & Dashboard
- **F6.1** Dashboard showing overall sync status and per-folder sync status
- **F6.2** Folder Manager: list of all detected folders with ON/OFF toggle (whitelist/blacklist)
- **F6.3** Sync Log: reverse-chronological history of all upload activity, filterable by status
- **F6.4** Failed upload entries show filename, folder, error reason, and timestamp
- **F6.5** Settings screen: account info, mobile data toggle, notification toggles, clear history
- **F6.6** Notifications: sync complete, upload errors, ongoing foreground service during active sync

### F7 — Reliability & Safety
- **F7.1** Never delete local photos under any circumstance
- **F7.2** Never upload to any Google account other than the signed-in user
- **F7.3** Never modify or delete Google Photos albums the app did not create
- **F7.4** Never sync over mobile data without explicit user opt-in
- **F7.5** All sync state persisted in local Room database — resumable after crash or restart
- **F7.6** Unit tests cover: FolderScanner, AlbumManager, SyncDatabase, upload retry logic

---

## Out of Scope for v1
- Two-way sync
- Multi-account support
- Scheduled sync windows
- Home screen widget
- Detailed upload failure log with error reasons (basic log only in v1)
- Reorganizing photos already backed up by native Google Photos

---

## Future Versions (Architecture Must Support)

| Feature | Notes |
|---|---|
| Two-way sync | Album rename in Google Photos propagates back to local folder name |
| Multi-account | Multiple Google accounts, each with independent sync state |
| Scheduled sync windows | User defines time windows (e.g., 2–4 AM only) |
| Home screen widget | Shows last sync time and status without opening app |
| Upload failure log | Full error detail viewer with per-file retry capability |

---

## Acceptance Criteria for v1 Sign-Off

- [ ] User can sign in with Google account on a fresh install
- [ ] App detects all photo folders on the device
- [ ] Blacklisted folders are never scanned or uploaded
- [ ] A Google Photos album is created for each non-blacklisted folder
- [ ] Photos are uploaded into the correct albums
- [ ] Re-running sync does not create duplicate albums or re-upload existing photos
- [ ] Local photos are never deleted
- [ ] Unit tests pass for core logic components
- [ ] App builds successfully targeting API 29 minimum, API 34 target
