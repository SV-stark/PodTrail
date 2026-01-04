# PodTrail

<div align="center">
  <img width="300" height="300" alt="PodTrail Logo" src="https://github.com/user-attachments/assets/9ff08f4a-5a53-46cb-b205-8d31fdbee25e" />
  <br>
  <br>

  ![Android Build](https://github.com/SV-stark/PodTrail/actions/workflows/build.yml/badge.svg)
  ![License](https://img.shields.io/badge/License-GPL%20v3-blue.svg)
  ![Kotlin](https://img.shields.io/badge/Kotlin-1.9.22-purple.svg)

  <p>
    <b>A minimal, privacy-focused Android podcast tracker.</b>
  </p>
</div>

---

## 📖 Overview

**PodTrail** is a lightweight Android application designed for podcast enthusiasts who value simplicity and privacy. Built with modern Android technologies, it offers a seamless experience for subscribing to, managing, and listening to your favorite podcasts.

## ✨ Features

- **RSS Feed Management**: Easily add podcast RSS URLs to subscribe.
- **Smart Parsing**: Automatically parses feeds to extract metadata, including iTunes tags, episode numbers, and durations.
- **Local Storage**: Persists podcasts and episode data locally using **Room** database.
- **Robust Playback**: High-quality audio playback powered by **ExoPlayer**.
- **Progress Tracking**: Remembers your playback position and automatically marks episodes as "listened" after 90% completion.

## 🛠️ Tech Stack

- **Language**: [Kotlin](https://kotlinlang.org/)
- **UI Framework**: [Jetpack Compose](https://developer.android.com/jetbrains/compose)
- **Database**: [Room](https://developer.android.com/training/data-storage/room)
- **Audio**: [ExoPlayer](https://developer.android.com/media/media3/exoplayer)

## 🚀 Getting Started

Follow these steps to build and run the application locally.

### Prerequisites

- **Android Studio** (Hedgehog or newer recommended)
- **JDK 17**

### Installation

1.  **Clone the Repository**
    ```bash
    git clone https://github.com/SV-stark/PodTrail.git
    cd PodTrail
    ```

2.  **Open in Android Studio**
    - Open Android Studio and select "Open an existing Android Studio project".
    - Navigate to the cloned `PodTrail` directory.

3.  **Build the Project**
    - Let Gradle sync the project dependencies.
    - Run the app on an emulator or physical device.

### Usage

1.  **Add a Podcast**: Tap the "Add" button and enter a valid public RSS feed URL.
2.  **Play an Episode**: Select a podcast, browse episodes, and tap to play.
3.  **Track Progress**: Playback progress is saved automatically.

## 🗺️ Roadmap

- [ ] **Enhanced Parsing**: Support for more iTunes tags and high-resolution artwork.
- [ ] **Background Playback**: Media notification controls and background service.
- [ ] **Cloud Sync**: Sync subscriptions and progress across devices.
- [ ] **Offline Mode**: Download episodes for offline listening.

## 📄 License

This project is licensed under the **GPL v3 License**. See the [LICENSE](LICENSE) file for details.
