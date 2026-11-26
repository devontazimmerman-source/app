# Vitron Remote (Android TV, over Wi-Fi)

Native Android app (Kotlin + Jetpack Compose) that pairs with an Android TV over your home Wi-Fi
and sends key presses. The layout copies the Vitron HTC 4068S remote.

## Build
1. Install Android Studio (Koala or newer).
2. File > Open, choose this `VitronRemote` folder, let Gradle sync finish (first sync downloads dependencies).
3. Plug in your phone (USB debugging on) or start an emulator, then press Run.
   To get an APK: Build > Build App Bundle(s) / APK(s) > Build APK(s).

## Use
1. Phone and TV on the same Wi-Fi, TV switched on.
2. Open the app, pick your TV (or type its IP address) and tap Pair.
3. Type the 6-character code shown on the TV.
4. Done. The TV is remembered; the app reconnects automatically.

## Notes
- Greyed-out keys (MODE, S.MODE, ASPECT, AUDIO MTS, T.SHIFT, REC, FAV, MEDIA, SCREENSHOT,
  FREEZE, REPEAT, SLEEP, mouse) exist on the original remote but have no Android TV Wi-Fi command.
- Keys that show media symbols (INDEX, TEXT, REVEAL, HOLD, SIZE, SUBPAGE) send previous, play/pause,
  next, rewind, stop and fast-forward, matching the printed icons.
- NFX / You / Facebook open those apps if they are installed on the TV.
- This was written without access to a real Vitron TV, so the pairing step is the part most likely
  to need a tweak. If pairing fails, note the exact message shown in the app.

## Code map (app/src/main/java/com/vitron/remote)
- net/Pairing.kt: pairing on port 6467 (code check)
- net/RemoteSession.kt: control connection on port 6466
- net/Tls.kt: client certificate (Android Keystore) and TLS sockets
- net/Proto.kt: minimal protobuf encoder/decoder, message framing
- net/Keys.kt: Android key codes
- ui/RemoteScreen.kt: the remote layout; ui/SetupScreen.kt: discovery + pairing screen
