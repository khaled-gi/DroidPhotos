# UI_SPEC.md — DroidPhotos Interface

## Design Philosophy
Clean, utilitarian. This is a background utility app — the UI exists to show status and configure behavior, not to be a primary experience. Minimal chrome, data-forward.

---

## Screen Map

```
[ Sign-In Screen ]
        ↓
[ Dashboard (Home) ]
    ├── [ Folder Manager Screen ]
    ├── [ Sync Log Screen ]
    └── [ Settings Screen ]
```

---

## Screen 1: Sign-In

**Purpose:** First-launch Google authentication.

**Elements:**
- App logo + name "DroidPhotos"
- Tagline: "Your Android folders. Your Google Photos albums."
- **Sign in with Google** button (standard Google branding)
- After sign-in: request `READ_MEDIA_IMAGES`, `READ_MEDIA_VIDEO`, `POST_NOTIFICATIONS` permissions with plain-language explanations

**Behavior:**
- On successful auth, navigate to Dashboard
- On subsequent launches, skip this screen (token persisted)

---

## Screen 2: Dashboard (Home)

**Purpose:** At-a-glance sync health. The main screen users see day-to-day.

**Layout (top to bottom):**

### Header Bar
- App name left, Settings gear icon right

### Sync Status Card
- Large status indicator: `✅ All synced`, `🔄 Syncing...`, or `⚠️ X errors`
- Last sync timestamp: "Last synced: Today at 2:43 PM"
- Total synced count: "4,231 files synced across 8 albums"
- **Sync Now** button (triggers an immediate manual sync cycle)

### Folder Status List
Scrollable list. One row per monitored folder:
```
[ Folder Icon ]  Trips                    ✅ 312 files
                 Last synced: 2 hrs ago   →

[ Folder Icon ]  Family                   🔄 Uploading...
                 47 of 203 files          →

[ Folder Icon ]  Work                     ⚠️ 2 failed
                 Last synced: Yesterday   →

[ Folder Icon ]  WhatsApp Images          ⛔ Excluded
                                          →
```
- Tapping a row goes to that folder's detail (within Folder Manager)
- Excluded (blacklisted) folders shown in muted style with ⛔

### Bottom Navigation Bar
- 🏠 Dashboard (active)
- 📁 Folders
- 📋 Log
- ⚙️ Settings

---

## Screen 3: Folder Manager

**Purpose:** Control which folders are synced and which are blacklisted.

**Layout:**

### Detected Folders List
Auto-discovered folders from MediaStore scan. Each row:
```
[ Folder Icon ]  DCIM/Camera              [Toggle: ON ]
                 1,204 files

[ Folder Icon ]  Trips                    [Toggle: ON ]
                 312 files

[ Folder Icon ]  Screenshots              [Toggle: OFF - Excluded]
                 88 files
```
- Toggle ON = folder is synced → album created/maintained in Google Photos
- Toggle OFF = folder is blacklisted → excluded from all sync activity
- Changes take effect on the next sync cycle

### Corresponding Google Photos Album
Below each active folder name, show the mapped album name:
`→ Album: "Trips"`

### Refresh Button
- **Rescan Folders** — re-runs MediaStore scan to detect newly created folders

---

## Screen 4: Sync Log

**Purpose:** Transparent history of what the app has done.

**Layout:**

### Filter Bar
- Filter by: All | Synced | Failed | Skipped

### Log Entries (reverse chronological)
```
2:43 PM  ✅  IMG_4821.jpg  →  Trips
2:43 PM  ✅  IMG_4820.jpg  →  Trips
2:41 PM  ⚠️  VID_0032.mp4  →  Family   [Upload failed: timeout]
2:38 PM  ✅  IMG_4819.jpg  →  Family
───────────────────────────────
Yesterday
4:12 PM  ✅  IMG_4801.jpg  →  Work
...
```

### Failed Files Actions
- Tapping a failed entry shows: filename, folder, error message, timestamp
- Option: **Retry This File** button

### Export Log
- **Export as CSV** button at bottom — saves log to Downloads folder

---

## Screen 5: Settings

**Purpose:** Configure app behavior.

**Sections:**

### Account
- Signed-in Google account (name + email + avatar)
- **Sign Out** button

### Sync Behavior
- Sync over mobile data: [Toggle ON/OFF] (default: ON)
- Sync frequency: informational label "Background sync runs approximately every 15 minutes (Android system managed)"

### Notifications
- Notify when sync completes: [Toggle ON/OFF]
- Notify on upload errors: [Toggle ON/OFF] (always logs regardless)

### Storage
- Synced files database size: "Local DB: 2.4 MB"
- **Clear Sync History** — resets the synced_files table (does NOT delete from Google Photos; will re-upload everything)

### About
- App version
- Link to Google Photos API terms
- **Send Feedback** (opens email)

---

## Notification Specs

### Sync Complete Notification
- Title: "DroidPhotos sync complete"
- Body: "47 new photos uploaded to 3 albums"
- Tap action: opens Dashboard

### Error Notification
- Title: "DroidPhotos: Upload errors"
- Body: "3 files failed to upload. Tap to view."
- Tap action: opens Sync Log filtered to Failed

### Ongoing Sync Notification (Foreground Service)
- Title: "DroidPhotos"
- Body: "Uploading photos... (23 of 47)"
- Non-dismissable while sync is in progress (required by Android for foreground services)

---

## Color & Style Guide

| Element | Value |
|---|---|
| Primary color | `#1A73E8` (Google Blue) |
| Success | `#34A853` (Google Green) |
| Error | `#EA4335` (Google Red) |
| Warning | `#FBBC04` (Google Yellow) |
| Background | `#FFFFFF` / `#F8F9FA` |
| Text primary | `#202124` |
| Text secondary | `#5F6368` |
| Font | Roboto (Android system default) |

Use Material Design 3 components throughout (Jetpack Compose Material3 library).
