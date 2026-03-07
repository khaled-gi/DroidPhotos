# DECISIONS.md — Architectural Decision Log
# DroidPhotos for Android

## Purpose
This file records the key decisions made during the design of DroidPhotos, along with the reasoning behind each one. It exists to prevent the agent or future developers from second-guessing or accidentally undoing deliberate choices. Before changing anything documented here, the user must be consulted.

---

## DEC-001 — Use WorkManager for Background Sync (not a plain Service or AlarmManager)

**Decision:** Background sync is implemented using Android WorkManager with a PeriodicWorkRequest.

**Why:**
- WorkManager is Android's officially recommended solution for deferrable, guaranteed background work
- Unlike a plain `Service`, WorkManager survives app restarts and device reboots automatically
- Unlike `AlarmManager`, WorkManager respects Android's battery optimization (Doze mode) without needing special handling
- WorkManager automatically retries failed work with configurable backoff policies
- It is the only approach that reliably works across all Android versions from API 29 onward

**Alternatives rejected:**
- `JobScheduler`: Lower-level API that WorkManager wraps — no reason to use directly
- `AlarmManager`: Designed for time-precise alarms, not background data sync
- Foreground Service (persistent): Too battery-intensive for a sync task that runs every 15 minutes; reserved for the active upload notification only

---

## DEC-002 — Ignore Photos Already Backed Up by Native Google Photos

**Decision:** DroidPhotos only manages photos it uploaded itself. It does not attempt to reorganize existing Google Photos backups.

**Why:**
- The Google Photos Library API (since 2019) explicitly prohibits reading or modifying media items that were not uploaded through the same app
- Attempting to access pre-existing items returns a 403 error
- Re-uploading pre-existing photos would create duplicates in the user's library
- The user explicitly chose this behavior during requirements gathering

**Implication:**
- The app tracks its own uploads in the local Room database (`synced_files` table)
- Only files with no existing `mediaItemId` in the DB are candidates for upload
- This is a permanent constraint of the Google Photos API, not a technical limitation of the app

---

## DEC-003 — Use Room (SQLite) for Local State, Not a Plain File or SharedPreferences

**Decision:** All sync state (uploaded files, album IDs, sync log) is stored in a Room database.

**Why:**
- The app needs to track thousands of files across multiple sync cycles — flat files or SharedPreferences cannot handle this at scale
- Room provides type-safe queries, migration support, and Flow-based reactive updates to the UI
- The `albums` table acts as a critical cache — without it, the app would make redundant API calls to check if albums already exist
- The `sync_log` table enables the full history view the user requested

**Alternatives rejected:**
- SharedPreferences: Only suitable for simple key-value settings, not relational data
- Raw SQLite: More error-prone than Room with no compile-time query validation
- Firebase/cloud DB: Unnecessary complexity; all state is per-device

---

## DEC-004 — Single Google Account Only (for v1)

**Decision:** v1 supports only one signed-in Google account at a time.

**Why:**
- The user's current need is single-account
- Multi-account support requires scoping all database records by account ID, a non-trivial refactor
- Implementing it correctly in v1 would slow down the build without current benefit

**Future-proofing:**
- The database schema uses `folder_name` as a key in the `albums` table — this will need an `account_id` column added when multi-account is built
- The `SyncRepository` is designed as a single access layer, making it the only place that needs updating when account scoping is added
- TASK-027 in `TASKS.md` covers the multi-account implementation

---

## DEC-005 — Flatten Nested Folders with Dashes

**Decision:** Nested local folder paths are converted to Google Photos album names by joining path components with " - " (space-dash-space).

**Example:** `Trips/Europe/Paris` → `Trips - Europe - Paris`

**Why:**
- Google Photos does not support nested albums — the structure must be flattened
- Dashes are human-readable and unambiguous in album names
- The user explicitly chose this format during requirements gathering
- The user's current folder depth is one level, so this rule is primarily future-proofing

**Constraint:** Album names in Google Photos have a maximum length of 500 characters — this is unlikely to be an issue with dash-flattened names but should be validated before API calls.

---

## DEC-006 — Use Jetpack Compose for UI (not XML layouts)

**Decision:** All UI is built with Jetpack Compose, Android's modern declarative UI toolkit.

**Why:**
- Compose is Google's recommended UI approach for new Android apps as of 2023
- Declarative UI is easier to reason about than XML layouts, especially for a beginner
- Compose integrates naturally with Kotlin Coroutines and Flow for reactive data updates
- Material Design 3 components are first-class citizens in Compose
- Better long-term support trajectory than the legacy View system

**Alternatives rejected:**
- XML layouts with ViewBinding: Still valid but legacy; harder to maintain and less Kotlin-idiomatic
- Flutter: Cross-platform but adds significant complexity; user is building a native Android app

---

## DEC-007 — Use Hilt for Dependency Injection

**Decision:** Hilt is used for dependency injection throughout the app.

**Why:**
- Hilt is Google's official DI solution for Android, built on top of Dagger
- It eliminates manual wiring of dependencies (e.g., passing database instances through constructors)
- WorkManager integration with Hilt is well-supported via `@HiltWorker`
- Makes the codebase significantly more testable — dependencies can be swapped for fakes in tests
- Required for clean architecture at the scale of this app

**Alternatives rejected:**
- Manual DI (passing objects through constructors): Unmanageable at scale; no, for this app
- Koin: Lighter-weight alternative but less Android-native; Hilt has better official documentation

---

## DEC-008 — Mobile Data Sync Defaults to ON (user can disable)

**Decision:** Background sync runs on both Wi-Fi and mobile data by default. The user can disable mobile data sync in Settings.

**Why:**
- The user did not select "Wi-Fi only" as a requirement — they selected "always on"
- Defaulting to ON means the app works immediately without configuration
- The hard rule "never sync on mobile data without explicit user permission" is satisfied because the user granted permission during onboarding by accepting the default

**Note:** If this creates unexpected data usage complaints, the default can be changed to Wi-Fi-only in a future update. The toggle infrastructure will already be in place.

---

## DEC-009 — Target Play Store Distribution from Day One

**Decision:** The app is built with Play Store distribution in mind, even during development.

**Implications:**
- Package name `com.droidphotos` must be finalized before publishing (cannot change after)
- OAuth consent screen must be submitted for Google verification before public release
- A privacy policy is required (Google Sign-In + Photos access mandates this)
- Release signing keystore must be created and stored securely — not committed to source control
- `minSdk 29` covers approximately 95% of active Android devices — acceptable for Play Store

---

## DEC-010 — Never Delete Local Photos (absolute rule)

**Decision:** The app will never delete, move, or modify any file in local device storage.

**Why:**
- Photo loss is catastrophic and irreversible
- The user's explicit requirement is to keep all local photos
- The app's role is backup and organization — not storage management
- This rule is enforced at the code level: `FolderScanner` is read-only, `GooglePhotosUploader` only calls outbound API endpoints

**This decision cannot be overridden by any future feature request without a separate explicit user decision recorded here.**
