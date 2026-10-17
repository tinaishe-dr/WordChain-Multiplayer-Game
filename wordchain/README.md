# WordChain – multiplayer word game (Java, sockets, Swing)

Change **one letter** of the current word to make a new real English word
(TALK → WALK → WALL …). Take turns, beat the clock, outlast your friends.

## Requirements
JDK 17+ (no other dependencies). Word lists ship in `resources/` (ENABLE list + popular words).

## Build & run
    ./build.sh            # or build.bat on Windows
    java -jar wordchain.jar                          # start the game client (one per player)
    java -cp wordchain.jar wordchain.server.ServerMain [port]   # optional dedicated server (default 5050)

Easiest play: one person ticks **"Host the game on this computer"** on the first screen;
everyone else enters that person's IP (shown in the menu) and port 5050.

## Features
- 2–4 players per room, 4-character room codes, lobby with live settings (host only)
- Random start word or your own custom word
- Server validates every move: exactly one letter changed, real word, not already played
- Turn countdown (timeout = lose a life); the clock shrinks as the chain grows
- Lives, scoring (speed + chain bonus), dead-end bonus, 2 hints per player (-5 pts)
- Resign, disconnect handling, host transfer, rematch from the lobby
- Room chat + system notices, word-chain history, animated tiles, confetti results screen

## Controls
Click a tile → type the new letter → Enter. Arrow keys move the selection, Backspace clears, Esc cancels.

## Layout
    common/   Message, Protocol, WordRules      shared by both sides
    server/   GameServer, ClientSession, GameRoom (state machine), Dictionary, GameSettings
    client/   net/Connection, RoomState, ui/* (screens + custom-painted components)
    test/     BotTest (headless 3-bot server test), Snap (renders screens to PNG)
