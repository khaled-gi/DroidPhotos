# API_SETUP.md — Google Photos API Configuration

## Overview

DroidPhotos uses the **Google Photos Library API** via OAuth 2.0. You must set up a Google Cloud project and configure credentials before the app can upload to Google Photos.

---

## Step 1: Create a Google Cloud Project

1. Go to [https://console.cloud.google.com](https://console.cloud.google.com)
2. Click **New Project**
3. Name it `DroidPhotos` and click **Create**

---

## Step 2: Enable the Google Photos Library API

1. In your project, go to **APIs & Services → Library**
2. Search for **Photos Library API**
3. Click **Enable**

> ⚠️ Note: As of 2024, the Google Photos Library API requires you to submit your app for verification if you plan to distribute it. For personal/development use, you can run in "Testing" mode with up to 100 test users.

---

## Step 3: Configure OAuth Consent Screen

1. Go to **APIs & Services → OAuth Consent Screen**
2. Select **External** (unless you have a Google Workspace org)
3. Fill in:
   - App name: `DroidPhotos`
   - User support email: your email
   - Developer contact email: your email
4. Click **Save and Continue**
5. On the **Scopes** screen, click **Add or Remove Scopes**
6. Add: `https://www.googleapis.com/auth/photoslibrary`
7. Save and continue through remaining steps
8. Add your own Google account as a **Test User**

---

## Step 4: Create OAuth 2.0 Credentials

1. Go to **APIs & Services → Credentials**
2. Click **Create Credentials → OAuth Client ID**
3. Application type: **Android**
4. Package name: `com.droidphotos` (must match your app's `applicationId` in `build.gradle`)
5. SHA-1 certificate fingerprint: run this in your terminal:
   ```bash
   keytool -keystore ~/.android/debug.keystore -list -v
   ```
   Copy the SHA-1 value and paste it here
6. Click **Create**

> You do NOT get a `client_secret` for Android OAuth — Android uses the package name + SHA-1 for verification instead. This is correct and expected.

---

## Step 5: Add Google Services to the Project

1. Download the `google-services.json` file from your Cloud Console:
   - Go to **Project Settings → Your Apps → Android App**
   - Download `google-services.json`
2. Place it in your Android project at: `/app/google-services.json`

---

## Step 6: Add Dependencies to build.gradle

**Project-level `build.gradle`:**
```groovy
buildscript {
    dependencies {
        classpath 'com.google.gms:google-services:4.4.0'
    }
}
```

**App-level `build.gradle`:**
```groovy
plugins {
    id 'com.google.gms.google-services'
}

dependencies {
    // Google Sign-In
    implementation 'com.google.android.gms:play-services-auth:21.0.0'

    // Google Photos Library API client (REST via Retrofit — no official Android SDK)
    implementation 'com.squareup.retrofit2:retrofit:2.9.0'
    implementation 'com.squareup.retrofit2:converter-gson:2.9.0'
    implementation 'com.squareup.okhttp3:okhttp:4.12.0'
    implementation 'com.squareup.okhttp3:logging-interceptor:4.12.0'

    // Google Auth
    implementation 'com.google.auth:google-auth-library-oauth2-http:1.19.0'
}
```

---

## Step 7: OAuth Scope Reference

| Scope | Purpose |
|---|---|
| `photoslibrary` | Full read/write access to the user's Google Photos library |
| `photoslibrary.appendonly` | Upload-only (cannot read existing library) |

Use `photoslibrary` for DroidPhotos so the app can both create albums and upload media.

---

## API Rate Limits (as of 2024)

| Limit | Value |
|---|---|
| Requests per day | 10,000 |
| Requests per 100 seconds | 1,000 |
| Max items per batchCreate | 50 |
| Max upload size per file | 200 MB (photos), 10 GB (videos) |

DroidPhotos handles rate limiting with exponential backoff on HTTP 429 responses.

---

## Testing Checklist

- [ ] Google Cloud project created
- [ ] Photos Library API enabled
- [ ] OAuth consent screen configured
- [ ] Your Google account added as test user
- [ ] Android OAuth credential created with correct SHA-1
- [ ] `google-services.json` placed in `/app/`
- [ ] Dependencies added to both `build.gradle` files
- [ ] App successfully completes Google Sign-In flow
- [ ] App can create a test album via API
- [ ] App can upload a single test photo
