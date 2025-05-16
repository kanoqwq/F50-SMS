const CACHE_NAME = 'ufi_tools_sw_cache';
const URLS_TO_CACHE = [
  '/',
  '/index.html',
  '/style/style.css',
  '/main.js',
  '/draglist.js',
  '/requests.js',
  '/theme.js',
  '/utils.js',
  '/icons/icon-192.webp',
  '/icons/icon-512.webp'
];

self.addEventListener('install', event => {
  event.waitUntil(
    caches.open(CACHE_NAME).then(cache => {
      return cache.addAll(URLS_TO_CACHE);
    })
  );
});

self.addEventListener('fetch', event => {
  event.respondWith(
    caches.match(event.request).then(response => {
      return response || fetch(event.request);
    })
  );
});