// Onglet « Trio & Carte » : carte des villages et calendrier partagé du Trio (comme SharedCalendarScreen).
import { html, raw, avatar } from "../ui.js";
import { dateCourte, points, prenom, couleur, cleVillage } from "../fmt.js";

let filtreCalendrier = "ALL";
let carte = null;

async function bilansDuTrio(ctx) {
  const liste = await Promise.all(ctx.profils.map((p) => ctx.donnees.bilan(p.profileId, ctx.saison)));
  return Object.fromEntries(ctx.profils.map((p, i) => [p.profileId, liste[i]]));
}

function enTeteTrio(ctx, bilans) {
  return html`
    <section class="card">
      <h3>Bilan Trio Gaume Trail • ${ctx.saison}</h3>
      <p class="small muted" style="margin:-2px 0 8px">Challenges 10 km et 5 km comptabilisés séparément (max 15)</p>
      <div class="kpis" style="grid-template-columns:repeat(3,minmax(0,1fr))">
        ${ctx.profils.map((p) => {
          const b = bilans[p.profileId];
          if (!b) return "";
          const c10 = b.challenge10km, c5 = b.challenge5km;
          const principal = c10.completedRacesCount >= c5.completedRacesCount && (c10.completedRacesCount > 0 || p.preferredDistanceKm !== 5) ? c10 : c5;
          const second = principal.distanceKm === 10 ? c5 : c10;
          return html`<div class="kpi" style="${raw(`border-top:3px solid ${couleur(p.avatarColorHex)}`)}">
            <div style="display:flex;align-items:center;gap:6px">${avatar(p, 22)}<b style="white-space:nowrap;overflow:hidden;text-overflow:ellipsis">${prenom(p.displayName)}</b></div>
            <div class="v" style="margin-top:4px">${points(principal.totalChallengePoints)} pts</div>
            <div class="label muted">${principal.distanceLabel} : ${principal.countingRacesCount}/15</div>
            ${second.completedRacesCount > 0 ? html`<div class="label muted">${second.distanceLabel} : ${second.countingRacesCount}/15</div>` : ""}
          </div>`;
        })}
      </div>
    </section>`;
}

