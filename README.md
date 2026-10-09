# bixi4flip

![logo](/app/src/main/res/mipmap-xxxhdpi/ic_launcher.png)

Voici une appli Android qui donne accès, sur une flip phone, à la carte de stations Bixi, et *que* à ça. Pour *emprunter* un Bixi en tant que flipphoneuse ou flipphoneur, il faut commander un [Clé Bixi](https://support.bixi.com/hc/fr/articles/7787493633811-Comment-commander-une-cl%C3%A9-BIXI) !

Here's an Android app that gives access, on a flip phone, to the Bixi station map (and *only* to that). To *borrow* a Bixi as a flipphoner, you need to order a [Bixi Key](https://support.bixi.com/hc/en-ca/articles/7787493633811-How-can-I-order-a-BIXI-key)!

## Ce que c'est / What it is

Une intégration basique entre Open Street Map et l'API public de BIXI, avec de la navigation par bouton. Ça permet de voir le nombre de vélos réguliers et éléctriques, ainsi que les points d'ancrage vides, dans toutes les stations BIXI pres de chez vous et à travers le Québec.

A basic integration between Open Street Map and the public Bixi API, with button navigation. It lets you see the number of regular and electric bikes, plus empty docks, at all the Bixi stations in your area and across Québec.

## AI Disclaimer IA

### En français

J'ai genéré la grande partie de ce code vraiment dans quelques secondes avec la version gratuite de Claude. J'en suis pas grandement fier, mais c'est vraiment pour réduire ma dépendence sur ma téléphone intélligente. Je suis plus développeur web que Android, mais le code n'est pas très immense, alors j'ai implémenté la traduction d'interface à la main, et s'il y a des bogues, je vais les fixer à la main.

J'ai fait le logo d'app dans [Photopea](https://www.photopea.com/) (pas avec de l'IA). Le logo "BIXI" appartient à Bixi. Je l'ai copié... sans permission. En esperant que je serai pas poursuit en justice ! La flip phone dans l'image est un Motorola Razr, ce qui peut pas rouler cette application, car ça ne roule pas Android.

### In english

I generated most of this code in really a few seconds with the free version of Claude. I'm not super proud of that, but it's really to reduce my dependance on my smartphone. I'm more of a web than Android developer, but the code isn't huge, so I implemented the UI translation by hand, and I will fix any bugs by hand, if there are any.

I made the app logo in [Photopea](https://www.photopea.com/) (not with AI). "BIXI" logo belongs to Bixi. I copied it... without permission. Hopefully will not be sued! The flip phone pictured is a Motorola Razr, which cannot run this app because it doesn't run Android.

## "Screenshots" (je sais pas prendre un screenshot sur mon flip phone)

<img width="500" alt="P_20261009_151114" src="https://github.com/user-attachments/assets/d79101d6-8282-4606-bb58-9a4f67b426b4" />

<img width="500"  alt="P_20261009_151057" src="https://github.com/user-attachments/assets/15219a93-0100-4874-ac16-4866afa7d7d9" />

<img width="500" alt="P_20261009_151046" src="https://github.com/user-attachments/assets/f3162f47-1da9-4c24-8cb0-7a769400c7d0" />

## Comment installer ? How to install

### En français

Notez que si votre flip phone n'est pas basé sur Android, elle ne roulera pas cette application.

1. Télécharger [adb](https://developer.android.com/tools/adb). Suivez les indications sur cette page pour comment utiliser adb avec votre téléphone.
2. Connecter votre flip phone à votre ordi via USB
3. Télécharger l'apk des Releases
4. Ouvrir un terminal dans le dossier de téléchargements et rouler : `adb install org.benwiley.bixi4flip.apk`

### In english

Note that if your flip phone is not based on Android, it won't run this app.

1. Download [adb](https://developer.android.com/tools/adb). Follow the instructions on this page for how to use adb with your phone.
2. Connect your Android flip phone to your computer with USB
3. Download the apk from releases
4. Open a terminal in the download directory and run: `adb install org.benwiley.bixi4flip.apk`

## Comment utiliser ? How to use

### En français

Les contrôles sont pas mal expliquées sur l'écran, mais en gros :

- Bouger sur carte : Flèches
- Approcher : 3, # ou Augmenter Volume
- Reculer : 1, * ou Baisser Volume
- Recentrer sur votre position : 5 ou OK
- Forcer une mise à jour de la carte (pas typiquement nécessaire) : 0

Si la carte commence centrée sur la Gare Centrale à Montréal, ça veut dire que votre position n'était pas immédiatement disponible au moment de charger l'application. Au bout de plusieurs secondes, ressayez le 5 ou OK pour recentrer sur votre position.

### In english

The controls are mostly explained on the screen (in French), but basically:

- Move around the map: Arrows
- Zoom in: 3, # or Volume up
- Zoom out: 1, * or Volume down
- Recenter on your location: 5 or OK
- Force a map update (not usually necessary): 0

If the map starts centered on Gare Centrale in Montreal, that means your location wasn't immediately available when the application loaded. After several seconds, retry 5 or OK to recenter on your location.

## How to build

### En français

1. Télécharger [Android studio](https://developer.android.com/studio)
2. Git clone ce repo
3. Ouvrir le repo dans Android studio
4. Une fois les choses chargées, ouvrir un terminal
5. `./gradlew assembleDebug`
6. Pour installer sur votre téléphone connectée via USB : `adb install app/build/outputs/apk/debug/app-debug.apk`

### In english

1. Download [Android studio](https://developer.android.com/studio)
2. Git clone this repo
3. Open the repo in Android studio
4. Once things are done loading, open a terminal
5. `./gradlew assembleDebug`
6. To install the built apk on your phone connected via USB: `adb install app/build/outputs/apk/debug/app-debug.apk`

## Pull requests

SVP pas d'améliorations, c'est vraiment censé d'être un truc très simple. S'il s'agit véritablement d'une fix bogue, allez-y, mais soyez certain de bien comprendre le code que vous me donnez, s'il vous plaît.

Please no "improvements," it's really meant to be a very simple thing. If you've really got a bug fix, go ahead, but be certain to understand the code you're providing, please.
