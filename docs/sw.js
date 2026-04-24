const CACHE_NAME = "kquiz-web-v12";
const CORE_ASSETS = [
  "./",
  "./index.html",
  "./styles.css",
  "./app.js",
  "./modules/app-shell.js",
  "./modules/ads.js",
  "./manifest.webmanifest",
  "./ads.txt",
  "./assets/icon.svg",
  "./assets/sfx/sfx_complete.mp3",
  "./assets/sfx/sfx_correct.mp3",
  "./assets/sfx/sfx_flip.mp3",
  "./assets/sfx/sfx_select.mp3",
  "./assets/sfx/sfx_wrong.mp3",
  "./THIRD_PARTY_NOTICES.md"
];

self.addEventListener("install", (event) => {
  event.waitUntil(caches.open(CACHE_NAME).then((cache) => cache.addAll(CORE_ASSETS)));
  self.skipWaiting();
});

self.addEventListener("activate", (event) => {
  event.waitUntil(
    caches.keys().then((keys) =>
      Promise.all(keys.filter((key) => key !== CACHE_NAME).map((key) => caches.delete(key)))
    )
  );
  self.clients.claim();
});

self.addEventListener("fetch", (event) => {
  const url = new URL(event.request.url);
  if (event.request.method !== "GET") return;
  if (url.origin !== self.location.origin) return;

  event.respondWith(
    caches.match(event.request, { ignoreSearch: true }).then((cached) => {
      if (cached) return cached;
      return fetch(event.request).then((response) => {
        const copy = response.clone();
        caches.open(CACHE_NAME).then((cache) => cache.put(event.request, copy));
        return response;
      });
    })
  );
});
