# Cloud build — My Tuition Manager

This package is prepared for a GitHub Actions cloud build.

## Easiest route

1. Create/sign in to a GitHub account.
2. Create a new repository, e.g. `my-tuition-manager`.
3. Upload the contents of this folder to the repository.
4. Open the repository's **Actions** tab.
5. Select **Build My Tuition Manager APK**.
6. Click **Run workflow**.
7. Wait for the build to finish.
8. Open the completed workflow run.
9. Download the artifact named **MyTuitionManager-debug-apk**.
10. Extract the downloaded artifact and install the APK on your Android phone.

GitHub's hosted Linux runners can build Android projects, and GitHub Actions can upload the generated APK as an artifact. This workflow creates a debug APK intended for testing and personal use.

## Important

Do not put passwords, signing keys, WhatsApp credentials, or private student data into the repository.

This first cloud build is for testing the app. Later, we can create a properly signed release APK if you want to distribute the app beyond your own phone.
