// Onglet « Perfs » : Forme, Records, Bilan, Saisons (comme GaumePerfsAndToolsHubSection).
import { html, raw, avatar } from "../ui.js";
import { allure, dec, dateCourte, jourMois, duree, ecartCourt, indice, points, prenom } from "../fmt.js";
import { enTete } from "./courses.js";

const SOUS_ONGLETS = ["Forme", "Records", "Bilan", "Saisons"];

/** Courbe de forme : allure brute et allure corrigée du parcours (plus haut = plus rapide). */
function courbeForme(pts) {
  const W = 340, H = 150, PL = 44, PR = 8, PT = 10, PB = 22;
  const valeurs = pts.flatMap((p) => [p.paceSecPerKm, p.correctedPaceSecPerKm].filter((v) => v != null));
  const min = Math.min(...valeurs) - 5, max = Math.max(...valeurs) + 5;
  const x = (i) => PL + (pts.length === 1 ? 0.5 : i / (pts.length - 1)) * (W - PL - PR);
  // Allure : moins de secondes = plus rapide = plus haut.
  const y = (v) => PT + ((v - min) / (max - min)) * (H - PT - PB);
  const chemin = (cle) => pts.map((p, i) => (p[cle] == null ? null : `${x(i).toFixed(1)},${y(p[cle]).toFixed(1)}`))
    .filter(Boolean).map((c, i) => (i ? "L" : "M") + c).join("");
  const graduations = [min + 5, (min + max) / 2, max - 5];
  return html`
    <svg class="chart" viewBox="${`0 0 ${W} ${H}`}" role="img" aria-label="Courbe de forme">
      ${graduations.map((g) => html`<line class="grid" x1="${PL}" x2="${W - PR}" y1="${y(g)}" y2="${y(g)}"/>
        <text x="${PL - 4}" y="${y(g) + 3}" text-anchor="end">${allure(g).replace(" /km", "")}</text>`)}
      <path class="raw" d="${chemin("paceSecPerKm")}"/>
      <path class="corr" d="${chemin("correctedPaceSecPerKm")}"/>
      ${pts.map((p, i) => (p.correctedPaceSecPerKm != null ? html`<circle class="dot" cx="${x(i)}" cy="${y(p.correctedPaceSecPerKm)}" r="3"/>` : ""))}
      <text x="${PL}" y="${H - 6}">${jourMois(pts[0].dateIso)}</text>
      <text x="${W - PR}" y="${H - 6}" text-anchor="end">${jourMois(pts[pts.length - 1].dateIso)}</text>
    </svg>`;
}

