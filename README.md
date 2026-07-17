# App-Limit

Eine native **Android**-Kindersicherungs-App (Kotlin + Jetpack Compose) im
**iOS-Look**, die die App-Nutzung auf dem Gerät des Kindes begrenzt — ähnlich
wie Google Family Link, aber als eigenständige App.

> Android, weil nur Android die nötigen System-Berechtigungen für Fremd-Apps
> (UsageStats, Accessibility, Overlay, Device-Admin) überhaupt zulässt. Die
> Optik ist bewusst im iOS-Stil gehalten (abgerundete Karten, iOS-Switches,
> helle Palette, ein Akzentton).

## Funktionen

- **PIN-Schutz** (Punkt 1): 4-stelliger Kinder-PIN, längerer Eltern-PIN. Beides
  gehasht (PBKDF2) in `EncryptedSharedPreferences` gespeichert, nie im Klartext.
- **Wochenlogik**: Die App lässt sich nur einmal pro Woche in einem
  konfigurierbaren Zeitfenster öffnen. Danach gesperrt — außer per Eltern-PIN.
- **Drei App-Kategorien** (Punkt 2): Plus-Apps (unbegrenzt), Apps mit Limit,
  gesperrte Apps.
- **Zeitlimits** (Punkt 3): Tageslimit (Standard 1 h, harte Obergrenze 2 h) für
  limitierte Apps; Geräte-Vollsperre bei Erreichen der Gesamtnutzungszeit.
  Automatischer Reset um Mitternacht.
- **Overlay-Pop-up** (Punkt 4): systemweites Overlay über limitierten/gesperrten
  Apps.
- **Eltern-Bereich** (Punkt 6): Kategorien, Limits, Nutzungsstatistik,
  Notfall-Entsperren — hinter dem Eltern-PIN.

## Architektur

```
app/src/main/java/com/applimit/
├── data/
│   ├── db/          Room: ManagedApp + AppCategory
│   ├── prefs/       SecurePinStore (Keystore), SettingsStore (DataStore)
│   └── repository/  AppLimitRepository (zentrale Fassade)
├── domain/          Reine Logik: LimitEvaluator, WeeklyAccess, UsageStatsReader
├── service/         AccessibilityService, EnforcementService (Foreground),
│                    Enforcer, OverlayController, DeviceLockController
├── ui/              Jetpack Compose (iOS-Theme + Screens)
│   ├── theme/  components/  pin/  onboarding/  home/  parent/
└── util/            PermissionsHelper
```

Die Limit-Entscheidung (`domain/LimitEvaluator`) ist bewusst frei von
Android-Abhängigkeiten und per JUnit getestet (`app/src/test`).

## Bauen

```bash
./gradlew assembleDebug        # APK bauen
./gradlew testDebugUnitTest    # Unit-Tests (Limit-Logik)
```

Benötigt Android SDK 34. Beim ersten Start führt die App durch das
Berechtigungs-Onboarding (die Sonderzugriffe müssen manuell in den
Android-Einstellungen erteilt werden — das ist systembedingt so).

## Wichtig: Sperr-Stufen & Grenzen

Welche Sperre technisch realistisch ist (Overlay vs. Geräteadmin vs. Device
Owner) und wie die offenen Auftragsfragen entschieden wurden, steht ausführlich
in **[docs/DECISIONS.md](docs/DECISIONS.md)** und als Code-Kommentar in
`service/DeviceLockController.kt`.
