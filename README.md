# WordChain

![Java](https://img.shields.io/badge/Java-8%2B-orange)
![Android](https://img.shields.io/badge/Android-8.0%2B-green)
![Networking](https://img.shields.io/badge/Networking-TCP%20sockets-blue)

A real-time multiplayer word game for desktop and Android, written in Java with plain socket programming.
Change **one letter** of the current word to make a new real English word, then pass the turn:

```
TALK  ->  WALK  ->  WALL  ->  WELL  ...
```

Take turns, beat the countdown, and be the last player standing. Desktop and Android players can share the same room.

## Features

- **2-4 players per room**, joined with a 4-character room code
- **Random or custom start word** (3-8 letters, chosen by the host)
- **Server-side validation**: exactly one letter changed, a real English word (about 170,000 words), and not already played
- **Turn countdown**: run out of time and you lose a life; the clock gets faster as the chain grows
- **Lives and scoring**: faster answers and longer chains score more
- **Dead-end bonus**: leave the next player with no possible move and earn extra points
- **Hints**: 2 per player per game, each costing 5 points
- **Room chat** and a live word-chain history
- **Host controls** in the lobby: players, seconds per turn, lives, start word
- **Resign / disconnect handling** and automatic host transfer
- **Desktop UI**: animated tiles, dark theme, confetti results screen
- **Android UI**: on-screen keyboard, haptic feedback, share button for the room code, screen stays on during play
- **Host anywhere**: on a dedicated server, on a PC, or directly on a phone

## How to Play

1. The host creates a room and shares the code. Everyone else joins with it.
2. When the game starts, a word appears. On your turn, **tap/click a letter to erase it**, type a replacement, and submit.
3. The new word must be a real word that has not been played yet. Invalid words are rejected and you can try again until the timer ends.
4. If time runs out you lose a life and the turn passes on. At zero lives you are out.
5. The last player standing wins.

### Rules and scoring

| Rule | Value |
| --- | --- |
| Players per room | 2-4 |
| Lives each | 1-5 (default 3) |
| Seconds per turn | 10-60 (default 20) |
| Word length | 3-8 letters |
| Points per word | 10 + 2 per second left + chain bonus (up to 10) |
| Dead-end bonus | +25 to the player who made the stuck word |
| Hints | 2 per player, -5 points each |
| Speed-up | 1 second faster every 4 words, never below 8 seconds |

If a word has no possible next move, the game announces a **dead end** and picks a fresh starting word.

## Tech Stack

- **Language:** Java (server and Android code are Java 8 compatible; desktop client is built with JDK 17+)
- **Networking:** TCP sockets, one text line per message (tab-separated fields)
- **Desktop client:** Java Swing with custom-painted components
- **Android client:** native Android SDK, all UI in code, no third-party libraries
- **Server:** multi-threaded, one `GameRoom` state machine per room, `ScheduledExecutorService` for turn timers
- **Word lists:** ENABLE word list and a common-words list (see Acknowledgements)

## Project Layout

```
wordchain/            Desktop client + server (JDK 17+)
  common/             Message, Protocol, WordRules (shared rules and wire format)
  server/             GameServer, ClientSession, GameRoom, Dictionary, GameSettings
  client/             Swing screens, custom components, socket Connection
wordchain-server/     Java 8 compatible dedicated server (jar + start-server.bat)
wordchain-android/    Android Studio project (Java 8 compatible copy of the shared and server code)
```

## Run Locally

### 1. Start a server (one machine)

Requires Java 8 or newer.

```
java -jar wordchain-server.jar          # default port 5050
java -jar wordchain-server.jar 6000     # custom port
```

On Windows you can double-click `start-server.bat`. The server prints the addresses players should use, for example `192.168.1.20 (port 5050)`.

### 2. Play

- **Desktop:** `java -jar wordchain.jar` (needs Java 17+, or the version it was built with). Enter a name, then either connect to a server address or tick **Host the game on this computer**.
- **Android:** install `WordChain.apk` (Android 8.0+, allow installs from unknown sources). Enter a name and the server address, or tick **Host the game on this phone**.

Everyone on the same Wi-Fi (or hotspot) connects using the address the host or server shows, with port `5050`.

> Android emulator tip: the emulator reaches your PC at `10.0.2.2`.

## Build From Source

**Desktop client**

```
./build.sh          # or build.bat on Windows; produces wordchain.jar
```

**Android app**

- Open `wordchain-android` in Android Studio and press Run, or
- Without Gradle: `ANDROID_JAR=... BUILD_TOOLS=... ./build_apk.sh`

**Dedicated server (Java 8)**

```
javac --release 8 -d out $(find src -name '*.java')
cp resources/*.txt out/
jar cfe wordchain-server.jar wordchain.server.ServerMain -C out .
```

## Deployment

The game server is a single small Java process, so it can run anywhere that can open a TCP port.

- **Same network:** run the server on any PC and share its LAN address (see above).
- **Over the internet:** run the server on a public host (a small VPS or container host) and open TCP port 5050, or expose a home PC with a TCP tunnel such as ngrok or playit.gg. Players then enter that public address in the app.
- **Without a server:** one phone or PC can host directly from the app, but other players must be able to reach it on the network.

Note: the server has no accounts or rate limiting. That is fine for playing with friends, but add basic limits before exposing it widely.

## Protocol Overview

Each message is one line: `TYPE<TAB>arg1<TAB>arg2...`

| Direction | Messages |
| --- | --- |
| Client to server | `HELLO` `CREATE` `JOIN` `SETTINGS` `START` `MOVE` `HINT` `CHAT` `RESIGN` `LEAVE` |
| Server to client | `WELCOME` `JOINED` `LEFT` `ROOM` `GAME_START` `TURN` `ACCEPTED` `REJECTED` `TIMEOUT` `ELIMINATED` `DEAD_END` `HINT_POS` `GAME_OVER` `INFO` `ERROR` `CHAT_MSG` |

The server is authoritative: clients only send intents (for example `MOVE position letter`) and the server validates every move, runs the timers and broadcasts the results.

## FAQ

**Why can't my friend join from a different network?**
The host's address is only reachable on the local network. For internet play, use a public server or a tunnel (see Deployment).

**The phone can't connect to my PC server but the emulator can.**
The emulator uses `10.0.2.2`, which bypasses the network. A real phone needs the PC's Wi-Fi address, the phone must be on the same network, and Windows Firewall must allow inbound TCP on port 5050:
`netsh advfirewall firewall add rule name="WordChain" dir=in action=allow protocol=TCP localport=5050` (run as administrator). Also make sure the Wi-Fi is set to a Private network and the router does not isolate devices.

**I get `UnsupportedClassVersionError` when starting the server.**
The jar was built for a newer Java than the one installed. Use `wordchain-server.jar`, which runs on Java 8 and up, or install a newer JDK.

**Can I use my own word list?**
Yes. Replace `words.txt` (all valid words) and `starters.txt` (common words used for random starts), one word per line, and rebuild.

**Does the Android app work in the background?**
While hosting, keep the app open; Android may stop background apps. Joining someone else's room has no such limit.

## Contributing

Ideas and fixes are welcome. Good places to start:

- A public-server mode with a join link (`wordchain://join/ABCD`)
- Rate limiting and basic abuse protection on the server
- Sound effects and more themes
- Unit tests for `GameRoom` and `WordRules`

## Authors

- Tinaishe Tapera

## Acknowledgements

- [ENABLE word list](https://github.com/dolph/dictionary) (public domain) for move validation
- Popular-words list from the same repository for random start words
