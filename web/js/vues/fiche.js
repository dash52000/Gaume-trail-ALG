// Fiche d'une course (panneau qui monte du bas) : le Trio sur la course, record, conditions, infos pratiques.
import { html, raw, icon, avatar, render } from "../ui.js";
import { dateLongue, dec, ecart, indice, indicePhrase, meteo, prenom, points, cleVillage, allure } from "../fmt.js";
import { donnees } from "../donnees.js";

const root = () => document.getElementById("sheet-root");

function fermer() {
  render(root(), "");
  document.body.style.overflow = "";
}

document.addEventListener("keydown", (e) => { if (e.key === "Escape") fermer(); });
document.addEventListener("click", (e) => {
  if (e.target.closest("[data-fermer]")) fermer();
});

function trouverCourse(raceId, ctx) {
  const items = [...(ctx.bilan?.racesWithContext || []), ...(ctx.ecran?.horsChallengeItems || [])];
  return items.find((i) => i.race.raceId === raceId);
}

function ligneResultat(p, r) {
  return html`
    <div class="trio-line fiche">
      ${avatar(p, 28)}
      <div class="n"><b>${prenom(p.displayName)}</b> <span class="muted small">${r.distanceName || r.distanceLabel}${r.bibNumber ? ` • #${r.bibNumber}` : ""}</span>
        <div class="label muted">${r.overallRank}e/${r.totalFinishers} • cat. ${r.categoryRank}e/${r.categoryTotal || "?"}${r.challengePoints ? ` • ${r.challengePoints} pts` : ""}</div></div>
      <div class="r"><b style="font-size:14px">${r.officialTimeFormatted}</b><br><span class="muted">${r.pacePerKmFormatted}</span></div>
    </div>`;
}

function blocTrio(item, ctx) {
  const parProfil = item.resultsByProfileAndDistance || {};
  return html`
    <section class="card">
      <h3>Le Trio sur cette course</h3>
      <div class="trio-results">
        ${ctx.meta.profils.map((p) => {
          const res = parProfil[p.profileId] || [];
          if (!res.length) {
            return html`<div class="trio-line">${avatar(p, 26)}<span class="n"><b>${prenom(p.displayName)}</b></span>
              <span class="r muted">${item.race.isCompleted ? "Non courue" : "À venir"}</span></div>`;
          }
          return res.map((r) => ligneResultat(p, r));
        })}
      </div>
    </section>`;
}

function blocTop15(item, ctx) {
  if (!item.race.isChallenge) {
    return html`<div class="banner info small">Course hors challenge : le résultat ne compte jamais pour le challenge (ni Top 15, ni classement).</div>`;
  }
  const d = ctx.dist;
  const res = d === 5 ? item.result5km : item.result10km;
  if (!res) return "";
  const r = d === 5 ? item.rankAmongRunnerRaces5km : item.rankAmongRunnerRaces10km;
  const c = d === 5 ? item.isCountingInTop15For5km : item.isCountingInTop15For10km;
  const manque = d === 5 ? item.pointsDeltaToEnterTop15For5km : item.pointsDeltaToEnterTop15For10km;
  return html`
    <div class="banner ${c ? "gold" : "info"}">
      ${icon(c ? "check" : "trophy")}
      <div><strong>${c ? `Compte dans le Top 15 de ${prenom(ctx.profil.displayName)} (#${r}/15)` : `Hors Top 15 (#${r})`}</strong>
        <span class="small muted">${res.challengePoints} pts sur le ${res.distanceLabel}${!c && manque ? ` • il manquait ${manque} pts pour entrer dans le Top 15` : ""}</span></div>
    </div>`;
}

function blocRecord(record) {
  if (!record || !record.editions?.length) return "";
  let verdict = "";
  if (record.status === "BEATEN") verdict = html`<span class="pill top15">battu (${ecart(record.deltaSeconds || 0)})</span>`;
  else if (record.status === "NOT_BEATEN") verdict = html`<span class="pill out">pas battu (${ecart(record.deltaSeconds || 0)})</span>`;
  else if (record.status === "TO_BEAT") verdict = html`<span class="pill gold">à battre : ${record.best.timeFormatted}</span>`;
  return html`
    <section class="card">
      <div class="section-title" style="margin:0 0 6px"><h2>Record de parcours (${record.distanceLabel})</h2>${verdict}</div>
      <div class="rows">
        ${record.editions.map((e) => html`<div class="row"><span class="k">${e.seasonYear}</span>
          <span class="v">${e.timeFormatted} <span class="muted small">• ${allure(e.paceSecPerKm)}${e.raceId === record.best.raceId ? " • record" : ""}</span></span></div>`)}
      </div>
    </section>`;
}

function blocPronostic(entry) {
  if (!entry) return "";
  return html`
    <section class="card">
      <h3>Pronostic avant-course (${dec(entry.distanceKm)} km)</h3>
      <div class="rows">
        <div class="row"><span class="k">Temps prévu</span><span class="v">~${entry.predictedTimeFormatted} (${entry.predictedPaceFormatted})</span></div>
        ${entry.actualTimeFormatted ? html`
          <div class="row"><span class="k">Temps réel</span><span class="v">${entry.actualTimeFormatted} (${entry.actualPaceFormatted || ""})</span></div>
          <div class="row"><span class="k">Verdict</span><span class="v">${entry.verdictLabel}${entry.deltaFormatted ? ` (${entry.deltaFormatted})` : ""}</span></div>` : ""}
      </div>
    </section>`;
}

function blocConditions(item) {
  const w = item.weather;
  const race = item.race;
  let meteoTxt;
  if (w) {
    const parts = [];
    if (w.temperatureC != null) parts.push(`${dec(w.temperatureC, 0)} °C`);
    const lib = meteo(w.weatherCode);
    if (lib) parts.push(lib);
    if (w.windKmh != null) parts.push(`vent ${dec(w.windKmh, 0)} km/h`);
    if (w.precipitationDuringMm != null && w.precipitationDuringMm > 0) parts.push(`${dec(w.precipitationDuringMm)} mm pendant la course`);
    if (w.rainPrevious72hMm != null) parts.push(`${dec(w.rainPrevious72hMm)} mm les 3 jours avant`);
    meteoTxt = `${w.startHourLocal != null ? `${w.startHourLocal} h : ` : ""}${parts.join(" • ")}`;
  } else if (race.startLatitude == null) {
    meteoTxt = "Lieu de départ non publié : météo indisponible";
  } else if (!race.isCompleted) {
    meteoTxt = "Disponible après la course";
  } else {
    meteoTxt = "Récupérée pour les courses courues par le Trio";
  }
  const parcours = (label, f, prev) => {
    let t;
    if (f != null) t = indicePhrase(f) + (prev != null ? ` (édition précédente : ${indice(prev)})` : "");
    else if (prev != null) t = `Édition précédente : ${indice(prev)}`;
    else t = "Indice inconnu (pas assez de coureurs réguliers, ou course pas encore courue)";
    return html`<div class="row"><span class="k">Parcours (${label})</span><span class="v small">${t}</span></div>`;
  };
  const terrain = item.terrainWeather?.terrainCondition;
  return html`
    <section class="card">
      <h3>Conditions</h3>
      <div class="rows">
        <div class="row"><span class="k">Météo au départ</span><span class="v small">${meteoTxt}${w?.source ? html`<br><span class="muted">${w.source}</span>` : ""}</span></div>
        ${race.isChallenge ? parcours("10 km", item.courseFactor10km, item.previousEditionFactor10km) : ""}
        ${race.isChallenge ? parcours("5 km", item.courseFactor5km, item.previousEditionFactor5km) : ""}
        ${terrain ? html`<div class="row"><span class="k">Terrain noté</span><span class="v">${terrain}</span></div>` : ""}
      </div>
    </section>`;
}

function blocInfos(item) {
  const r = item.race;
  const lignes = [];
  if (r.address) lignes.push(["Lieu", r.address]);
  if (r.schedule) lignes.push(["Départ", r.schedule]);
  if (r.kidsSchedule) lignes.push(["Jeunes", r.kidsSchedule]);
  if (r.terrainDescription) lignes.push(["Terrain", r.terrainDescription]);
  const dists = r.isChallenge
    ? [r.distanceKm ? `grande ${dec(r.distanceKm)} km` : null, r.distance5Km ? `petite ${dec(r.distance5Km)} km` : null]
    : [r.longDistanceLabel, r.shortDistanceLabel];
  if (dists.some(Boolean)) lignes.push(["Distances", dists.filter(Boolean).join(" • ")]);
  const surPlace = [["Garderie", r.hasGarderie], ["Douches", r.hasDouche], ["Vestiaires", r.hasVestiaire], ["Resto", r.hasResto]]
    .filter(([, v]) => v === true).map(([k]) => k);
  if (surPlace.length) lignes.push(["Sur place", surPlace.join(" • ")]);
  const traces = (r.routeLinks || "").split("\n").map((l) => l.split("\t")).filter((p) => p.length === 2 && p[1].startsWith("http"));
  const lat = r.startLatitude ?? item.gaumeGeo.latitude;
  const lon = r.startLongitude ?? item.gaumeGeo.longitude;
  return html`
    <section class="card">
      <h3>Infos pratiques</h3>
      ${lignes.length ? html`<div class="rows">${lignes.map(([k, v]) => html`<div class="row"><span class="k">${k}</span><span class="v small">${v}</span></div>`)}</div>`
        : html`<p class="muted small">MyChrono ne publie pas encore d'infos pratiques pour cette manche.</p>`}
      <div class="chips wrap" style="margin-top:10px">
        ${traces.map(([l, u]) => html`<a class="btn" href="${u}" target="_blank" rel="noopener">${icon("route")} Tracé ${l}</a>`)}
        ${r.sourceUrl ? html`<a class="btn" href="${r.sourceUrl}" target="_blank" rel="noopener">${icon("link")} Page officielle</a>` : ""}
        ${lat != null && lon != null ? html`<a class="btn" href="${`https://www.google.com/maps/dir/?api=1&destination=${lat},${lon}`}" target="_blank" rel="noopener">${icon("map")} ${r.startLatitude != null ? "Itinéraire jusqu'au départ" : "Itinéraire jusqu'au village"}</a>` : ""}
      </div>
    </section>`;
}

function blocVillage(item, villages) {
  const v = villages[cleVillage(item.race.village)];
  if (!v?.found || !v.extract) return "";
  return html`
    <section class="card">
      <h3>${v.title || item.race.village}</h3>
      <p class="small" style="margin:0">${v.extract}</p>
      ${v.pageUrl ? html`<p class="label" style="margin:6px 0 0"><a href="${v.pageUrl}" target="_blank" rel="noopener">Wikipédia</a></p>` : ""}
    </section>`;
}

/** Profil d'altitude de la trace GPX (« lat,lon,ele;… »), dessiné en SVG. */
function profilAltitude(pointsTexte) {
  const pts = pointsTexte.split(";").map((p) => p.split(",").map(Number)).filter((p) => p.length >= 3 && p.every((x) => !isNaN(x)));
  if (pts.length < 2) return "";
  const R = 6371000;
  let d = 0;
  const serie = [{ d: 0, e: pts[0][2] }];
  for (let i = 1; i < pts.length; i++) {
    const [la1, lo1] = pts[i - 1], [la2, lo2] = pts[i];
    const dla = (la2 - la1) * Math.PI / 180, dlo = (lo2 - lo1) * Math.PI / 180;
    const a = Math.sin(dla / 2) ** 2 + Math.cos(la1 * Math.PI / 180) * Math.cos(la2 * Math.PI / 180) * Math.sin(dlo / 2) ** 2;
    d += 2 * R * Math.asin(Math.sqrt(a));
    serie.push({ d, e: pts[i][2] });
  }
  const W = 320, H = 110, P = 22;
  const emin = Math.min(...serie.map((s) => s.e)), emax = Math.max(...serie.map((s) => s.e));
  const x = (v) => P + (v / d) * (W - P - 4);
  const y = (v) => H - 16 - ((v - emin) / Math.max(1, emax - emin)) * (H - 30);
  const ligne = serie.map((s, i) => `${i ? "L" : "M"}${x(s.d).toFixed(1)},${y(s.e).toFixed(1)}`).join("");
  return html`
    <svg class="chart" viewBox="${`0 0 ${W} ${H}`}" role="img" aria-label="Profil d'altitude">
      <path d="${ligne + `L${x(d).toFixed(1)},${H - 16}L${x(0)},${H - 16}Z`}" fill="var(--primary-container)" opacity=".7"/>
      <path d="${ligne}" class="corr"/>
      <text x="${P}" y="10">${Math.round(emax)} m</text>
      <text x="${P}" y="${H - 18}">${Math.round(emin)} m</text>
      <text x="${W - 4}" y="${H - 3}" text-anchor="end">${dec(d / 1000)} km</text>
    </svg>`;
}

export async function ouvrirFiche(raceId, ctx) {
  const item = trouverCourse(raceId, ctx);
  if (!item) return;
  const race = item.race;
  const record = ctx.ecran?.records?.byRaceId?.[raceId];
  const prono = ctx.ecran?.pronostics?.allPronosticsByRaceId?.[raceId];
  const contenu = html`
    <div class="sheet-backdrop" data-fermer></div>
    <div class="sheet" role="dialog" aria-modal="true" aria-label="${race.title}">
      <div class="sheet-bar"><span class="grip"></span>
        <button class="icon-btn" type="button" data-fermer aria-label="Fermer">${icon("close")}</button></div>
      <div class="sheet-head">
        <span class="pill ${race.isChallenge ? "round" : "hc"}">${race.isChallenge ? `Manche ${race.roundNumber}` : "Hors challenge"}</span>
        <h2>${race.title}</h2>
        <div class="muted small">${item.gaumeGeo.canonicalVillage} • ${item.gaumeGeo.territoryName} • ${dateLongue(race.dateIso)}${race.finishersCount ? ` • ${points(race.finishersCount)} arrivants` : ""}</div>
      </div>
      <div class="stack">
        ${blocTop15(item, ctx)}
        ${blocTrio(item, ctx)}
        ${blocRecord(record)}
        ${blocPronostic(prono)}
        <div id="trace-zone"></div>
        ${blocConditions(item)}
        ${blocInfos(item)}
        ${blocVillage(item, ctx.villages)}
      </div>
    </div>`;
  render(root(), contenu);
  document.body.style.overflow = "hidden";

  if (item.activityMetrics?.hasTrack) {
    const t = await donnees.trace(raceId, item.activityMetrics.profileId);
    const zone = document.getElementById("trace-zone");
    if (t && zone) {
      const m = item.activityMetrics;
      render(zone, html`<section class="card"><h3>Trace GPX de ${prenom(ctx.profil.displayName)}</h3>
        ${raw(String(profilAltitude(t.points)))}
        <p class="small muted" style="margin:4px 0 0">${dec(m.recordedDistanceKm)} km enregistrés${m.elevationGainMeters != null ? ` • +${m.elevationGainMeters} m D+` : ""}${m.avgHeartRateBpm ? ` • FC moy. ${m.avgHeartRateBpm}` : ""}</p></section>`);
    }
  }
}
