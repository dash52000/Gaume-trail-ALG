// Lecture des fichiers produits par le robot (dossier data/).
// Chaque fichier n'est téléchargé qu'une fois par visite.

const cache = new Map();

async function charger(chemin) {
  if (!cache.has(chemin)) {
    const p = fetch(`data/${chemin}`, { cache: "no-cache" }).then((r) => {
      if (!r.ok) throw new Error(`${chemin} : ${r.status}`);
      return r.json();
    });
    cache.set(chemin, p);
    p.catch(() => cache.delete(chemin));
  }
  return cache.get(chemin);
}

/** Facultatif : null si le fichier n'existe pas (saison sans données, trace absente…). */
async function chargerSiPresent(chemin) {
  try {
    return await charger(chemin);
  } catch (e) {
    return null;
  }
}

export const donnees = {
  meta: () => charger("meta.json"),
  /** Écran d'un profil, pour une saison et une distance (10 ou 5). */
  etat: (profil, saison, dist) => chargerSiPresent(`etat/${profil}-${saison}-${dist}.json`),
  /** Bilan de saison d'un profil (toutes ses courses de la saison). */
  bilan: (profil, saison) => chargerSiPresent(`bilans/${profil}-${saison}.json`),
  classement: (saison, dist) => chargerSiPresent(`classements/${saison}-${dist}.json`),
  trace: (raceId, profil) => chargerSiPresent(`traces/${raceId}__${profil}.json`),
  /** Oublie tout : prochain affichage = fichiers retéléchargés. */
  oublier: () => cache.clear()
};
