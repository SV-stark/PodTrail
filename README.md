# PodTrail

<img width="592" height="595" alt="podtrail" src="https://github.com/user-attachments/assets/9ff08f4a-5a53-46cb-b205-8d31fdbee25e" />

PodTrail is a minimal Android podcast tracker (Kotlin + Jetpack Compose + ExoPlayer) that:
- Lets you add podcast RSS feed URLs
- Parses feeds (including iTunes tags: episode number, duration)
- Stores podcasts & episodes in Room
- Plays episode audio (ExoPlayer)
- Tracks playback progress and marks episodes as "listened" when playback passes a threshold (90%)

Quickstart
1. Create a new repository on GitHub named "podtrail" (or run the commands below).
2. Clone the repo or create the Android Studio project locally.
3. Add the source files under package `com.example.podtrack`.
4. Build & run on an emulator or device with network access.
5. Add a feed URL (public RSS), open a podcast, tap an episode to play. Playback progress is saved; if you listen >= 90% the episode will be marked as listened.

Notes and next steps
- Improve feed parsing (more itunes tags, artwork).
- Add background playback & media notification.
- Add sync across devices (server or cloud).
- Add downloads & offline playback.

License: MIT 
