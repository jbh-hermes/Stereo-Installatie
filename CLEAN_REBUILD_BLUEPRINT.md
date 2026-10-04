# Stereo-Installatie – Definitieve Android Bouwblauwdruk

## 1. Doel
Een volledig nieuwe Android-app voor bediening van:
- Marantz NA8005 netwerkspeler
- Marantz PM8003 versterker

De app wordt opnieuw opgebouwd vanaf nul.
De vorige tijdelijke Java/Compose-versies gelden niet als technische basis.
De oude app en het nieuwe visuele ontwerp gelden alleen als functionele en visuele referentie.

## 2. Visuele basis
Vaste stijl:
- Donkere antraciet/zwarte achtergrond
- Donkergrijze cards
- Goud/amber als hoofdkleur voor knoppen en iconen
- Paars voor actieve tabselectie
- Witte primaire tekst
- Lichtgrijze secundaire tekst
- Afgeronde cards en knoppen
- Echte Material Icons
- Geen emoji- of Unicode-iconen
- Compacte bediening passend op Samsung S26
- Bottom navigation altijd boven de Android-systeembalk

App-logo:
- Zwarte/donkere basis
- Platenspeler/vinyl als hoofdsymbool
- Gouden accenten
- Naam/beeldmerk Stereo-Installatie
- Geschikt als adaptive Android launcher icon

## 3. Hoofdnavigatie
Vijf vaste tabs:
1. Start
2. Radio
3. Favorieten
4. Versterker
5. Bronnen

De actieve tab krijgt:
- Paarse indicator
- Wit icoon
- Witte tekst

Niet-actieve tabs:
- Lichtgrijs icoon
- Lichtgrijze tekst

## 4. Tab 1 – Start
Doel: in één oogopslag zien wat speelt en snel een bron kiezen.

Boven:
- Titel: Stereo-Installatie
- Instellingenknop rechtsboven

Now Playing-card:
- Bron/station
- Artiest
- Titel
- Eventueel albumart indien beschikbaar
- Stereo VU-meters L/R
- VU-meters bewegen op audio wanneer technisch beschikbaar
- Knop: Zender aan favorieten toevoegen

Snel kiezen:
- Internet Radio
- Muziekserver
- USB
- Favorieten

Gedrag:
- Tik Internet Radio -> Radio-tab
- Tik Muziekserver -> NA8005 bron Muziekserver
- Tik USB -> NA8005 bron USB
- Tik Favorieten -> Favorieten-tab

## 5. Tab 2 – Radio
Titel: Radio

NA8005 power:
- NA8005 aan
- NA8005 uit / stand-by

Huidig station:
- Stationnaam
- Artiest
- Titel

Bediening:
- Vorige
- Volgende
- Zender bewaren

Zoeken:
- Zoekveld
- Zoekknop
- Zoekresultaten in app
- Geen browser/webpagina openen

Favoriet opslaan:
- Keuze uit slot 01 t/m 50
- Bezet/vrij zichtbaar
- Bestaande favoriet niet stil overschrijven
- Bevestiging bij overschrijven

## 6. Tab 3 – Favorieten
Titel: Marantz-favorieten

Doel:
- Alle 50 Marantz-favorietslots beheren

Per slot:
- Slotnummer 01–50
- Stationnaam
- Radio-icoon
- Tik om direct af te spelen
- Verwijderknop

Onder/boven:
- Favorieten vernieuwen
- Favoriet toevoegen

Gedrag:
- Lijst rechtstreeks uit NA8005 ophalen
- Geen hardcoded stations
- Geen webpagina openen
- Vrije slots herkenbaar
- Bij fout duidelijke melding

## 7. Tab 4 – Versterker
Titel: PM8003 versterker

### Alles
- Alles aan
- Alles uit

Alles aan:
1. PM8003 aan
2. NA8005 aan
3. App wacht op bevestiging/verbinding waar nodig

Alles uit:
1. PM8003 stand-by
2. NA8005 stand-by

Belangrijk:
- Alles-uit mag niet stoppen na alleen NA8005.
- Beide opdrachten afzonderlijk uitvoeren en controleren.

### Alleen PM8003
- Aan
- Stand-by

### Volume
- Zachter
- Harder
- Dempen
- Geluid aan

### Ingang kiezen
- CD
- Tuner
- AUX
- Phono

Belangrijk:
- PM8003-bediening heeft een eigen client/controller.
- PM8003-commando's mogen nooit via de NA8005 power-functies lopen.

## 8. Tab 5 – Bronnen
Titel: Bronnen

Bronnen:
- Internet Radio
- Muziekserver / NAS
- USB
- Favorieten
- Spotify
- Digitaal coax
- Digitaal optisch
- USB-DAC

Elke bron:
- Eigen icoon
- Naam
- Tik -> juiste NA8005 bron selecteren

## 9. Apparaten strikt scheiden

### Na8005Client
Verantwoordelijk voor:
- Power
- Stand-by
- Now Playing
- Internet Radio
- Muziekserver/NAS
- USB
- Spotify
- Digitale ingangen
- Favorieten
- Bronselectie

