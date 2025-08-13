# Build APK with GitHub Actions (No local Android setup)

1) Create a **new repository** on GitHub (default branch `main`).
2) Upload the contents of this folder (drag & drop), then **Commit**.
3) Go to **Actions** → the workflow **Build Android APK (Debug)** runs automatically (or click **Run workflow**).
4) Open the run → download artifact **cookassistant-debug.apk**.
5) Install the APK on your device.

If your default branch is not `main`, either rename it to `main` or edit `.github/workflows/android.yml` to your branch.