# Gaume Trail ALG

Suivi multi-coureurs du Challenge des Allures Libres de Gaume : calendrier, résultats MyChrono, traces GPX et progression sur plusieurs saisons. Appli Android générée avec Google AI Studio.

## Récupérer l'APK

Chaque push sur `main` compile l'appli via GitHub Actions et publie l'APK dans les [Releases](../../releases/latest).
Sur le téléphone : télécharger `gaume-trail-alg.apk`, l'ouvrir, autoriser l'installation depuis le navigateur la première fois.

Les mises à jour s'installent par-dessus l'ancienne version sans perdre les données, grâce à la clé de signature fixe `debug.keystore` à la racine.

## Version web (iPhone et autres)

Version consultation de l'appli, publiée sur GitHub Pages : https://dash52000.github.io/Gaume-trail-ALG/

- **Sur iPhone** : ouvrir l'adresse dans Safari, bouton Partager, « Sur l'écran d'accueil ». L'appli demande « Qui es-tu ? » à la première ouverture, puis s'ouvre toujours sur la bonne fiche.
- **Les données** : le robot (`.github/workflows/web.yml`) récupère les résultats MyChrono chaque matin et le week-end au soir, refait les calculs avec le code de l'appli Android (`WebDataExportTest`, `AlgUiStateBuilder`) et écrit `web/data/`. Les chiffres sont donc les mêmes que sur le téléphone.
- **Les saisies** (dossards, traces GPX, notes, objectifs) : sauvegarde de l'appli Android déposée dans `donnees/saisies.json`. Le robot la reprend à chaque passage.
- **Le site** (`web/`) : HTML, CSS et JavaScript sans compilation. Il s'installe comme une appli (PWA) et s'ouvre hors ligne avec les dernières données vues.

Le dépôt est public : tout ce qui est dans `web/data/` et `donnees/` est visible par qui a l'adresse.
