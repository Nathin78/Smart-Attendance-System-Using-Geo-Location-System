const http = require('http');
const fs = require('fs');
const path = require('path');

const root = path.resolve(process.cwd(), 'frontend');
const port = Number(process.env.PORT || 5500);

const mime = {
  '.html': 'text/html; charset=utf-8',
  '.css': 'text/css; charset=utf-8',
  '.js': 'application/javascript; charset=utf-8',
  '.json': 'application/json; charset=utf-8',
  '.png': 'image/png',
  '.jpg': 'image/jpeg',
  '.jpeg': 'image/jpeg',
  '.gif': 'image/gif',
  '.svg': 'image/svg+xml',
  '.ico': 'image/x-icon'
};

http.createServer((req, res) => {
  const rawPath = req.url.split('?')[0];
  let safePath = decodeURIComponent(rawPath || '/');
  if (safePath === '/' || safePath === '\\') safePath = '/index.html';
  safePath = path.posix.normalize(safePath).replace(/^\/+/, '');
  const filePath = path.resolve(root, safePath);

  if (!filePath.startsWith(root + path.sep)) {
    res.writeHead(403);
    res.end('Forbidden');
    return;
  }

  fs.stat(filePath, (err, stat) => {
    if (err || !stat.isFile()) {
      res.writeHead(404, { 'Content-Type': 'text/plain; charset=utf-8' });
      res.end('Not Found');
      return;
    }

    const ext = path.extname(filePath).toLowerCase();
    res.writeHead(200, { 'Content-Type': mime[ext] || 'application/octet-stream' });
    fs.createReadStream(filePath).pipe(res);
  });
}).listen(port, () => {
  console.log(`Frontend running at http://localhost:${port}/index.html`);
});
