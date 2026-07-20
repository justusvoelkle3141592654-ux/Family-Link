# Stadt-Land-Fluss (gegen den Computer)

Eine native Android-App des Klassikers **Stadt-Land-Fluss** – du spielst gegen
den Computer. Offline, ohne Werbung, ohne Berechtigungen.

## Spielen

1. Kategorien auswählen (Stadt, Land, Fluss, Name, Tier, Beruf) und optional das
   60-Sekunden-Zeitlimit aktivieren.
2. Ein zufälliger Buchstabe wird gezogen – finde zu jeder Kategorie ein Wort,
   das mit diesem Buchstaben beginnt.
3. Auf **Fertig** tippen. Der Computer spielt gleichzeitig mit.
4. Punkte pro Kategorie:
   - **20 Punkte** – gültiges Wort, das nur du hast
   - **10 Punkte** – ihr habt beide dasselbe Wort
   - **0 Punkte** – kein oder ungültiges Wort
5. Weiter mit **Nächste Runde** oder **Spiel beenden** für den Endstand.

## APK herunterladen

Bei jedem Push auf den Branch baut GitHub Actions die App und veröffentlicht die
Datei `Stadt-Land-Fluss.apk`:

- **Release:** unter *Releases* → Tag `apk-latest` → `Stadt-Land-Fluss.apk`
- **Artefakt:** im jeweiligen Workflow-Lauf unter *Artifacts*

Voraussetzung zum Installieren: Android 8.0 (API 26) oder neuer. Eventuell muss
„Installation aus unbekannten Quellen“ erlaubt werden.

## Selbst bauen

```bash
./gradlew assembleDebug
# Ergebnis: app/build/outputs/apk/debug/app-debug.apk
```

Benötigt JDK 17 und das Android SDK (Platform 34, Build-Tools 34.0.0).

## Projektaufbau

| Datei | Zweck |
|-------|-------|
| `MainActivity.kt` | Oberfläche & Spielablauf (programmatisch aufgebaut) |
| `GameLogic.kt` | Regeln, Buchstaben, Bewertung/Punkte |
| `WordBank.kt` | Wortschatz des Computer-Gegners je Kategorie |
