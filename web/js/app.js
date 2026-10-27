// Gaume Trail ALG — version web (consultation).
// Affiche les données calculées chaque jour par le robot GitHub, avec les mêmes calculs que l'appli Android.
import { donnees } from "./donnees.js";
import { html, raw, icon, avatar, render } from "./ui.js";
import { dateLongue, prenom } from "./fmt.js";
import * as courses from "./vues/courses.js";
import * as perfs from "./vues/perfs.js";
import * as trio from "./vues/trio.js";
import * as classement from "./vues/classement.js";
import * as defis from "./vues/defis.js";
import { ouvrirFiche } from "./vues/fiche.js";

const ONGLETS = [
  { id: "courses", titre: "Courses", icone: "courses", vue: courses },
  { id: "perfs", titre: "Perfs", icone: "perfs", vue: perfs },
  { id: "trio", titre: "Trio & Carte", icone: "trio", vue: trio },
  { id: "defis", titre: "Défis", icone: "defis", vue: defis },
  { id: "classement", titre: "Classement", icone: "classement", vue: classement }
];

function lire(cle, defaut) {
  try { return localStorage.getItem(cle) ?? defaut; } catch (e) { return defaut; }
}
function ecrire(cle, valeur) {
  try { localStorage.setItem(cle, valeur); } catch (e) { /* stockage indisponible : tant pis */ }
}

const etat = {
  meta: null,
  moi: lire("alg.moi", null),
  profil: null,
  saison: null,
  /** Distance choisie pour chaque profil (sinon sa distance du challenge). */
  distances: {},
  onglet: (location.hash || "").replace("#", "") || "courses",
  filtre: "ALL",
  sousOnglets: { perfs: 0, trio: 1, defis: 0 },
  classement: { recherche: "", filtre: "tous", nombre: 100 }
};

const $ = (id) => document.getElementById(id);

function profilActif() {
  return etat.meta.profils.find((p) => p.profileId === etat.profil) || etat.meta.profils[0];
}

function distance() {
  const p = profilActif();
  return etat.distances[p.profileId] || (p.preferredDistanceKm === 5 ? 5 : 10);
}

/** Ce que reçoit chaque écran. */
async function contexte() {
  const p = profilActif();
  const dist = distance();
  const [ecran, bilan] = await Promise.all([
    donnees.etat(p.profileId, etat.saison, dist),
    donnees.bilan(p.profileId, etat.saison)
  ]);
  return {
    meta: etat.meta,
    profils: etat.meta.profils,
    profil: p,
    moi: etat.moi,
    saison: etat.saison,
    dist,
    ecran,
    bilan,
    filtre: etat.filtre,
    sousOnglet: etat.sousOnglets[etat.onglet] ?? 0,
    classement: etat.classement,
    villages: etat.meta.villages || {},
    donnees,
    rafraichir: afficher,
    ouvrirFiche: (raceId) => ouvrirFiche(raceId, { meta: etat.meta, profil: p, saison: etat.saison, dist, ecran, bilan, villages: etat.meta.villages || {} })
  };
}

function afficherHaut() {
  const p = profilActif();
  render($("profiles"), etat.meta.profils.map((x) => html`
    <button class="profile-chip" type="button" data-act="profil" data-v="${x.profileId}" aria-pressed="${x.profileId === p.profileId}">
      ${avatar(x, 22)}<span class="name">${prenom(x.displayName)}</span>
    </button>`).join(""));

  render($("season"), etat.meta.saisons.slice().reverse().map((s) =>
    html`<option value="${s}" ${raw(s === etat.saison ? "selected" : "")}>Saison ${s}</option>`).join(""));

  const sombre = document.documentElement.dataset.theme === "dark" ||
    (!document.documentElement.dataset.theme && matchMedia("(prefers-color-scheme: dark)").matches);
  render($("theme"), icon(sombre ? "sun" : "moon"));

  render($("nav"), ONGLETS.map((o) => html`
    <button class="nav-btn" type="button" data-act="onglet" data-v="${o.id}" ${raw(o.id === etat.onglet ? 'aria-current="page"' : "")}>
      <span class="ic">${icon(o.icone)}</span>${o.titre}
    </button>`).join(""));

  $("data-date").textContent = etat.meta.jour ? `Résultats MyChrono du ${dateLongue(etat.meta.jour)}` : "Allures Libres de Gaume";
}

let rendu = 0;
async function afficher({ hautDePage = false } = {}) {
  const n = ++rendu;
  afficherHaut();
  const onglet = ONGLETS.find((o) => o.id === etat.onglet) || ONGLETS[0];
  const ctx = await contexte();
  if (n !== rendu) return; // un autre affichage a été demandé entre-temps
  const main = $("view");
  try {
    const contenu = await onglet.vue.vue(ctx);
    if (n !== rendu) return;
    const moi = etat.meta.profils.find((x) => x.profileId === etat.moi);
    render(main, html`${contenu}
      <p class="footer-note">${moi ? html`Appli ouverte par ${prenom(moi.displayName)} · <a href="#" data-act="qui">changer</a> · ` : ""}${etat.meta.synchro}</p>`);
    onglet.vue.apres?.(ctx, main);
  } catch (e) {
    console.error(e);
    render(main, html`<div class="error">Impossible d'afficher cet écran : ${e.message}</div>`);
  }
  if (hautDePage) window.scrollTo({ top: 0 });
}

