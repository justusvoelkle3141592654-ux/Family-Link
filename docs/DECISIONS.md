# Entscheidungen & technische Grenzen

Dieses Dokument beantwortet transparent die offenen Fragen aus dem Projektauftrag
und erklärt ehrlich, welche Sperr-Stufe technisch erreichbar ist.

## Beantwortung der offenen Fragen

Die vier Rückfragen aus dem Auftrag wurden mit sinnvollen Standardwerten
beantwortet. **Alle sind im Elternbereich bzw. über Code-Konstanten änderbar.**

| # | Frage | Getroffene Entscheidung | Wo änderbar |
|---|-------|-------------------------|-------------|
| 1 | Zählt Plus-App-Zeit in die 2h-Vollsperre? | **Ja, alle Apps zählen** (wie im Auftrag wörtlich formuliert). | Elternbereich → Zeitlimits → „Alle Apps zählen mit" (`AppSettings.fullLockCountsAllApps`) |
| 2 | Dritte Kategorie „komplett gesperrt"? | **Ja, umgesetzt** (Auftrag Punkt 2 beschreibt drei Kategorien). | Kategorie bleibt einfach leer, wenn ungenutzt (`AppCategory.BLOCKED`) |
| 3 | Vollsperre täglich um Mitternacht zurücksetzen? | **Ja, automatisch um 00:00.** | Elternbereich (`AppSettings.autoResetAtMidnight`) |
| 4 | Ziel-Android-Version? | **minSdk 29 (Android 10)**, targetSdk 34. | `app/build.gradle.kts` |

## Sperr-Stufen: Was ist realistisch umsetzbar?

Der Auftrag verlangt eine ehrliche Kennzeichnung, welche Sperre erreicht wird.
Der vollständige Code-Kommentar dazu steht in
`app/src/main/java/com/applimit/service/DeviceLockController.kt`.

### Stufe 1 – Overlay-Sperre (immer verfügbar)
Ein bildschirmfüllendes `SYSTEM_ALERT_WINDOW`-Overlay legt sich über die App und
macht sie unbedienbar. Über den AccessibilityService wird das Overlay bei jedem
App-Wechsel sofort wieder gezeigt, sodass Home-/Recents-Buttons die blockierte
App nicht dauerhaft freigeben.
**Grenze:** Power-Button und Notruf bleiben erreichbar — das lässt sich auf
normalem Android nicht vollständig verhindern.

### Stufe 2 – Geräteadministrator `lockNow()` (optional, im Onboarding aktivierbar)
Erzwingt den sicheren Sperrbildschirm des Geräts. Das Kind muss den
Geräte-Code neu eingeben. **Grenze:** Power-Button/Notruf weiterhin nutzbar.

### Stufe 3 – Device Owner (nur bei Ersteinrichtung)
Die einzige wirklich unumgehbare Stufe — genau wie Google Family Link. Ermöglicht
Lock-Task-/Kiosk-Modus (Home, Recents, Statusleiste deaktiviert, keine
Deinstallation). **Erfordert QR-/NFC-/afw-Provisionierung auf einem frisch
zurückgesetzten Gerät** und kann NICHT nachträglich auf einem bereits
eingerichteten Telefon aktiviert werden.

**Diese App liefert Stufe 1 + 2 out of the box** (auf jedem normalen Telefon
installierbar). Stufe 3 ist dokumentiert, wird aber nicht automatisch
provisioniert, weil das nach der Ersteinrichtung physikalisch unmöglich ist.

## Berechtigungen, die manuell erteilt werden müssen

Keine dieser Sonderzugriffe kann per Laufzeit-Dialog angefordert werden. Der
Onboarding-Flow (`ui/onboarding`) führt Schritt für Schritt zu den jeweiligen
Einstellungsseiten:

- **Nutzungszugriff** (`PACKAGE_USAGE_STATS`) — App-Nutzungsstatistik
- **Bedienungshilfe** (AccessibilityService) — App-Wechsel-Erkennung
- **Über anderen Apps anzeigen** (`SYSTEM_ALERT_WINDOW`) — Overlays
- **Geräteadministrator** (optional) — echtes `lockNow()`

## Sicherheit der PINs

PINs werden nie im Klartext gespeichert. Es wird ein zufälliger Salt plus ein
PBKDF2-SHA256-Hash gespeichert, und zwar in `EncryptedSharedPreferences`, deren
Schlüssel im Android Keystore (wo vorhanden hardwaregestützt) liegt. Siehe
`data/prefs/SecurePinStore.kt`.
