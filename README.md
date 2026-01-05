# PodTrail

<div align="center">
  <img width="300" height="300" alt="PodTrail Logo" src="https://github.com/user-attachments/assets/9ff08f4a-5a53-46cb-b205-8d31fdbee25e" />
  <br>
  <br>

  ![Android Build](https://github.com/SV-stark/PodTrail/actions/workflows/build.yml/badge.svg)
  ![License](https://img.shields.io/badge/License-GPL%20v3-blue.svg)
  ![Kotlin](https://img.shields.io/badge/Kotlin-2.2.0-purple.svg)

  <p>
    <b>A minimal, privacy-focused Android podcast tracker.</b>
  </p>
</div>

---

## 📖 Overview

**PodTrail** is a lightweight Android application designed for podcast enthusiasts who value simplicity and privacy. Built with modern Android technologies, it offers a seamless experience for tracking your favorite podcasts.

## ✨ Features

- **Podcast Search & Discovery**:  Search for podcasts directly using the **iTunes API** or explore top charts by genre.
- **RSS Feed Management**: Easily add podcast RSS URLs to subscribe.
- **Smart Parsing**: Automatically parses feeds to extract metadata, including iTunes tags, episode numbers, and high-quality artwork.
- **Modern UI**: Clean **Material 3** interface with Dynamic Color support, AMOLED mode, and customizable themes.
- **Episode Tracking**: 
    - Track listening history and streaks. 
    - Sort episodes by **Date** (Newest/Oldest) or **Duration** (Shortest/Longest).
    - Mark episodes as listened with a single tap.
- **Real-Time Details**: View full episode descriptions, including rich text, fetched in real-time.
- **Personalized Profile**: Visualize your listening stats with a dynamic genre breakdown chart.
- **Calendar View**: Review your listening history organized by date.

## 🛠️ Tech Stack

- **Language**: [Kotlin](https://kotlinlang.org/)
- **UI Framework**: [Jetpack Compose](https://developer.android.com/jetbrains/compose)
- **Database**: [Room](https://developer.android.com/training/data-storage/room)
- **Network**: [OkHttp](https://square.github.io/okhttp/) & [Gson](https://github.com/google/gson)
- **Architecture**: MVVM with Uni-directional Data Flow
- **Image Loading**: [Coil](https://coil-kt.github.io/coil/)

## 🚀 Getting Started

Follow these steps to build and run the application locally.

### Prerequisites

- **Android Studio** (Ladybug | 2024.2.1 or newer recommended)
- **JDK 21** (Required for Gradle 9.2+)

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

1.  **Add a Podcast**: Tap the "Add" button to search or enter a valid RSS feed URL.
2.  **Discover**: Browse curated lists and genres in the Discover tab.
3.  **Manage Episodes**: Open a podcast to view episodes. use the Sort button to organize them.
4.  **Track Progress**: Mark episodes as listened. Check the Profile tab to see your listening stats and genre breakdown.

## 🗺️ Roadmap

- [ ] **Cloud Sync**: Sync subscriptions and progress across devices.
- [ ] **Offline Playback**: Download episodes for offline listening.


## 📄 License

This project is licensed under the **GPL v3 License**. See the [LICENSE](LICENSE) file for details.
