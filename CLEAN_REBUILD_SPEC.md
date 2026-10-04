# Stereo-Installatie – Clean Rebuild Functionele Specificatie

## Doel
Volledig nieuwe Android-app voor bediening van:
- Marantz NA8005
- Marantz PM8003

Belangrijk:
- NA8005- en PM8003-bediening technisch gescheiden houden.
- Auto-IP voor de NA8005.
- Oude app alleen gebruiken als visuele/functionele referentie.
- Vijf vaste tabbladen onderaan.

## Tab 1 – Start
Toont:
- Actief radiostation
- Artiest
- Titel

Snel kiezen:
1. Internet Radio
2. Muziekserver
3. USB
4. Favorieten

## Tab 2 – Radio
Functies:
- NA8005 aan
- NA8005 uit / stand-by
- Huidig radiostation tonen
- Zender bewaren
- Vorige radiostation
- Volgende radiostation
- Radiostation zoeken
- Zoekveld voor vTuner / internet radio

## Tab 3 – Favorieten
Functies:
- Marantz-favorieten uitlezen
- Maximaal 50 slots
- Favoriet direct afspelen
- Favoriet toevoegen
- Favoriet verwijderen
- Favorieten vernieuwen
- Slotnummers 01 t/m 50 zichtbaar

## Tab 4 – Versterker
Boven:
- Alles aan
- Alles uit

Alleen PM8003:
- Aan
- Stand-by

Volume:
- Harder
- Zachter
- Dempen
- Geluid aan

Ingang kiezen:
- CD
- Tuner
- AUX
- Phono

Belangrijk:
- PM8003-opdrachten mogen nooit per ongeluk NA8005 power bedienen.
- PM8003 krijgt een eigen controller/service-laag.

## Tab 5 – Bronnen
Bronnen:
1. Internet Radio
2. Muziekserver / NAS
3. USB
4. Favorieten
5. Spotify
6. Digitaal coax
7. Digitaal optisch
8. USB-DAC

## Navigatie
Vijf vaste tabbladen:
1. Start
2. Radio
3. Favorieten
4. Versterker
5. Bronnen

De navigatiebalk moet altijd volledig boven de Samsung/Android systeembalk staan.

## Vormgeving
Gebaseerd op de oude app:
- Donkere achtergrond
- Donkergrijze kaarten
- Goudkleurige bedieningselementen
- Paarse actieve tab/knopaccenten
- Echte Material-iconen
- Geen emoji/unicode-pictogrammen
- Compacte knoppen
- Teksten mogen niet afbreken of verdwijnen
- Layout geschikt voor Samsung S26

## Architectuur
Losse componenten:
- Na8005Discovery
- Na8005Client
- Pm8003Client
- RadioRepository
- FavoritesRepository
- NowPlayingRepository
- UI / Compose-schermen

Geen functie mag rechtstreeks een verkeerde apparaatclient aanroepen.

## Bouwvolgorde
1. App-shell + 5 tabs
2. NA8005 discovery + power
3. PM8003 power + inputs + volume
4. Now Playing
5. Radio bediening
6. Favorieten 01-50
7. Bronnen
8. Volledige visuele afwerking
9. Eindtest per functie
