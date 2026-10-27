// Service worker : l'appli s'ouvre même sans réseau (dernières données vues).
// - Fichiers de l'appli : servis depuis le cache, mis à jour en arrière-plan.
// - Données (data/) : réseau d'abord (résultats à jour), cache si hors ligne.
const VERSION = "alg-web-v3";
const SHELL = [
  "./",
  "index.html",
  "manifest.webmanifest",
  "css/app.css",
  "js/app.js",
  "js/fmt.js",
  "js/ui.js",
  "js/donnees.js",
  "js/vues/courses.js",
  "js/vues/fiche.js",
  "js/vues/perfs.js",
  "js/vues/trio.js",
  "js/vues/classement.js",
  "js/vues/defis.js",
  "img/foret-gaume.jpg",
  "img/icone-192.png",
  "img/favicon-32.png",
  "vendor/leaflet/leaflet.js",
  "vendor/leaflet/leaflet.css",
  "fonts/plus-jakarta-sans-latin-400-normal.woff2",
  "fonts/plus-jakarta-sans-latin-600-normal.woff2",
  "fonts/plus-jakarta-sans-latin-700-normal.woff2",
  "fonts/plus-jakarta-sans-latin-800-normal.woff2",
  "fonts/outfit-latin-700-normal.woff2",
  "fonts/jetbrains-mono-latin-400-normal.woff2",
  "fonts/jetbrains-mono-latin-700-normal.woff2"
];

self.addEventListener("install", (event) => {
  event.waitUntil(caches.open(VERSION).then((c) => c.addAll(SHELL)).then(() => self.skipWaiting()));
});

self.addEventListener("activate", (event) => {
  event.waitUntil(
    caches.keys()
      .then((keys) => Promise.all(keys.filter((k) => k !== VERSION).map((k) => caches.delete(k))))
      .then(() => self.clients.claim())
  );
});

self.addEventListener("fetch", (event) => {
  const req = event.request;
  if (req.method !== "GET") return;
  const url = new URL(req.url);
  if (url.origin !== self.location.origin) return; // fonds de carte : pas de cache

  if (url.pathname.includes("/data/")) {
    event.respondWith(
      fetch(req)
        .then((res) => {
          if (res.ok) {
            const copy = res.clone();
            caches.open(VERSION).then((c) => c.put(req, copy));
          }
          return res;
        })
        .catch(() => caches.match(req).then((hit) => hit || Response.error()))
    );
    return;
  }

  event.respondWith(
    caches.match(req).then((hit) => {
      const network = fetch(req)
        .then((res) => {
          if (res.ok) {
            const copy = res.clone();
            caches.open(VERSION).then((c) => c.put(req, copy));
          }
          return res;
        })
        .catch(() => hit);
      return hit || network;
    })
  );
});
