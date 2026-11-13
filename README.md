# aharemen

An unofficial Dhiraagu client for Android 8+.

Check balances, build or take back allowances, send and request RB, view activity, and top up.

## Build

Requires JDK 17+ (`JAVA_HOME`) and Android SDK platform/build tools 36 under `tools/sdk-platform/` and `tools/sdk-build-tools/`.

Copy `ClientConfig.java.example` to `app/src/main/java/app/shazin/aharemen/ClientConfig.java` and fill in your API configuration.

```powershell
.\test.ps1
.\build.ps1
```

Output: `dist/aharemen.apk`. Keep `tools/development.keystore` to sign future updates.
