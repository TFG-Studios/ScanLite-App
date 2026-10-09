# ScanLite
(Keep in mind this is a VIBE-CODED OPEN-SOURCE PROJECT)
**A fast, lightweight, privacy-focused document scanner for Android.**

ScanLite turns your phone into a simple, powerful document scanner. Scan paper documents, automatically detect their boundaries, enhance their appearance, and export them as PDFs or images — all directly on your device.

Designed with performance, privacy, and simplicity in mind, ScanLite aims to deliver the essential features of a document scanner without unnecessary features, complicated workflows, or heavy resource usage.

## ✨ Features

* **Automatic Document Detection** — Detect document edges to help capture clean, properly framed scans.
* **Image Enhancement** — Apply filters to improve readability and create cleaner-looking documents.
* **Brightness & Contrast Controls** — Fine-tune scanned pages to achieve the appearance you want.
* **Multi-Page Scanning** — Combine multiple scanned pages into a single document.
* **PDF Export** — Export your scans as convenient PDF documents.
* **Image Export** — Save individual scanned pages as image files.
* **Lightweight & Fast** — Designed for responsive performance, including on low-end Android devices.
* **Clean, Friendly Interface** — Enjoy a modern, professional interface that keeps scanning simple and intuitive.
* **Offline-First Privacy** — Scan, process, and export documents locally without relying on cloud services or an internet connection.

## 🔒 Privacy First

Your documents should remain yours.

ScanLite is designed to process scans locally on your Android device.

* No cloud account is required.
* No internet connection is required for core scanning functionality.
* Documents are not uploaded to an external server by the app.
* Your files remain under your control on your device.

**Privacy by design:** document scanning and enhancement should happen locally, not on a remote server.

## 📦 Lightweight by Design

ScanLite aims for an APK size of approximately **1 MB**, keeping installation and storage requirements low.

The actual APK size may vary depending on the build configuration, Android architecture, and included libraries. The published release size should be verified against the final build.

## 🚀 Getting Started

### Install the App

1. Open the project's [Releases](../../releases) page.
2. Download the latest available Android APK.
3. Install the APK on your Android device.
4. Open ScanLite and start scanning.

If no release is available yet, you can build the app from source.

### Build from Source

#### Requirements

* Android Studio (with all JDK and SDK stuff), or just use Github Actions to build after cloning the repository.

#### Instructions

1. Clone the repository:

   ```bash
   git clone https://github.com/YOUR-USERNAME/ScanLite-app.git
   ```

2. Open the downloaded `ScanLite-app` directory in Android Studio.

3. Allow Gradle to synchronize the project and download any required build dependencies.

4. Build the debug APK from the project directory:

   ```bash
   ./gradlew assembleDebug
   ```

   On Windows, use:

   ```bat
   gradlew.bat assembleDebug
   ```

5. After a successful build, look for the APK at:

   ```text
   app/build/outputs/apk/debug/app-debug.apk
   ```

Replace `YOUR-USERNAME` with the actual GitHub username or organization that owns the repository.

## 📱 How to Use ScanLite

1. **Scan a page** using your phone's camera.
2. **Review the detected document** and adjust its boundaries when necessary.
3. **Enhance the scan** using filters, brightness, and contrast controls.
4. **Add more pages** to create a multi-page document.
5. **Export your work** as a PDF or as image files.
6. **Save and access your results** locally on your device.

The aim is to keep the entire workflow fast, straightforward, and accessible.

## 🛠️ Built With

ScanLite is an open-source Android project.

* **Platform:** Android
* **Language:** English
* **Build System:** Gradle

Implementation details and supported Android versions may vary by release.

## 🤝 Contributing

Contributions are welcome!

Whether you want to fix a bug, improve performance, refine the interface, or suggest a useful feature, your contributions can help make ScanLite better.

### How to Contribute

1. Fork this repository.
2. Create a branch for your changes.
3. Make your changes and test them.
4. Commit your work with a descriptive message.
5. Open a pull request explaining what you changed and why.

Please keep contributions aligned with the project's main goals:

* Offline functionality.
* Minimal resource usage.
* Privacy-focused document processing.
* A clean and intuitive user experience.
* Compatibility with supported Android devices.

## 🗺️ Roadmap

Potential areas for continued improvement include:

* Improving automatic document-edge detection.
* Refining scan quality and image-processing performance.
* Enhancing accessibility and interface consistency.

Suggestions and constructive feedback are welcome.

## 🐛 Reporting Bugs

Found a bug or experienced an unexpected result?

Please open an issue in the GitHub repository and include:

* A clear description of the problem.
* Steps to reproduce it.
* Your Android version and device model.
* Relevant screenshots or logs, if available.

Avoid including private documents or other sensitive information in bug reports.

## 📄 License

ScanLite is intended to be an open-source project.

**Before publishing, add a `LICENSE` file** containing your chosen open-source license, then update this section with the license name and a link to that file.

Until a license is selected and added, the repository's reuse and redistribution permissions are not explicitly established.

## ❤️ Support the Project

If ScanLite is useful to you, consider starring the repository, reporting bugs, suggesting improvements, or contributing code.

Every contribution helps make lightweight, privacy-focused document scanning more accessible.

---

**ScanLite — Scan simply. Keep it private.**