function carteCourse(item, ctx, bilans) {
  const race = item.race;
  const parProfil = item.resultsByProfileAndDistance || {};
  const quelquUn = Object.keys(parProfil).length > 0;
  return html`
    <article class="card tap race-card" data-act="fiche" data-race="${race.raceId}">
      <div class="race-head">
        <span class="pill ${race.isChallenge ? (race.isSeasonFinale ? "gold" : "round") : "hc"}">${race.isChallenge ? `M#${race.roundNumber}` : "HC"}</span>
        <div class="titles">
          <div class="title">${race.title}</div>
          <div class="sub">${race.village} • ${dateCourte(race.dateIso)} • ${race.isChallenge ? (race.isCompleted ? "terminée" : "à venir") : "hors challenge"}</div>
        </div>
      </div>
      ${quelquUn ? html`<div class="trio-results">${ctx.profils.map((p) => {
        const res = parProfil[p.profileId] || [];
        if (!res.length) {
          return html`<div class="trio-line">${avatar(p, 22)}<span class="n muted">${prenom(p.displayName)}</span><span class="r muted">Non courue</span></div>`;
        }
        return res.map((r) => {
          const b = bilans[p.profileId];
          const ch = r.distanceLabel === "5 km" ? b?.challenge5km : b?.challenge10km;
          const compte = race.isChallenge && (ch?.top15RaceIds || []).includes(race.raceId);
          return html`<div class="trio-line">${avatar(p, 22)}
            <span class="n">${prenom(p.displayName)} <span class="muted small">(${r.distanceName || r.distanceLabel})</span></span>
            <span class="r">#${r.bibNumber} • <b>${r.officialTimeFormatted}</b><br>
              <span style="${raw(`color:${compte ? "var(--top15)" : "var(--on-surface-variant)"}`)}">${race.isChallenge ? `${r.challengePoints} pts${compte ? " • Top 15" : ""}` : "hors challenge"}</span></span></div>`;
        });
      })}</div>` : ""}
    </article>`;
}

function calendrier(ctx, bilans) {
  const manches = ctx.bilan.racesWithContext || [];
  const hc = ctx.ecran.horsChallengeItems || [];
  const toutes = [...manches, ...hc].sort((a, b) => a.race.dateIso.localeCompare(b.race.dateIso) || a.race.roundNumber - b.race.roundNumber);
  const liste = filtreCalendrier === "COMPLETED" ? toutes.filter((i) => i.race.isCompleted)
    : filtreCalendrier === "UPCOMING" ? toutes.filter((i) => !i.race.isCompleted) : toutes;
  const filtres = [["ALL", `Toutes (${toutes.length})`], ["COMPLETED", "Terminées"], ["UPCOMING", "À venir"]];
  return html`
    ${enTeteTrio(ctx, bilans)}
    <div class="section-title"><h2>Calendrier ${ctx.saison} (${manches.length} manches${hc.length ? ` + ${hc.length} hors challenge` : ""})</h2></div>
    <div class="chips">${filtres.map(([v, l]) => html`<button class="chip" type="button" data-trio-filtre="${v}" aria-pressed="${filtreCalendrier === v}">${l}</button>`)}</div>
    ${liste.length ? liste.map((i) => carteCourse(i, ctx, bilans)) : html`<div class="card muted" style="text-align:center">Aucune course dans ce filtre.</div>`}`;
}

function vueCarte(ctx) {
  const pts = ctx.ecran.villageMapPoints || [];
  const visites = pts.filter((p) => Object.keys(p.visitsBySeason || {}).length).length;
  return html`
    <section class="card">
      <h3>Carte de la Gaume • ${prenom(ctx.profil.displayName)}</h3>
      <p class="small muted" style="margin:-2px 0 8px">${visites} village(s) visité(s) sur ${pts.length} • rond plein = déjà couru, rond vide = au calendrier</p>
      <div id="carte"></div>
    </section>`;
}

export async function vue(ctx) {
  if (!ctx.bilan || !ctx.ecran) return html`<div class="card">Pas encore de données pour cette saison.</div>`;
  const sous = ctx.sousOnglet ?? 1;
  const bilans = sous === 1 ? await bilansDuTrio(ctx) : null;
  return html`
    <div class="stack">
      <div class="segmented">
        <button class="chip" type="button" data-act="sous-onglet" data-v="0" aria-pressed="${sous === 0}">Carte</button>
        <button class="chip" type="button" data-act="sous-onglet" data-v="1" aria-pressed="${sous === 1}">Trio & Calendrier (${(ctx.bilan.racesWithContext || []).length})</button>
      </div>
      ${sous === 0 ? vueCarte(ctx) : calendrier(ctx, bilans)}
    </div>`;
}

export function apres(ctx, el) {
  el.querySelectorAll("[data-trio-filtre]").forEach((b) => b.addEventListener("click", () => {
    filtreCalendrier = b.dataset.trioFiltre;
    ctx.rafraichir();
  }));
  if ((ctx.sousOnglet ?? 1) === 0) dessinerCarte(ctx);
}

async function chargerLeaflet() {
  if (window.L) return window.L;
  if (!document.querySelector('link[href$="leaflet.css"]')) {
    const css = document.createElement("link");
    css.rel = "stylesheet";
    css.href = "vendor/leaflet/leaflet.css";
    document.head.appendChild(css);
  }
  await new Promise((ok, ko) => {
    const s = document.createElement("script");
    s.src = "vendor/leaflet/leaflet.js";
    s.onload = ok;
    s.onerror = ko;
    document.head.appendChild(s);
  });
  return window.L;
}

async function dessinerCarte(ctx) {
  const zone = document.getElementById("carte");
  if (!zone) return;
  let L;
  try {
    L = await chargerLeaflet();
  } catch (e) {
    zone.innerHTML = '<p class="muted small" style="padding:12px">Carte indisponible hors ligne.</p>';
    return;
  }
  if (carte) { carte.remove(); carte = null; }
  carte = L.map(zone, { zoomControl: true, attributionControl: true });
  L.tileLayer("https://tile.openstreetmap.org/{z}/{x}/{y}.png", {
    maxZoom: 17,
    attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
  }).addTo(carte);
  const pts = ctx.ecran.villageMapPoints || [];
  const coul = couleur(ctx.profil.avatarColorHex);
  const courses = [...(ctx.bilan.racesWithContext || []), ...(ctx.ecran.horsChallengeItems || [])];
  const limites = [];
  pts.forEach((p) => {
    const saisons = Object.entries(p.visitsBySeason || {});
    const visite = saisons.length > 0;
    const m = L.circleMarker([p.latitude, p.longitude], {
      radius: visite ? 8 : 6, color: coul, weight: 2, fillColor: coul, fillOpacity: visite ? 0.85 : 0.08
    }).addTo(carte);
    const ici = courses.filter((c) => cleVillage(c.race.village) === p.key);
    const info = ctx.villages[p.key];
    const lignes = [
      `<b>${escapeHtml(p.village)}</b>`,
      visite ? saisons.map(([y, n]) => `${y} : ${n} course(s)`).join(" • ") : "Pas encore couru",
      ...ici.map((c) => `<a href="#" data-act="fiche" data-race="${escapeHtml(c.race.raceId)}">${c.race.isChallenge ? "M#" + c.race.roundNumber : "HC"} • ${escapeHtml(dateCourte(c.race.dateIso))}</a>`),
      info?.found && info.extract ? `<span style="color:var(--on-surface-variant)">${escapeHtml(info.extract.slice(0, 160))}${info.extract.length > 160 ? "…" : ""}</span>` : ""
    ].filter(Boolean);
    m.bindPopup(lignes.join("<br>"));
    limites.push([p.latitude, p.longitude]);
  });
  if (limites.length) carte.fitBounds(limites, { padding: [20, 20] });
  else carte.setView([49.68, 5.55], 10);
}

function escapeHtml(s) {
  return String(s ?? "").replace(/[&<>"']/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));
}