### Pm8003Client
Verantwoordelijk voor:
- Aan
- Stand-by
- Volume harder
- Volume zachter
- Mute
- Unmute
- CD
- Tuner
- AUX
- Phono

Geen enkele UI-knop mag rechtstreeks netwerkcode uitvoeren.
UI -> ViewModel -> juiste client.

## 10. Netwerk en Auto-IP

### NA8005
- Laatst werkend IP onthouden
- Eerst dat IP testen
- Indien onbereikbaar: UPnP/SSDP discovery
- Nieuw gevonden IP opslaan
- Huidig bekend IP: 192.168.68.73

### PM8003
De PM8003-netwerkmethode moet vóór implementatie exact worden vastgesteld uit:
- oude APK
- fysieke aansluiting met NA8005
- eventueel remote-out/remote-control gedrag

Geen aannames meer over PM8003-protocol.

## 11. Architectuur
Voorgestelde structuur:

app/
- ui/
  - MainScreen
  - StartScreen
  - RadioScreen
  - FavoritesScreen
  - AmplifierScreen
  - SourcesScreen
  - components/
- viewmodel/
  - StereoViewModel
  - RadioViewModel
  - FavoritesViewModel
  - AmplifierViewModel
- data/
  - Na8005Client
  - Na8005Discovery
  - Pm8003Client
  - FavoritesRepository
  - RadioRepository
  - NowPlayingRepository
- model/
  - FavoriteSlot
  - NowPlaying
  - Source
  - DeviceState
- theme/
  - Color
  - Type
  - Shape

## 12. App-status
De UI krijgt echte apparaatstatus in plaats van alleen “opdracht verzonden”.

Voorbeelden:
- NA8005: Aan / Stand-by / Niet bereikbaar
- PM8003: Aan / Stand-by / Status onbekend
- Geselecteerde bron
- Mute-status indien uitleesbaar

Geen onjuiste melding “staat al aan” zonder echte statuscontrole.

## 13. Foutafhandeling
Meldingen moeten duidelijk zijn:
- NA8005 niet gevonden
- PM8003 reageert niet
- Favorieten konden niet worden geladen
- Bron kon niet worden geselecteerd
- Netwerkverbinding ontbreekt

Geen technische foutcodes in normale UI.

## 14. VU-meters
Startscherm krijgt stereo VU-meters.

Eerste versie:
- Visuele meters in vintage stijl
- L/R gescheiden

Tweede stap:
- Indien audio-analyse vanuit beschikbare stream haalbaar is: echte live beweging
- Anders beweging op basis van beschikbare audiostatus/API

Meters mogen de app niet vertragen.

## 15. Logo en launcher-icon
Eigen logo:
- Vinyl/platenspeler
- Donkere achtergrond
- Goudkleurig
- Stereo-Installatie herkenbaar

Android:
- Adaptive icon foreground
- Adaptive icon background
- Round icon
- Launcher icon mipmap-resources

## 16. Bouwvolgorde

### Fase A – Fundament
1. Schoon Android-project
2. Compose theme
3. 5-tab navigatie
4. Logo + launcher icon

### Fase B – NA8005
5. Auto-IP / discovery
6. Power aan/uit
7. Bronnen
8. Now Playing

### Fase C – PM8003
9. Exact protocol vaststellen
10. Aan/stand-by
11. Volume/mute
12. Ingangen

### Fase D – Radio
13. Huidig station
14. Vorige/volgende
15. Zoeken
16. Station bewaren

### Fase E – Favorieten
17. Slots 01–50 uitlezen
18. Afspelen
19. Toevoegen
20. Verwijderen

### Fase F – Afwerking
21. VU-meters
22. Pixel-perfect styling
23. Samsung S26 test
24. Foutafhandeling
25. Release APK

## 17. Testmatrix

NA8005:
- Aan vanuit stand-by
- Stand-by vanuit aan
- Internet Radio
- NAS
- USB
- Spotify
- Coax
- Optisch
- USB-DAC
- Now Playing

PM8003:
- Aan vanuit stand-by
- Stand-by vanuit aan
- Volume +
- Volume -
- Mute
- Unmute
- CD
- Tuner
- AUX
- Phono

Favorieten:
- Laden
- Slot 01
- Slot 50
- Vrij slot
- Toevoegen
- Overschrijven
- Verwijderen
- Afspelen

Alles aan/uit:
- Beide apparaten reageren
- Geen apparaat wordt overgeslagen

## 18. Definition of Done
De app is pas klaar als:
- alle vijf tabs werken
- NA8005 en PM8003 correct gescheiden zijn
- favorieten 01–50 werken
- geen browser nodig is voor normale bediening
- Auto-IP werkt
- UI overeenkomt met het gekozen ontwerp
- bottom nav goed staat op Samsung S26
- launcher-icon/logo correct is
- alles-aan en alles-uit beide apparaten correct bedienen
