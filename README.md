# Little Things Android

Android client for **Little Things** — a daily reflection and journaling app. This project mirrors the iOS `LittleThingsApp` architecture and delivers a v1 focused on the **Google Sign-In → token persistence → Home** auth flow. SignIn is fully implemented; Splash, Home tabs, and other screens are placeholders for future iterations.

**Design spec:** [docs/superpowers/specs/2026-07-05-littlethings-android-architecture-design.md](docs/superpowers/specs/2026-07-05-littlethings-android-architecture-design.md)  
**Startup flow:** [docs/app-startup-flow.md](docs/app-startup-flow.md)

---

## Module Structure

Five Gradle modules:

| Module | Purpose |
|--------|---------|
| `:app` | Application entry, Activities, Domain (SignIn, Home), Service layer (Auth, interceptors, DTOs) |
| `:core:common` | Shared utilities — `AppEnvironment`, feature toggles, `InjectionValues`, logging |
| `:core:network` | HTTP client — `ApiClient`, `ApiRequest`, retry/error handling |
| `:core:persistence` | Secure token storage — `SessionService`, `SecureTokenStorage` |
| `:core:uicomponent` | Shared UI resources — colors, themes, fonts |

```
LittleThingsAndroidAI/
├── app/                    # Application + Domain + Service
├── core/
│   ├── common/
│   ├── network/
│   ├── persistence/
│   └── uicomponent/
└── docs/superpowers/       # Architecture spec & implementation plan
```

---

## AppGraph (Manual DI)

Dependency injection is handled manually via `AppGraph`, aligned with iOS `AppCoordinator.init`. No Hilt/Koin — dependencies are wired in one place at startup.

`LTApplication.onCreate()` calls `AppGraph.build(context, environment)`, which constructs:

- `SessionService` — encrypted token storage
- `ApiClient` — bare (unauthenticated) and authenticated instances
- Interceptors — `AuthInterceptor`, `RefreshTokenInterceptor`, `LogoutInterceptor`
- Repositories & services — `AuthRepository`, `AppDataWithAuthorizationService`, etc.

Access the graph anywhere after startup:

```kotlin
AppGraph.current.sessionService
AppGraph.current.authRepository
```

---

## Build & Test

From the project root (`LittleThingsAndroidAI/`):

```bash
# Build debug APK
./gradlew assembleDebug

# Run unit tests (core + app)
./gradlew :core:network:testDebugUnitTest \
          :core:persistence:testDebugUnitTest \
          :app:testDebugUnitTest

# Run all unit tests
./gradlew testDebugUnitTest
```

Debug APK output: `app/build/outputs/apk/debug/app-debug.apk`

---

## Google Sign-In Setup

Google Sign-In requires a Web Client ID and a registered SHA-1 fingerprint in Google Cloud Console.

### 1. Replace Web Client ID

Edit `app/src/main/res/values/strings.xml` and replace the placeholder:

```xml
<string name="default_web_client_id">YOUR_WEB_CLIENT_ID</string>
```

Use the **Web client** OAuth 2.0 Client ID from [Google Cloud Console](https://console.cloud.google.com/) → APIs & Services → Credentials (same project as your Android OAuth client).

### 2. Register SHA-1 Fingerprint

1. Open Google Cloud Console → your project → **APIs & Services** → **Credentials**.
2. Select (or create) the **Android** OAuth 2.0 client for package name `com.littlethingsandroidai`.
3. Add your debug/release **SHA-1** certificate fingerprint.

Debug SHA-1 (local keystore):

```bash
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
```

Copy the SHA-1 value into the Android OAuth client. Without a matching SHA-1, Google Sign-In will fail at runtime.

---

## Tech Stack

- Kotlin 2.2, AGP 9.1
- XML + ViewBinding (Compose BOM retained for future use)
- Navigation Component, OkHttp 4, EncryptedSharedPreferences
- Google Sign-In, JUnit 5, MockWebServer

## iOS Reference

API contracts and Service layer patterns follow iOS `LittleThingsApp/app/LTApp/LTApp/Source/Service/`. UI follows iOS `Domain/` View/ViewModel equivalents where implemented.
