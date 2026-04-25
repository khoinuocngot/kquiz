const CACHE_NAME = "kquiz-web-v14";
const SHARE_CACHE = "kquiz-share-v1";
const SHARE_PAYLOAD_URL = new URL("./shared-payload", self.location.href).href;
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
      Promise.all(keys.filter((key) => key !== CACHE_NAME && key !== SHARE_CACHE).map((key) => caches.delete(key)))
    )
  );
  self.clients.claim();
});

async function fileToBase64(file) {
  const buffer = await file.arrayBuffer();
  const bytes = new Uint8Array(buffer);
  let binary = "";
  const chunkSize = 0x8000;
  for (let i = 0; i < bytes.length; i += chunkSize) {
    binary += String.fromCharCode(...bytes.subarray(i, i + chunkSize));
  }
  return btoa(binary);
}

self.addEventListener("fetch", (event) => {
  const url = new URL(event.request.url);
  if (url.origin === self.location.origin && url.pathname.endsWith("/share-target") && event.request.method === "POST") {
    event.respondWith((async () => {
      const form = await event.request.formData();
      const rawFiles = form.getAll("files").filter((item) => item && typeof item === "object" && "arrayBuffer" in item);
      const files = [];
      for (const file of rawFiles.slice(0, 3)) {
        files.push({
          name: file.name || "shared-file",
          type: file.type || "application/octet-stream",
          size: file.size || 0,
          base64: await fileToBase64(file)
        });
      }
      const payload = {
        title: form.get("title") || "",
        text: form.get("text") || "",
        url: form.get("url") || "",
        files,
        receivedAt: Date.now()
      };
      const cache = await caches.open(SHARE_CACHE);
      await cache.put(SHARE_PAYLOAD_URL, new Response(JSON.stringify(payload), { headers: { "Content-Type": "application/json" } }));
      return Response.redirect(new URL("./?share=1#/home", self.location.href), 303);
    })());
    return;
  }

  if (url.href === SHARE_PAYLOAD_URL && event.request.method === "GET") {
    event.respondWith((async () => {
      const cache = await caches.open(SHARE_CACHE);
      const response = await cache.match(SHARE_PAYLOAD_URL);
      if (response) await cache.delete(SHARE_PAYLOAD_URL);
      return response || new Response("{}", { headers: { "Content-Type": "application/json" } });
    })());
    return;
  }

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

self.addEventListener("notificationclick", (event) => {
  event.notification.close();
  const targetUrl = new URL(event.notification.data?.url || "./#/smart-review", self.location.href).href;
  event.waitUntil((async () => {
    const clientList = await self.clients.matchAll({ type: "window", includeUncontrolled: true });
    for (const client of clientList) {
      if ("focus" in client) {
        client.navigate(targetUrl);
        return client.focus();
      }
    }
    return self.clients.openWindow(targetUrl);
  })());
});
