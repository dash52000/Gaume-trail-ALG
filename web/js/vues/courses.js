// Onglet « Courses » : en-tête du challenge, classement provisoire, prochaine manche, liste des manches.
import { html, raw, icon, avatar } from "../ui.js";
import { points, ecart, dateCourte, dec, dPlus, prenom, rang } from "../fmt.js";

const MAX = 15;

export const resultat = (item, dist) => (dist === 5 ? item.result5km : item.result10km);
export const compte = (item, dist) => (dist === 5 ? item.isCountingInTop15For5km : item.isCountingInTop15For10km);
const rangTop15 = (item, dist) => (dist === 5 ? item.rankAmongRunnerRaces5km : item.rankAmongRunnerRaces10km);
const challenge = (bilan, dist) => (dist === 5 ? bilan.challenge5km : bilan.challenge10km);

/** En-tête compact du challenge (comme UnifiedCompactChallengeHeaderCard). */
export function enTete(ctx) {
  const b = ctx.bilan;
  const actif = challenge(b, ctx.dist);
  const prog = Math.min(1, actif.countingRacesCount / MAX) * 100;
  const onglet = (c, titre, bib) => html`
    <button class="dist-tab" type="button" data-act="dist" data-v="${c.distanceKm}" aria-pressed="${c.distanceKm === ctx.dist}">
      <div class="t"><span>${titre}</span><span class="${c.isChallengeComplete ? "complete" : ""}">${c.countingRacesCount}/15</span></div>
      <div class="s">${c.hasAssignedBib ? `${bib ? "#" + bib : "Aucun"} • ${points(c.totalChallengePoints)} pts` : "Pas de dossard"}</div>
    </button>`;
  return html`
    <section class="card hero-card">
      <div class="hero-band">
        <div style="min-width:0">
          <div class="who">${b.profile.displayName} • Saison ${b.seasonYear}</div>
          <div class="bib">${actif.hasAssignedBib ? `Dossard #${actif.assignedBibNumber} (${actif.distanceLabel})` : `Aucun dossard sur le ${actif.distanceLabel}`}</div>
        </div>
      </div>
      <div class="hero-body">
        <div class="dist-tabs">
          ${onglet(b.challenge10km, "10 km", b.profile.defaultBib10km)}
          ${onglet(b.challenge5km, "5 km", b.profile.defaultBib5km)}
        </div>
        <div class="progress ${actif.isChallengeComplete ? "complete" : ""}"><div style="${raw(`width:${prog}%`)}"></div></div>
        ${actif.hasAssignedBib ? html`
          <div class="metrics">
            <div class="metric hl"><div class="l">Top 15 (${actif.countingRacesCount}/15)</div><div class="v">${points(actif.totalChallengePoints)} pts</div></div>
            <div class="metric"><div class="l">Moyenne / manche</div><div class="v">${points(actif.averagePointsPerRace)} pts</div></div>
            <div class="metric"><div class="l">Meilleur score</div><div class="v">${points(actif.bestPoints)} pts</div></div>
          </div>` : html`<div class="banner info small">0 course sur le ${actif.distanceLabel} (aucun dossard).</div>`}
      </div>
    </section>`;
}

/** Classement provisoire du profil et récapitulatif du Trio (comme ChallengeStandingCard). */
export function carteClassement(ctx) {
  const parProfil = ctx.ecran?.trioStandings || {};
  const mine = parProfil[ctx.profil.profileId]?.[String(ctx.dist)];
  const lignes = ctx.profils.map((p) => {
    const d = parProfil[p.profileId] || {};
    const dist = d[String(p.preferredDistanceKm)] ? p.preferredDistanceKm : Number(Object.keys(d)[0]);
    const s = d[String(dist)];
    return s ? { p, dist, s } : null;
  }).filter(Boolean);
  return html`
    <section class="card tap" data-act="onglet" data-v="classement">
      <div class="section-title" style="margin:0"><h2>Classement challenge ${ctx.dist} km</h2><span class="muted small">provisoire</span></div>
      ${mine ? html`
        <div class="standing-main">
          <div><div class="big">${mine.standing.overallRank}e / ${points(mine.runnersInDistance)}</div><div class="muted small">au général</div></div>
          <div><div class="big cat">${mine.standing.categoryRank}e / ${points(mine.runnersInCategory)}</div><div class="muted small">catégorie ${mine.standing.category || "?"}</div></div>
        </div>
        <div style="font-weight:600">${points(mine.standing.totalPoints)} pts • ${mine.standing.racesCounted}/${MAX} courses comptées${mine.standing.racesRun > mine.standing.racesCounted ? ` (${mine.standing.racesRun} courues)` : ""}</div>`
      : html`<p class="muted">${ctx.profil.displayName} n'apparaît pas encore au classement du ${ctx.dist} km.</p>`}
      ${lignes.length > 1 ? html`
        <hr class="soft">
        <div class="label" style="font-weight:700;margin-bottom:4px">Le Trio</div>
        <div class="trio-results">
          ${lignes.map(({ p, dist, s }) => html`
            <div class="trio-line">${avatar(p, 22)}<span class="n">${prenom(p.displayName)} • ${dist} km</span>
              <span class="r">${s.standing.overallRank}e / ${points(s.runnersInDistance)} • ${points(s.standing.totalPoints)} pts</span></div>`)}
        </div>` : ""}
      <p class="label muted" style="margin:8px 0 0">Recalculé depuis les résultats MyChrono (15 meilleurs résultats sur la distance). Le classement officiel peut différer en cas d'égalité. Touchez pour le classement complet.</p>
    </section>`;
}

/** Pronostic de la prochaine manche (comme NextUpcomingPronosticBannerCard). */
function prochaineManche(ctx) {
  const pr = ctx.ecran?.pronostics;
  const next = pr?.nextUpcomingPronostic;
  if (!pr?.hasAssignedBib || !next) return "";
  const villages = (pr.recentReferenceRaces || []).map((r) => r.villageName).join(", ");
  return html`
    <section class="card tap" data-act="fiche" data-race="${next.raceId}">
      <div class="label muted" style="font-weight:700">Pronostic prochaine manche (${pr.distanceLabel})</div>
      <div style="font-weight:800;margin-top:4px">PROCHAINE MANCHE • #${next.roundNumber} ${next.villageName.toUpperCase()}</div>
      <div class="muted small">${dec(next.distanceKm)} km • ${dPlus(next.elevationGainMeters)} • ${dateCourte(next.dateIso)}</div>
      <div class="metrics" style="margin-top:8px">
        <div class="metric hl"><div class="l">Temps prévu</div><div class="v">${next.predictedTimeFormatted}</div></div>
        <div class="metric"><div class="l">Allure prévue</div><div class="v" style="color:var(--teal)">${next.predictedPaceFormatted}</div></div>
      </div>
      ${villages ? html`<p class="small muted" style="margin:6px 0 0">Basé sur les ${pr.recentReferenceRaces.length} dernière(s) course(s) (${villages})</p>` : ""}
    </section>`;
}

function badgeTop15(item, dist, res) {
  const r = rangTop15(item, dist);
  if (r == null) return "";
  return compte(item, dist)
    ? html`<span class="pill top15" title="${res.distanceLabel} • Top 15 (#${r}/15)">${icon("check")}Top 15 · #${r}</span>`
    : html`<span class="pill out" title="${res.distanceLabel} • Hors Top 15 (#${r})">${icon("trophy")}Hors Top 15 · #${r}</span>`;
}

