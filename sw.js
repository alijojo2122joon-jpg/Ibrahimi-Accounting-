/* Ibrahimi Accounting — offline service worker */
const VER = 'ia-v7.6';
const SHELL = ['./', './index.html', './manifest.json', './icons/icon-192.png', './icons/icon-512.png', './icons/icon.svg'];
const RUNTIME = 'ia-runtime';
const TILES = 'ia-tiles';
self.addEventListener('install', e => {
  e.waitUntil(caches.open(VER).then(c => c.addAll(SHELL)).then(() => self.skipWaiting()));
});
self.addEventListener('activate', e => {
  e.waitUntil(caches.keys().then(keys => Promise.all(keys.filter(k => ![VER, RUNTIME, TILES].includes(k)).map(k => caches.delete(k)))).then(() => self.clients.claim()));
});
async function trim(name, max) { const c = await caches.open(name); const k = await c.keys(); for (let i = 0; i < k.length - max; i++) await c.delete(k[i]); }
self.addEventListener('fetch', e => {
  const req = e.request; if (req.method !== 'GET') return;
  const url = new URL(req.url);
  if (url.hostname.endsWith('supabase.co')) return;               // sync traffic: always live
  if (req.mode === 'navigate') {                                   // app page: network first, cache fallback
    e.respondWith(fetch(req).then(r => { const cp = r.clone(); caches.open(VER).then(c => c.put('./index.html', cp)); return r; }).catch(() => caches.match('./index.html')));
    return;
  }
  if (url.hostname.includes('tile.openstreetmap.org')) {           // map tiles: cache first, keep last 600
    e.respondWith(caches.open(TILES).then(c => c.match(req).then(hit => hit || fetch(req).then(r => { c.put(req, r.clone()); trim(TILES, 600); return r; }))));
    return;
  }
  // fonts, leaflet, icons: stale-while-revalidate
  e.respondWith(caches.match(req).then(hit => {
    const net = fetch(req).then(r => { if (r && (r.ok || r.type === 'opaque')) { const cp = r.clone(); caches.open(RUNTIME).then(c => c.put(req, cp)); } return r; }).catch(() => hit);
    return hit || net;
  }));
});
