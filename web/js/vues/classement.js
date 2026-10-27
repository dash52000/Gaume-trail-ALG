// Onglet « Classement » : classement provisoire complet du Challenge (15 meilleurs résultats sur la distance).
import { html, avatar } from "../ui.js";
import { points, prenom } from "../fmt.js";

function normaliser(s) {
  return (s || "").toLowerCase().normalize("NFD").replace(/[̀-ͯ]/g, "");
}

export async function vue(ctx) {
  const lignes = await ctx.donnees.classement(ctx.saison, ctx.dist);
  const distTabs = html`
    <div class="segmented">
      ${[10, 5].map((d) => html`<button class="chip" type="button" data-act="dist" data-v="${d}" aria-pressed="${ctx.dist === d}">Challenge ${d} km</button>`)}
    </div>`;
  if (!lignes || !lignes.length) {
    return html`<div class="stack">${distTabs}<div class="card muted">Pas encore de classement ${ctx.dist} km pour ${ctx.saison}.</div></div>`;
  }

  // Le Trio dans ce classement (par identifiant MyChrono).
  const trioParId = new Map(ctx.profils.filter((p) => p.myChronoRunnerId).map((p) => [String(p.myChronoRunnerId), p]));
  const trio = lignes.filter((l) => trioParId.has(String(l.id)));
  const moiLigne = lignes.find((l) => String(l.id) === String(ctx.profil.myChronoRunnerId));
  const maCat = moiLigne?.cat;
  const parCat = {};
  lignes.forEach((l) => { parCat[l.cat] = (parCat[l.cat] || 0) + 1; });

  const { recherche, filtre, nombre } = ctx.classement;
  let liste = lignes;
  if (filtre === "cat" && maCat) liste = liste.filter((l) => l.cat === maCat);
  if (filtre === "trio") liste = trio;
  if (filtre === "complet") liste = liste.filter((l) => l.comptees >= 15);
  const q = normaliser(recherche.trim());
  if (q) liste = liste.filter((l) => normaliser(l.nom).includes(q) || String(l.dossard) === q.replace("#", ""));
  const visibles = liste.slice(0, nombre);

  const filtres = [
    ["tous", `Tous (${points(lignes.length)})`],
    maCat ? ["cat", `Cat. ${maCat} (${parCat[maCat]})`] : null,
    ["complet", "15 courses"],
    ["trio", `Le Trio (${trio.length})`]
  ].filter(Boolean);

  return html`
    <div class="stack">
      ${distTabs}
      ${trio.length ? html`
        <section class="card">
          <h3>Le Trio au classement ${ctx.dist} km ${ctx.saison}</h3>
          <div class="trio-results">${trio.map((l) => {
            const p = trioParId.get(String(l.id));
            return html`<div class="trio-line">${avatar(p, 24)}<span class="n"><b>${prenom(p.displayName)}</b> <span class="muted small">cat. ${l.cat} : ${l.rangCat}e / ${parCat[l.cat]}</span></span>
              <span class="r"><b>${l.rang}e</b> / ${points(lignes.length)}<br><span class="muted">${points(l.points)} pts • ${l.comptees}/15</span></span></div>`;
          })}</div>
        </section>` : ""}
      <input id="recherche" class="search" type="search" placeholder="Chercher un nom ou un dossard" value="${recherche}" autocomplete="off" enterkeyhint="search">
      <div class="chips">${filtres.map(([v, l]) => html`<button class="chip" type="button" data-act="classement-filtre" data-v="${v}" aria-pressed="${filtre === v}">${l}</button>`)}</div>
      <section class="card" style="padding:4px 8px">
        <div class="standings">
          ${visibles.length === 0 ? html`<p class="muted" style="text-align:center">Personne ne correspond.</p>` : visibles.map((l) => {
            const p = trioParId.get(String(l.id));
            return html`<div class="st-row ${p ? "trio" : ""}">
              <div class="rk">${l.rang}</div>
              <div class="nm"><div>${p ? avatar(p, 16) : ""} ${l.nom}</div><div>#${l.dossard || "—"} • ${l.cat || "?"} ${l.rangCat}e${l.courues > l.comptees ? ` • ${l.courues} courues` : ""}</div></div>
              <div class="pt">${points(l.points)} pts<small>${l.comptees}/15</small></div>
            </div>`;
          })}
        </div>
        ${liste.length > visibles.length ? html`<button class="btn more" type="button" data-act="plus">Voir plus (${points(liste.length - visibles.length)} restants)</button>` : ""}
      </section>
      <p class="label muted" style="margin:0">Classement provisoire recalculé depuis les résultats MyChrono : somme des 15 meilleurs résultats sur une même distance (règlement ALG). Le classement officiel peut différer en cas d'égalité.</p>
    </div>`;
}
