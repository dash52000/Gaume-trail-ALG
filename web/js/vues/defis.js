// Onglet « Défis » : records de parcours, badges, défis du Trio, passeport des villages, objectifs
// (comme ChallengesContent de l'appli Android). Consultation seule : les objectifs se fixent dans l'appli Android.
import { html, raw, icon } from "../ui.js";
import { ecart, jourMois, dateCourte, prenom, cleVillage } from "../fmt.js";

const STATUTS = {
  BEATEN: ["Record battu", "top15"],
  NOT_BEATEN: ["Pas battu", "bad"],
  TO_BEAT: ["À battre", "gold"],
  FIRST: ["1re édition", "out"]
};

function barre(progress, target, fait) {
  const pct = target > 0 ? Math.min(1, Math.max(0, progress / target)) * 100 : 0;
  return html`<div class="progress ${fait ? "complete" : ""}"><div style="${raw(`width:${pct}%`)}"></div></div>`;
}

/** Ouvre la fiche seulement si la course est dans la saison affichée (sinon la fiche n'a pas ses données). */
function lienFiche(raceId, ctx) {
  return raceId && raceId.startsWith(`alg_${ctx.saison}_`) ? raw(`data-act="fiche" data-race="${raceId}"`) : "";
}

function records(ctx) {
  const rs = ctx.ecran.records?.records || [];
  const n = (s) => rs.filter((r) => r.status === s).length;
  return html`
    <section class="card">
      <h3>Records de parcours • ${ctx.saison}</h3>
      <p class="small muted" style="margin:-2px 0 8px">Chaque parcours déjà couru devient un duel contre toi-même : bats ta meilleure édition. Même longueur : au temps ; longueur changée : à l'allure.</p>
      <div class="chips">${[10, 5].map((d) => html`<button class="chip" type="button" data-act="dist" data-v="${d}" aria-pressed="${ctx.dist === d}">${d} km</button>`)}</div>
      ${rs.length ? html`<div class="label" style="font-weight:700;margin-top:8px">${n("BEATEN")} battu(s) • ${n("NOT_BEATEN")} pas battu(s) • ${n("TO_BEAT")} à battre</div>` : ""}
    </section>
    ${rs.length === 0 ? html`<div class="card muted" style="text-align:center">Aucun parcours couru sur cette distance.</div>` : rs.map((r) => {
      const [txt, cls] = STATUTS[r.status] || ["", "out"];
      const cible = r.upcomingRaceId || (r.editions.find((e) => e.seasonYear === ctx.saison) || r.editions[r.editions.length - 1])?.raceId;
      const lien = lienFiche(cible, ctx);
      return html`
        <article class="card record ${cls} ${lien ? "tap" : ""}" ${lien}>
          <div class="race-head">
            <div class="titles">
              <div class="title">${r.village}</div>
              <div class="sub">${r.title.includes("— ") ? r.title.split("— ").slice(1).join("— ") : r.title}</div>
            </div>
            <span class="pill ${cls === "bad" ? "clay" : cls}">${txt}</span>
          </div>
          <div style="font-weight:600;margin-top:4px">${r.editions.map((e) => `${e.seasonYear} : ${e.timeFormatted}${e.raceId === r.best.raceId && r.editions.length > 1 ? " ★" : ""}`).join("  •  ")}</div>
          ${r.deltaSeconds != null ? html`<div class="small" style="${raw(`color:${r.deltaSeconds < 0 ? "var(--top15)" : "var(--clay)"}`)}">${ecart(r.deltaSeconds)} sur la référence ${r.bestBefore?.seasonYear ?? ""}</div>` : ""}
          ${r.status === "TO_BEAT" && r.upcomingDateIso ? html`<div class="small" style="color:var(--gold)">Le ${jourMois(r.upcomingDateIso)} : ${r.best.timeFormatted} à battre (${r.best.seasonYear})</div>` : ""}
        </article>`;
    })}`;
}

function badges(ctx) {
  const liste = ctx.ecran.badges || [];
  const parCat = new Map();
  liste.forEach((b) => { if (!parCat.has(b.category)) parCat.set(b.category, []); parCat.get(b.category).push(b); });
  return html`
    <section class="card">
      <h3>Badges • ${prenom(ctx.profil.displayName)}</h3>
      <p class="small muted" style="margin:-2px 0 0">Tous viennent de faits vérifiables : résultats MyChrono, météo réelle au départ, indice des parcours, calendrier.</p>
    </section>
    ${[...parCat.entries()].map(([cat, bs]) => html`
      <div class="section-title" style="margin:4px 0 -2px"><h2>${cat}</h2><span class="mono">${bs.filter((b) => b.unlocked).length}/${bs.length}</span></div>
      ${bs.slice().sort((a, b) => Number(b.unlocked) - Number(a.unlocked)).map((b) => html`
        <article class="card badge ${b.unlocked ? "unlocked" : ""}">
          <span class="badge-ic">${icon(b.unlocked ? "check" : "lock")}</span>
          <div style="min-width:0;flex:1">
            <div style="font-weight:700">${b.title}</div>
            <div class="small muted">${b.description}</div>
            ${b.detail ? html`<div class="label" style="font-weight:600;margin-top:2px">${b.detail}</div>` : ""}
            ${!b.unlocked && b.target > 1 ? html`<div style="margin-top:6px">${barre(b.progress, b.target, false)}</div><div class="label muted" style="margin-top:2px">${b.progress} / ${b.target}</div>` : ""}
          </div>
        </article>`)}`)}`;
}