function ligneRecord(record) {
  if (!record) return "";
  const ref = record.bestBefore;
  switch (record.status) {
    case "BEATEN": return html`<div class="hint good">Record de parcours battu : ${ecart(record.deltaSeconds || 0)} sur ${ref?.seasonYear ?? ""}</div>`;
    case "NOT_BEATEN": return html`<div class="hint bad">${ecart(record.deltaSeconds || 0)} sur ton record (${ref?.timeFormatted ?? ""}, ${ref?.seasonYear ?? ""})</div>`;
    case "TO_BEAT": return html`<div class="hint gold">Record à battre : ${record.best.timeFormatted} (${record.best.seasonYear})</div>`;
    default: return "";
  }
}

/** Carte d'une manche (comme RaceRoundOverviewCard). */
export function carteManche(item, ctx, { record, prono } = {}) {
  const race = item.race;
  const dist = ctx.dist;
  const res = resultat(item, dist);
  const autre = resultat(item, dist === 5 ? 10 : 5);
  const tw = item.terrainWeather;
  const prevue = tw?.isPlannedForSimulator && tw.plannedDistanceKm === dist;
  let statut;
  if (res) statut = badgeTop15(item, dist, res);
  else if (prevue) statut = html`<span class="pill teal">${tw.simulatedTargetPoints ? `Prévue (${tw.simulatedTargetPoints} pts)` : "Prévue"}</span>`;
  else if (autre) statut = html`<span class="pill teal">Courue sur ${autre.distanceLabel}</span>`;
  else if (race.isCompleted) statut = html`<span class="pill out">Non courue</span>`;
  else statut = html`<span class="pill upcoming">${icon("clock")}À venir</span>`;
  return html`
    <article class="card tap race-card ${res && compte(item, dist) ? "counting" : ""}" data-act="fiche" data-race="${race.raceId}">
      <div class="race-head">
        <span class="pill ${res ? "done" : "round"}">M#${race.roundNumber}</span>
        <div class="titles">
          <div class="title">${race.title}</div>
          <div class="sub">${race.title.startsWith(item.gaumeGeo.canonicalVillage) ? "" : `${item.gaumeGeo.canonicalVillage} • `}${item.gaumeGeo.territoryName} • ${dateCourte(race.dateIso)}</div>
        </div>
        ${statut}
      </div>
      ${res ? html`
        <div class="mini-stats">
          <div class="mini-stat"><div class="l">Chrono</div><div class="v">${res.officialTimeFormatted}</div></div>
          <div class="mini-stat"><div class="l">Allure</div><div class="v">${res.pacePerKmFormatted}</div></div>
          <div class="mini-stat"><div class="l">Scratch</div><div class="v">${res.overallRank}e/${res.totalFinishers}</div></div>
          <div class="mini-stat hl"><div class="l">Points</div><div class="v">${res.challengePoints} pts</div></div>
        </div>` : ""}
      ${ligneRecord(record)}
      ${prono ? html`<div class="prono-mini"><span>Pronostic avant-course (${dec(prono.distanceKm)} km)</span><b>~${prono.predictedTimeFormatted} (${prono.predictedPaceFormatted})</b></div>` : ""}
    </article>`;
}

