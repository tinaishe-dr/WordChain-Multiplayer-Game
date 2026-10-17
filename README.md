# WordChain for Android (native, Java)

Same game and network protocol as the desktop version: change **one letter** to make a new real
word (TALK -> WALK -> WALL ...). 2-4 players, turn timer, lives, scoring, hints, dead-end bonus, chat.
Desktop and Android players can share a room.

## Install
- **Quick:** copy `WordChain.apk` to the phone and open it (allow "install unknown apps"). Android 8.0+.
- **Android Studio:** open this folder, let Gradle sync, press Run. (Gradle wrapper jar not included;
  Studio downloads Gradle 8.7 from `gradle-wrapper.properties`.)
- **Command line (no Gradle):** `ANDROID_JAR=... BUILD_TOOLS=... ./build_apk.sh`

## Playing
1. One phone ticks **Host the game on this phone** and taps Play -> Create a room. The menu shows
   the address friends should use (same Wi-Fi / hotspot).
2. Others enter that address + port 5050 (or a desktop server's address) and join with the 4-letter code.
3. Tap a tile, tap the new letter on the on-screen keyboard, press **Submit**.

Hosting runs inside the app, so keep the host's app open (screen stays on during a game).
Prefer a dedicated host? Run the desktop `ServerMain` and everyone connects to it.

## Code map
    wordchain/common, wordchain/server, wordchain/net   game rules, rooms, protocol, socket client
                                                        (Android-safe copies of the desktop code)
    com/wordchain/android/                              UI: MainActivity + 4 screens, custom views
                                                        (tiles, keyboard, timer, players), chat dialog
All UI is built in code (no XML layouts, no third-party libraries).
