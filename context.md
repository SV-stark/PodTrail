<project>
name: PodTrail
type: Android App
domain: Podcast Tracker (Privacy-focused, Minimal, Local-first)
repo: e:\PodTrail
</project>

<stack>
lang: Kotlin 2.2+ (exclusive, NO Java)
ui: Jetpack Compose Material 3 (NO XML)
db: Room
net: OkHttp + Gson
img: Coil
arch: MVVM + UDF (Uni-directional Data Flow)
build: Gradle kts, JDK 21, Android Studio Ladybug+
</stack>

<features>
search: iTunes API
rss: URL addition, smart parsing (XML to domain models)
tracking: Listen history, streaks, duration/date sorting
ui: M3 Dynamic Color, AMOLED dark, Calendar history, Profile stats
</features>

<directories base="app/src/main/java/com/...">
ui/: Compose screens (HomeScreen, DiscoverScreen)
ui/components/: Reusable Compose views (EpisodeCard, AudioPlayer)
ui/theme/: M3 Material Theme (Color, Typography, Shapes)
data/: Room entities, DAOs, Repositories
network/: OkHttp clients, iTunes APIs, RSS/XML parsers
viewmodel/: Binds data to UI states
models/: Domain & DTOs
</directories>

<rules>
state: Expose StateFlow from ViewModels, collectAsState in Compose
concurrency: Coroutines (viewModelScope, lifecycleScope)
imports: Explicitly stated, NO wildcard (*) imports
errors: runCatching/try-catch for network/parse -> pass state to UI via VM
formatting: Idiomatic Kotlin
</rules>

<llm_instructions>
- Output CODE ONLY when requested. Omit boilerplate/filler.
- Prefer inline functions/extensions when saving lines/tokens without harming readability.
- Re-use existing Material 3 Compose components.
- Adhere strictly to existing directory and naming structures.
</llm_instructions>