function quiEsTu() {
  const root = $("welcome-root");
  render(root, html`
    <div class="welcome" role="dialog" aria-label="Qui es-tu ?">
      <div class="hero"><div><h1>Qui es-tu&nbsp;?</h1><p>L'appli s'ouvrira toujours sur ta fiche. Les deux autres restent à un tap.</p></div></div>
      <div class="body">
        ${etat.meta.profils.map((p) => html`
          <button class="who-btn" type="button" data-act="choisir" data-v="${p.profileId}">
            ${avatar(p, 44)}
            <span><strong>${p.displayName}</strong><span class="muted small">Challenge ${p.preferredDistanceKm} km · ${p.clubName}</span></span>
          </button>`)}
      </div>
    </div>`);
}

document.addEventListener("click", (ev) => {
  const el = ev.target.closest("[data-act]");
  if (!el) return;
  const act = el.dataset.act;
  const v = el.dataset.v;
  switch (act) {
    case "onglet":
      etat.onglet = v;
      if (el.dataset.sous != null) etat.sousOnglets[v] = Number(el.dataset.sous);
      history.replaceState(null, "", "#" + v);
      afficher({ hautDePage: true });
      break;
    case "sous-onglet":
      etat.sousOnglets[etat.onglet] = Number(v);
      afficher();
      break;
    case "profil":
      etat.profil = v;
      etat.classement.nombre = 100;
      afficher();
      break;
    case "dist":
      etat.distances[profilActif().profileId] = Number(v);
      etat.classement.nombre = 100;
      afficher();
      break;
    case "filtre":
      etat.filtre = v;
      afficher();
      break;
    case "classement-filtre":
      etat.classement.filtre = v;
      etat.classement.nombre = 100;
      afficher();
      break;
    case "plus":
      etat.classement.nombre += 200;
      afficher();
      break;
    case "fiche":
      ev.preventDefault();
      contexte().then((ctx) => ctx.ouvrirFiche(el.dataset.race));
      break;
    case "choisir":
      etat.moi = v;
      etat.profil = v;
      ecrire("alg.moi", v);
      render($("welcome-root"), "");
      afficher({ hautDePage: true });
      break;
    case "qui":
      ev.preventDefault();
      quiEsTu();
      break;
    default:
      return;
  }
});

$("season").addEventListener("change", (ev) => {
  etat.saison = Number(ev.target.value);
  etat.classement.nombre = 100;
  afficher({ hautDePage: true });
});

$("theme").addEventListener("click", () => {
  const sombre = document.documentElement.dataset.theme === "dark" ||
    (!document.documentElement.dataset.theme && matchMedia("(prefers-color-scheme: dark)").matches);
  const theme = sombre ? "light" : "dark";
  document.documentElement.dataset.theme = theme;
  ecrire("alg.theme", theme);
  afficher();
});

// Recherche dans le classement : on garde le curseur dans le champ.
document.addEventListener("input", (ev) => {
  if (ev.target.id !== "recherche") return;
  etat.classement.recherche = ev.target.value;
  etat.classement.nombre = 100;
  clearTimeout(document.rechercheTimer);
  document.rechercheTimer = setTimeout(async () => {
    await afficher();
    const champ = $("recherche");
    if (champ) {
      champ.focus();
      champ.setSelectionRange(champ.value.length, champ.value.length);
    }
  }, 180);
});

// L'appli revient au premier plan (iPhone) : nouvelles données éventuelles.
let dernierRetour = Date.now();
document.addEventListener("visibilitychange", async () => {
  if (document.visibilityState !== "visible" || Date.now() - dernierRetour < 10 * 60 * 1000) return;
  dernierRetour = Date.now();
  donnees.oublier();
  try {
    etat.meta = await donnees.meta();
    afficher();
  } catch (e) { /* hors ligne : on garde l'affichage */ }
});

async function demarrer() {
  try {
    etat.meta = await donnees.meta();
  } catch (e) {
    render($("view"), html`<div class="error">Les données ne sont pas encore disponibles (${e.message}). Réessaie dans quelques minutes.</div>`);
    return;
  }
  etat.saison = etat.meta.saisonParDefaut;
  const ids = etat.meta.profils.map((p) => p.profileId);
  if (!ids.includes(etat.moi)) etat.moi = null;
  etat.profil = etat.moi || ids[0];
  if (!ONGLETS.some((o) => o.id === etat.onglet)) etat.onglet = "courses";
  await afficher();
  if (!etat.moi) quiEsTu();
}

if ("serviceWorker" in navigator) {
  window.addEventListener("load", () => navigator.serviceWorker.register("sw.js").catch(() => {}));
}

demarrer();