function forme(ctx) {
  const pts = ctx.ecran.formePoints || [];
  const label = `${ctx.dist} km`;
  const pr = ctx.ecran.pronostics;
  return html`
    <section class="card">
      <h3>Forme ${ctx.saison} • ${label}</h3>
      ${pts.length === 0 ? html`<p class="muted">Aucune course courue sur le ${label} cette saison.</p>` : html`
        <div class="kpis" style="grid-template-columns:1fr 1fr">
          <div class="kpi"><div class="l">forme actuelle</div><div class="v">${allure(ctx.ecran.formeAllure)}</div><div class="s">allure ramenée à un parcours moyen de la saison</div></div>
          <div class="kpi"><div class="l">courses</div><div class="v">${pts.length}</div><div class="s">${jourMois(pts[0].dateIso)} → ${jourMois(pts[pts.length - 1].dateIso)}</div></div>
        </div>
        ${pts.length > 1 ? html`
          <div style="margin-top:10px">${courbeForme(pts)}</div>
          <div class="legend"><span><i></i>allure corrigée du parcours</span><span><i class="raw"></i>allure brute</span></div>`
          : html`<p class="small muted">Une seule course courue : la courbe apparaîtra dès la deuxième.</p>`}
        <div class="rows" style="margin-top:8px">
          ${pts.slice().reverse().map((p) => html`
            <div class="row tap" data-act="fiche" data-race="${p.raceId}" style="cursor:pointer">
              <span class="k small">M#${p.roundNumber} ${p.village}<br><span class="label">${dateCourte(p.dateIso)} • ${p.timeFormatted} • ${allure(p.paceSecPerKm)}</span></span>
              <span class="v small">${p.correctedPaceSecPerKm != null ? allure(p.correctedPaceSecPerKm) : "—"}<br><span class="label muted">${p.factor != null ? indice(p.factor) : "parcours sans indice"}</span></span>
            </div>`)}
        </div>
        <p class="small muted" style="margin:8px 0 0">Plus haut = plus rapide. L'indice d'un parcours compare l'allure des coureurs réguliers ce jour-là à leur allure habituelle : il tient compte du dénivelé, du terrain et de la météo.</p>`}
    </section>
    ${pr?.hasAssignedBib ? pronostics(pr) : ""}`;
}

function pronostics(pr) {
  const verdicts = pr.completedWithVerdict || [];
  const avenir = pr.upcomingPronostics || [];
  return html`
    <section class="card">
      <h3>Pronostics ${pr.distanceLabel}</h3>
      <div class="rows">
        <div class="row"><span class="k">Allure récente</span><span class="v">${pr.recentAveragePaceFormatted} (${dec(pr.recentAverageSpeedKmh)} km/h)</span></div>
        ${(pr.recentReferenceRaces || []).map((r) => html`<div class="row"><span class="k small">M#${r.roundNumber} ${r.villageName} (${jourMois(r.dateIso)})</span><span class="v small">${r.officialTimeFormatted} • ${r.paceUsedFormatted}</span></div>`)}
      </div>
      ${avenir.length ? html`
        <div class="label" style="font-weight:700;margin:10px 0 4px">Prochaines manches</div>
        <div class="rows">${avenir.map((e) => html`<div class="row tap" data-act="fiche" data-race="${e.raceId}" style="cursor:pointer">
          <span class="k small">M#${e.roundNumber} ${e.villageName} • ${dateCourte(e.dateIso)}</span><span class="v small">~${e.predictedTimeFormatted} (${e.predictedPaceFormatted})</span></div>`)}</div>` : ""}
      ${verdicts.length ? html`
        <div class="label" style="font-weight:700;margin:10px 0 4px">Prévu vs réel • ${pr.betterThanExpectedCount} mieux • ${pr.onTargetCount} dans le mille • ${pr.slowerThanExpectedCount} plus lent</div>
        <div class="rows">${verdicts.map((e) => html`<div class="row tap" data-act="fiche" data-race="${e.raceId}" style="cursor:pointer">
          <span class="k small">M#${e.roundNumber} ${e.villageName}<br><span class="label">prévu ${e.predictedTimeFormatted} • réel ${e.actualTimeFormatted}</span></span><span class="v small">${e.verdictShortBadge}<br><span class="label muted">${e.deltaFormatted || ""}</span></span></div>`)}</div>` : ""}
      ${!avenir.length && !verdicts.length ? html`<p class="small muted" style="margin:8px 0 0">Pas de manche à venir sur cette saison.</p>` : ""}
    </section>`;
}

function records(ctx) {
  const d = ctx.dist === 5 ? ctx.bilan.challenge5km : ctx.bilan.challenge10km;
  const rp = ctx.ecran.recordsPerso;
  const entrees = rp ? [rp.fastestRaceRecord, rp.bestPointsRecord, rp.bestOverallRankRecord, rp.bestCategoryRankRecord, rp.biggestElevationRecord, rp.regularityRecord].filter(Boolean) : [];
  const parcours = ctx.ecran.records?.records || [];
  const etiquette = { BEATEN: ["battu", "top15"], NOT_BEATEN: ["pas battu", "out"], TO_BEAT: ["à battre", "gold"], FIRST: ["1re fois", "round"] };
  return html`
    <section class="card">
      <h3>Saison ${ctx.saison} • ${d.distanceLabel}</h3>
      <div class="kpis">
        <div class="kpi"><div class="l">Allure (${d.distanceLabel})</div><div class="v">${d.averagePaceFormatted}</div><div class="s">${dec(d.averageSpeedKmh)} km/h moy.</div></div>
        <div class="kpi"><div class="l">Cumul (${d.distanceLabel})</div><div class="v">${dec(d.totalDistanceRunKm)} km</div><div class="s">${d.completedRacesCount} course(s)</div></div>
        <div class="kpi"><div class="l">Dénivelé (${d.distanceLabel})</div><div class="v">+${d.totalElevationGainMeters} m</div><div class="s">${d.elevationKnownRacesCount} trace(s) GPX</div></div>
        <div class="kpi"><div class="l">Meilleure (${d.distanceLabel})</div><div class="v">${d.bestPoints} pts</div><div class="s">${d.bestRaceTitle || "0 course"}</div></div>
      </div>
    </section>
    <section class="card">
      <h3>Records personnels de Gaume</h3>
      ${entrees.length ? html`<div class="rows">${entrees.map((e) => html`
        <div class="row"><span class="k small"><b style="color:var(--on-surface)">${e.categoryTitle}</b><br>${e.villageAndRound} • ${dateCourte(e.dateIso)}</span>
          <span class="v small">${e.mainValue}<br><span class="label muted">${e.secondaryValue}</span></span></div>`)}</div>`
        : html`<p class="muted small">Aucun record enregistré sur le Challenge ${d.distanceLabel} pour le moment (0 course officielle courue sur cette distance).</p>`}
    </section>
    ${parcours.length ? html`
      <section class="card">
        <h3>Records de parcours (${d.distanceLabel})</h3>
        <div class="rows">${parcours.map((r) => {
          const [txt, cls] = etiquette[r.status] || ["", "out"];
          const dernier = r.editions[r.editions.length - 1];
          return html`<div class="row"><span class="k small"><b style="color:var(--on-surface)">${r.village}</b><br>${r.editions.map((e) => `${e.seasonYear} : ${e.timeFormatted}`).join(" • ")}</span>
            <span class="v"><span class="pill ${cls}">${txt}</span>${r.deltaSeconds != null && dernier ? html`<br><span class="label muted">${ecartCourt(r.deltaSeconds)}</span>` : ""}</span></div>`;
        })}</div>
      </section>` : ""}`;
}

function ligneRecap(r, profil) {
  return html`<div class="trio-line">${avatar(profil, 24)}<span class="n"><b>${prenom(r.profileName)}</b> <span class="muted small">(${r.distanceLabel})</span></span>
    <span class="r">${r.countingRaces}/15 • ${points(r.totalPoints)} pts${r.standing ? html`<br><span class="muted">${r.standing.overallRank}e / ${points(r.runnersInDistance)}</span>` : ""}</span></div>`;
}

function bilan(ctx) {
  const r = ctx.ecran.activeRecap;
  if (!r) return html`<div class="card muted">Pas de bilan pour cette saison.</div>`;
  const trend = r.formStartPaceSecPerKm != null && r.formEndPaceSecPerKm != null
    ? (r.formEndPaceSecPerKm < r.formStartPaceSecPerKm ? "en progrès" : r.formEndPaceSecPerKm > r.formStartPaceSecPerKm ? "en baisse" : "stable")
    : null;
  const lignes = [
    ["Challenge", r.challengeComplete ? `Validé : ${r.countingRaces}/15 courses` : `${r.countingRaces}/15 courses comptées`],
    ["Courses", `${r.racesRun} course(s) courue(s) • ${points(r.totalPoints)} pts sur les 15 meilleures`],
    r.standing ? ["Classement provisoire", `${r.standing.overallRank}e / ${points(r.runnersInDistance)} • ${r.standing.categoryRank}e / ${points(r.runnersInCategory)} en catégorie ${r.standing.category}`] : null,
    r.bestPointsRace ? ["Meilleur score", `${r.bestPointsRace.points} pts à ${r.bestPointsRace.village}`] : null,
    r.fastestRace ? ["Allure la plus rapide", `${r.fastestRace.village} (${jourMois(r.fastestRace.dateIso)}) • ${r.fastestRace.timeFormatted} • ${allure(r.fastestRace.paceSecPerKm)}`] : null,
    r.bestCorrectedRace ? ["Meilleure course (parcours corrigé)", `${r.bestCorrectedRace.village} (${jourMois(r.bestCorrectedRace.dateIso)}) • ${allure(r.bestCorrectedRace.correctedPaceSecPerKm)}`] : null,
    ["Kilomètres de course", `${dec(r.totalKm)} km en ${duree(r.totalTimeSeconds)}`],
    ["Villages", `${r.villagesThisSeason} village(s) cette saison${r.newVillagesThisSeason ? `, dont ${r.newVillagesThisSeason} nouveau(x)` : ""}`],
    r.horsChallengeRaces ? ["Hors challenge", `${r.horsChallengeRaces} course(s), sans effet sur le challenge`] : null,
    trend ? ["Forme", `${allure(r.formStartPaceSecPerKm)} en début de saison → ${allure(r.formEndPaceSecPerKm)} en fin (${trend})`] : null
  ].filter(Boolean);
  const trioRecaps = ctx.ecran.trioRecaps || {};
  return html`
    <section class="card">
      <div class="label muted" style="font-weight:700">ALLURES LIBRES DE GAUME • BILAN ${r.seasonYear}</div>
      <h3 style="margin-top:4px">${r.profileName} • Challenge ${r.distanceLabel}${r.isSeasonOver ? " • saison terminée" : " • saison en cours"}</h3>
      ${r.challengeComplete ? html`<span class="pill top15" style="margin-bottom:6px">CHALLENGE VALIDÉ</span>` : ""}
      <div class="rows">${lignes.map(([k, v]) => html`<div class="row"><span class="k">${k}</span><span class="v small">${v}</span></div>`)}</div>
    </section>
    <section class="card">
      <h3>Bilan du Trio</h3>
      <div class="trio-results">${ctx.profils.map((p) => (trioRecaps[p.profileId] ? ligneRecap(trioRecaps[p.profileId], p) : ""))}</div>
    </section>`;
}

async function saisons(ctx) {
  const annees = ctx.meta.saisons.slice().sort();
  const bilans = await Promise.all(annees.map((y) => ctx.donnees.bilan(ctx.profil.profileId, y)));
  const cmp = ctx.ecran.sameCourseComparisons || [];
  const yA = ctx.ecran.comparisonYearA, yB = ctx.ecran.comparisonYearB;
  const avecResultat = cmp.filter((c) => c.resultSeasonA || c.resultSeasonB);
  return html`
    <section class="card">
      <h3>Progression saison par saison</h3>
      <div class="rows">${annees.map((y, i) => {
        const b = bilans[i];
        if (!b) return "";
        const d = ctx.dist === 5 ? b.challenge5km : b.challenge10km;
        return html`<div class="row"><span class="k"><b style="color:var(--on-surface)">Saison ${y} • ${d.distanceLabel}</b><br><span class="small">${d.countingRacesCount} / 15 courses • ${points(d.totalChallengePoints)} pts</span></span>
          <span class="v small">${d.completedRacesCount ? html`${d.averagePaceFormatted}<br><span class="label muted">${dec(d.totalDistanceRunKm)} km • ${d.averagePointsPerRace} pts moy.</span>` : html`<span class="muted">aucune course</span>`}</span></div>`;
      })}</div>
    </section>
    <section class="card">
      <h3>Mêmes parcours : ${yA} vs ${yB} (${ctx.dist} km)</h3>
      ${avecResultat.length === 0 ? html`<p class="muted small">Aucun parcours couru sur cette distance en ${yA} ou en ${yB}.</p>` : html`
        <div class="rows">${avecResultat.map((c) => html`
          <div class="row"><span class="k small"><b style="color:var(--on-surface)">${c.village}</b><br>${yA} : ${c.resultSeasonA?.officialTimeFormatted || "—"} • ${yB} : ${c.resultSeasonB?.officialTimeFormatted || "—"}</span>
            <span class="v small">${c.timeDeltaSeconds != null ? html`<span style="${raw(`color:${c.timeDeltaSeconds < 0 ? "var(--top15)" : "var(--clay)"}`)}">${ecartCourt(c.timeDeltaSeconds)}</span>` : html`<span class="muted">—</span>`}
              ${c.pointsDelta != null ? html`<br><span class="label muted">${c.pointsDelta > 0 ? "+" : ""}${c.pointsDelta} pts</span>` : ""}</span></div>`)}</div>`}
    </section>`;
}

export async function vue(ctx) {
  if (!ctx.bilan || !ctx.ecran) return html`<div class="card">Pas encore de données pour cette saison.</div>`;
  const sous = ctx.sousOnglet || 0;
  const libelles = SOUS_ONGLETS;
  let contenu;
  if (sous === 0) contenu = forme(ctx);
  else if (sous === 1) contenu = records(ctx);
  else if (sous === 2) contenu = bilan(ctx);
  else contenu = await saisons(ctx);
  return html`
    <div class="stack">
      ${enTete(ctx)}
      <div class="segmented">${libelles.map((l, i) => html`<button class="chip" type="button" data-act="sous-onglet" data-v="${i}" aria-pressed="${i === sous}">${l}</button>`)}</div>
      ${contenu}
    </div>`;
}
