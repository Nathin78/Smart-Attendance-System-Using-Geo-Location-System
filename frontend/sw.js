const CACHE_NAME = "smart-attendance-v1";
const APP_SHELL = [
  "./",
  "index.html",
  "login.html",
  "user-login.html",
  "admin-login.html",
  "register.html",
  "user-dashboard.html",
  "admin-dashboard.html",
  "attendance.html",
  "profile.html",
  "about.html",
  "contact.html",
  "css/styles.css",
  "js/api.js",
  "js/config.js",
  "js/attendance.js",
  "js/admin-dashboard.js",
  "js/user-dashboard.js",
  "js/login.js",
  "js/register.js",
  "js/profile.js",
  "js/about.js",
  "js/contact.js",
  "js/local-dev.js",
  "favicon.svg",
  "favicon.ico"
];

self.addEventListener("install", (event) => {
  event.waitUntil(
    caches.open(CACHE_NAME).then((cache) => cache.addAll(APP_SHELL))
  );
});

self.addEventListener("activate", (event) => {
  event.waitUntil(
    caches.keys().then((keys) =>
      Promise.all(keys.filter((key) => key !== CACHE_NAME).map((key) => caches.delete(key)))
    )
  );
});

self.addEventListener("fetch", (event) => {
  if (event.request.method !== "GET") return;
  event.respondWith(
    caches.match(event.request).then((cached) => cached || fetch(event.request))
  );
});
