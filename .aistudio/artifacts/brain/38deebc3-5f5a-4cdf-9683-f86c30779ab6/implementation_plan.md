# Plan : Pronostics uniquement sur les prochaines courses à venir (basés sur les 3 dernières courses)

## Objectif
Supprimer les pronostics rétrospectifs sur les courses déjà terminées (ou passées) pour que le **Pronostic** serve uniquement à préparer les **prochaines manches à venir**, calculé de façon transparente à partir de tes **3 dernières courses courues**.

---

## Modifications prévues

### 1. Moteur de calcul basé exclusivement sur les 3 dernières courses (`GaumePerfsCalculator`)
- **Sélection de l'historique récent** :
  - Récupération des **jusqu'à 3 dernières manches réellement courues** par le coureur sur la distance active (`10 km` ou `5 km`), triées par ordre chronologique.
  - Calcul de l'**Allure Équivalente Plat récente** uniquement à partir de ces courses récentes (en neutralisant leur `D+` et leur terrain).
  - Conservation de la liste des **courses récentes de référence** (village, chrono, allure) pour afficher clairement sur quelles courses repose le pronostic.
  - **Aucun pronostic fictif** si le coureur n'a encore aucune course enregistrée sur la distance.
- **Filtrage strict sur les prochaines manches à venir** :
  - Seules les manches **non encore courues** situées **après la dernière manche courue** (manches futures du calendrier) reçoivent une entrée de pronostic (`GaumeRacePronosticEntry`).
  - Les manches déjà courues (ayant un chrono MyChrono) et les manches passées manquées n'ont **aucun pronostic** dans `allPronosticsByRaceId`.

### 2. Mise à jour de l'onglet `Courses` et de la `Fiche Détail`
- **Cartes de la liste des courses (`RunnerOverviewScreen`)** :
  - **Manche terminée** : affiche uniquement le chrono officiel, l'allure, le classement, les points Challenge et le bouton **Carte Bilan** (plus de badge « Mieux / Moins bien que prévu »).
  - **Manche à venir** : affiche le badge `⏱️ Pronostic : ~XX:XX (X:XX /km)` calculé sur les 3 dernières courses.
- **Bandeau en haut de `Courses` (`NextUpcomingPronosticBannerCard`)** :
  - Met en avant uniquement la **prochaine manche à venir** avec son temps estimé et le rappel des **courses récentes utilisées** comme base.
- **Fiche Détail d'une manche (`RaceDetailModalScreen`)** :
  - Si la manche est **déjà courue** : affiche uniquement la carte **Résultat Officiel MyChrono** (sans bloc pronostic rétrospectif).
  - Si la manche est **à venir** : affiche la carte **Pronostic Avant-Course** détaillée (temps estimé, allure cible, D+ du parcours et rappel des 3 dernières courses de référence).

### 3. Mise à jour de l'onglet `Perfs > Pronostics` (`GaumePerfsAndShareHub`)
- **Carte « Mon Historique Récent (Base de calcul) »** : présente les **3 dernières courses courues** (village, chrono, D+, allure équivalente plat retenue).
- **Liste « Pronostics des Prochaines Manches à Venir »** : affiche uniquement les manches futures du calendrier avec leur estimation de temps et d'allure adaptée au dénivelé (`+D+`) de chaque village.