function carteHorsChallenge(item) {
  const race = item.race;
  const res = item.result10km || item.result5km;
  const distances = [race.longDistanceLabel, race.shortDistanceLabel].filter(Boolean).join(" / ");
  return html`
    <article class="card tap" data-act="fiche" data-race="${race.raceId}">
      <div class="race-head">
        <span class="pill hc">HC</span>
        <div class="titles">
          <div class="title">${race.title}</div>
          <div class="sub">${[item.gaumeGeo.canonicalVillage, dateCourte(race.dateIso), distances].filter(Boolean).join(" • ")}</div>
        </div>
        ${res ? html`<div style="text-align:right"><div class="mono" style="font-weight:800;color:var(--primary)">${res.officialTimeFormatted}</div>
            <div class="label muted">${res.distanceName || res.distanceLabel} • ${res.overallRank}e/${res.totalFinishers}</div></div>`
          : html`<span class="label muted">${race.isCompleted ? "Non courue" : "À venir"}</span>`}
      </div>
    </article>`;
}

export async function vue(ctx) {
  if (!ctx.bilan || !ctx.ecran) {
    return html`<div class="card">Pas encore de données pour ${ctx.profil.displayName} en ${ctx.saison}.</div>`;
  }
  const b = ctx.bilan;
  const dist = ctx.dist;
  const actif = challenge(b, dist);
  const recap = ctx.ecran.activeRecap;
  const prono = ctx.ecran.pronostics;
  const records = ctx.ecran.records?.byRaceId || {};
  const items = b.racesWithContext || [];

  let liste;
  switch (ctx.filtre) {
    case "COMPLETED": liste = items.filter((i) => resultat(i, dist)); break;
    case "UPCOMING": liste = items.filter((i) => !resultat(i, dist)); break;
    case "TOP_15_ONLY": liste = items.filter((i) => compte(i, dist)); break;
    default:
      liste = actif.hasAssignedBib
        ? items.slice().sort((a, c) => (resultat(c, dist) ? 1 : 0) - (resultat(a, dist) ? 1 : 0) || a.race.roundNumber - c.race.roundNumber)
        : [];
  }
  const filtres = [
    ["ALL", actif.hasAssignedBib ? "Toutes" : "Mes courses (0)"],
    ["COMPLETED", `Courues (${actif.completedRacesCount})`],
    ["TOP_15_ONLY", `Top 15 (${actif.countingRacesCount})`],
    ["UPCOMING", "Calendrier"]
  ];
  const hc = ctx.ecran.horsChallengeItems || [];

  return html`
    <div class="stack">
      ${enTete(ctx)}
      ${recap?.isSeasonOver && recap.racesRun > 0 ? html`
        <div class="banner gold card tap" data-act="onglet" data-v="perfs" data-sous="2">
          ${icon("trophy")}
          <div><strong>Saison ${recap.seasonYear} terminée</strong>
            <span class="small muted">${recap.challengeComplete ? `Challenge ${recap.distanceLabel} validé` : `${recap.countingRaces}/15 courses comptées`} • ${recap.racesRun} courses • touchez pour le bilan</span></div>
        </div>` : ""}
      ${actif.hasAssignedBib ? carteClassement(ctx) : ""}
      ${actif.hasAssignedBib ? prochaineManche(ctx) : ""}
      <div class="section-title"><h2>Manches ${b.seasonYear} • ${actif.distanceLabel}</h2>
        <span class="mono">${actif.completedRacesCount} courue(s) • ${actif.countingRacesCount}/15 Top 15</span></div>
      <div class="chips">
        ${filtres.map(([v, l]) => html`<button class="chip" type="button" data-act="filtre" data-v="${v}" aria-pressed="${ctx.filtre === v}">${l}</button>`)}
      </div>
      ${liste.length === 0 ? html`
        <div class="card muted" style="text-align:center">${actif.hasAssignedBib
          ? `0 course enregistrée dans ce filtre pour le Challenge ${actif.distanceLabel}.`
          : `0 course pour ${b.profile.displayName} sur le Challenge ${actif.distanceLabel} (aucun dossard attribué pour ce challenge).`}</div>`
        : liste.map((i) => carteManche(i, ctx, {
            record: records[i.race.raceId],
            prono: actif.hasAssignedBib ? prono?.allPronosticsByRaceId?.[i.race.raceId] : null
          }))}
      ${hc.length && ctx.filtre !== "TOP_15_ONLY" ? html`
        <div class="section-title" style="flex-direction:column;align-items:flex-start;gap:0">
          <h2>Hors challenge ${b.seasonYear} (${hc.length})</h2>
          <span class="small muted">${(() => { const n = hc.filter((i) => i.result10km || i.result5km).length; return n ? `${n} courue(s) • ` : ""; })()}résultats récupérés quand MyChrono les publie, sans effet sur le challenge</span>
        </div>
        ${hc.map(carteHorsChallenge)}` : ""}
    </div>`;
}

export { rang };