function trio(ctx) {
  const defis = ctx.ecran.trioChallenges || [];
  return html`
    <section class="card">
      <h3>Défis du Trio • ${ctx.saison}</h3>
      <p class="small muted" style="margin:-2px 0 0">À trois, pas les uns contre les autres.</p>
    </section>
    ${defis.map((c) => html`
      <section class="card">
        <div class="race-head" style="align-items:flex-end">
          <div class="titles"><div class="title">${c.title}</div></div>
          <b style="color:var(--primary);white-space:nowrap">${c.target != null ? `${c.progress} / ${c.target} ${c.unit}` : `${c.progress} ${c.unit}`}</b>
        </div>
        <div class="small muted" style="margin:2px 0 6px">${c.description}</div>
        ${c.target != null ? barre(c.progress, c.target, c.progress >= c.target) : ""}
        ${c.detail ? html`<div class="label" style="margin-top:6px">${c.detail}</div>` : ""}
      </section>`)}`;
}

function villages(ctx) {
  const p = ctx.ecran.activePassport;
  if (!p) return html`<div class="card muted">Pas de passeport pour cette saison.</div>`;
  const total = p.visited.reduce((s, v) => s + v.visits.length, 0);
  return html`
    <section class="card">
      <h3>Passeport des villages • ${ctx.profil.displayName}</h3>
      <p class="small muted" style="margin:-2px 0 0">${p.visited.length} village(s) • ${total} course(s), toutes saisons et courses hors challenge comprises</p>
      ${p.notYetVisited.length ? html`
        <hr class="soft">
        <div style="font-weight:700" class="small">Jamais couru (calendrier ${p.latestCalendarSeason ?? ""}) : ${p.notYetVisited.length}</div>
        <div class="small muted">${p.notYetVisited.join(" • ")}</div>` : ""}
      <p class="label muted" style="margin:8px 0 0">La carte des villages est dans l'onglet Trio & Carte.</p>
    </section>
    ${p.visited.length === 0 ? html`<div class="card muted">Aucune course courue pour l'instant.</div>` : p.visited.map((v) => {
      const depuis = Math.min(...v.visits.map((x) => x.seasonYear));
      const info = ctx.villages[cleVillage(v.village)];
      return html`
        <details class="card village">
          <summary class="race-head">
            <span class="pin">${icon("pin")}</span>
            <div class="titles"><div class="title">${v.village}</div><div class="sub">${v.territoryName} • depuis ${depuis}</div></div>
            <span class="pill round">×${v.visits.length}</span>
          </summary>
          <div class="rows" style="margin-top:6px">
            ${v.visits.map((x) => {
              const lien = lienFiche(x.raceId, ctx);
              return html`<div class="row ${lien ? "tap" : ""}" ${lien}>
                <span class="k small">${dateCourte(x.dateIso)} ${x.seasonYear}<br><span class="label">${x.raceTitle.includes("— ") ? x.raceTitle.split("— ").slice(1).join("— ") : x.raceTitle}</span></span>
                <span class="v small">${x.timeFormatted}<br><span class="label muted">${x.distanceName}${x.isChallenge ? "" : " • hors challenge"}</span></span></div>`;
            })}
          </div>
          ${info?.found && info.extract ? html`<p class="small" style="margin:8px 0 0">${info.extract}</p>
            ${info.pageUrl ? html`<p class="label" style="margin:4px 0 0"><a href="${info.pageUrl}" target="_blank" rel="noopener">Source : Wikipédia (« ${info.title} »)</a></p>` : ""}`
          : html`<p class="label muted" style="margin:8px 0 0">Pas de fiche Wikipédia fiable pour ce village.</p>`}
        </details>`;
    })}`;
}

function objectifs(ctx) {
  const goals = ctx.ecran.goals || [];
  const saison = ctx.ecran.goalSeason;
  return html`
    <section class="card">
      <h3>Objectifs ${saison}</h3>
      <p class="small muted" style="margin:-2px 0 0">Ceux que ${prenom(ctx.profil.displayName)} choisit, rien d'imposé. Ils se fixent dans l'appli Android.</p>
    </section>
    ${goals.map((g) => {
      const fait = g.target != null && g.progress >= g.target;
      return html`
        <section class="card">
          <div class="race-head">
            <div class="titles"><div class="title">${g.title}</div><div class="sub" style="white-space:normal">${g.description}</div></div>
            <b style="${raw(`color:${fait ? "var(--top15)" : "var(--primary)"};white-space:nowrap`)}">${g.target != null ? `${g.progress} / ${g.target}` : g.progress}</b>
          </div>
          ${g.target != null ? html`<div style="margin-top:8px">${barre(g.progress, g.target, fait)}</div>`
            : html`<div class="label muted" style="margin-top:6px">Pas d'objectif fixé (proposé : ${g.suggested} ${g.unit})</div>`}
        </section>`;
    })}`;
}

export async function vue(ctx) {
  if (!ctx.bilan || !ctx.ecran) return html`<div class="card">Pas encore de données pour cette saison.</div>`;
  const e = ctx.ecran;
  const battus = (e.records?.records || []).filter((r) => r.status === "BEATEN").length;
  const bs = e.badges || [];
  const onglets = [
    `Records (${battus})`,
    `Badges (${bs.filter((b) => b.unlocked).length}/${bs.length})`,
    "Trio",
    `Villages (${e.activePassport?.visited?.length ?? 0})`,
    `Objectifs ${e.goalSeason ?? ""}`
  ];
  const sous = ctx.sousOnglet || 0;
  const vues = [records, badges, trio, villages, objectifs];
  return html`
    <div class="stack">
      <div class="chips">${onglets.map((l, i) => html`<button class="chip" type="button" data-act="sous-onglet" data-v="${i}" aria-pressed="${i === sous}">${l}</button>`)}</div>
      ${(vues[sous] || records)(ctx)}
    </div>`;
}
