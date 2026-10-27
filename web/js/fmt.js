// Mises en forme, reprises de l'appli Android (mêmes textes).

const dateFmt = new Intl.DateTimeFormat("fr-FR", { weekday: "short", day: "numeric", month: "short" });
const dateFmtYear = new Intl.DateTimeFormat("fr-FR", { day: "numeric", month: "short", year: "numeric" });

/** « 2026-10-18 » → « dim. 18 oct. » */
export function dateCourte(iso) {
  if (!iso) return "";
  const d = new Date(iso + "T12:00:00");
  return isNaN(d) ? iso : dateFmt.format(d);
}

/** « 2026-10-18 » → « 18 oct. 2026 » */
export function dateLongue(iso) {
  if (!iso) return "";
  const d = new Date(iso + "T12:00:00");
  return isNaN(d) ? iso : dateFmtYear.format(d);
}

/** « 2026-10-18 » → « 18/10 » */
export function jourMois(iso) {
  return iso ? iso.substring(8, 10) + "/" + iso.substring(5, 7) : "";
}

/** 17056 → « 17 056 » */
export function points(n) {
  return n == null ? "—" : Number(n).toLocaleString("fr-FR").replace(/ | /g, " ");
}

/** Allure en s/km → « 5:07 /km » */
export function allure(sec) {
  if (sec == null) return "--:-- /km";
  const s = Math.round(sec);
  return `${Math.floor(s / 60)}:${String(s % 60).padStart(2, "0")} /km`;
}

/** Écart de temps signé : « −1:12 », « +0:45 », « +1:02:03 » (comme GoalsCalculator.formatDelta). */
export function ecart(sec) {
  const sign = sec < 0 ? "−" : "+";
  const a = Math.abs(sec);
  if (a >= 3600) return `${sign}${Math.floor(a / 3600)}:${String(Math.floor((a % 3600) / 60)).padStart(2, "0")}:${String(a % 60).padStart(2, "0")}`;
  return `${sign}${Math.floor(a / 60)}:${String(a % 60).padStart(2, "0")}`;
}

/** Écart court : « -1m12s », « +45s » (comme formatSignedTimeDelta). */
export function ecartCourt(sec) {
  const sign = sec < 0 ? "-" : "+";
  const a = Math.abs(sec);
  const m = Math.floor(a / 60);
  const s = a % 60;
  return m > 0 ? `${sign}${m}m${String(s).padStart(2, "0")}s` : `${sign}${s}s`;
}

/** Durée totale : « 13 h 36 » ou « 45 min » */
export function duree(sec) {
  const h = Math.floor(sec / 3600);
  const m = Math.floor((sec % 3600) / 60);
  return h > 0 ? `${h} h ${String(m).padStart(2, "0")}` : `${m} min`;
}

/** Nombre décimal à la française : 10.6 → « 10,6 » */
export function dec(n, digits = 1) {
  if (n == null || isNaN(n)) return "—";
  return Number(n).toLocaleString("fr-FR", { minimumFractionDigits: digits, maximumFractionDigits: digits });
}

/** Indice de difficulté : 1.08 → « +8 % plus lent » (comme CourseDifficultyCalculator.formatFactor). */
export function indice(f) {
  const pct = Math.round((f - 1) * 100);
  if (pct === 0) return "parcours moyen";
  return pct > 0 ? `+${pct} % plus lent` : `${-pct} % plus rapide`;
}

export function indicePhrase(f) {
  const pct = Math.round((f - 1) * 100);
  if (pct === 0) return "Dans la moyenne des parcours de la saison";
  return pct > 0 ? `${pct} % plus lent qu'un parcours moyen de la saison` : `${-pct} % plus rapide qu'un parcours moyen de la saison`;
}

export function dPlus(m) {
  return m != null ? `+${m} m D+` : "D+ inconnu";
}

/** Libellé météo Open-Meteo (comme OpenMeteoClient.weatherLabel). */
export function meteo(code) {
  if (code == null) return null;
  if (code === 0) return "Ciel clair";
  if (code === 1 || code === 2) return "Éclaircies";
  if (code === 3) return "Couvert";
  if (code === 45 || code === 48) return "Brouillard";
  if ([51, 53, 55, 56, 57].includes(code)) return "Bruine";
  if ([61, 63, 65, 66, 67].includes(code)) return "Pluie";
  if ([71, 73, 75, 77, 85, 86].includes(code)) return "Neige";
  if ([80, 81, 82].includes(code)) return "Averses";
  if ([95, 96, 99].includes(code)) return "Orage";
  return null;
}

/** Couleur d'un profil (avatarColorHex, entier ARGB) → « #1b5e3a ». */
export function couleur(argb) {
  if (argb == null) return "#1B5E3A";
  return "#" + (Number(argb) & 0xffffff).toString(16).padStart(6, "0");
}

export function prenom(nom) {
  return (nom || "").split(" ")[0];
}

export function initiale(nom) {
  return (nom || "?").trim().charAt(0).toUpperCase();
}

/** Clé d'un village, comme GaumeInitialSeedData.normalizeKey (« Mussy-la-Ville » → « mussy la ville »). */
export function cleVillage(texte) {
  return (texte || "").toLowerCase().trim().normalize("NFD").replace(/[̀-ͯ]/g, "")
    .replace(/['’\-()]/g, " ").replace(/\s+/g, " ").trim();
}

/** Ordinal court : 1 → « 1er », 2 → « 2e » */
export function rang(n) {
  return n == null ? "—" : n === 1 ? "1er" : `${n}e`;
}
