const http = require('http');
const https = require('https');
const url = require('url');

const PORT = 3000;
const TARGET = 'https://api.example.com'; // 替换为实际的API地址

const server = http.createServer((req, res) => {
  if (req.method === 'OPTIONS') {
    // 处理预检请求
    res.writeHead(204, {
      'Access-Control-Allow-Origin': '*',
      'Access-Control-Allow-Methods': 'POST, OPTIONS',
      'Access-Control-Allow-Headers': 'Content-Type, Authorization',
      'Access-Control-Max-Age': '86400'
    });
    res.end();
    return;
  }

  const parsedUrl = url.parse(req.url);
  const options = {
    hostname: url.parse(TARGET).hostname,
    port: url.parse(TARGET).port || 443,
    path: parsedUrl.path,
    method: req.method,
    headers: req.headers
  };

  delete options.headers['host'];
  delete options.headers['origin'];
  delete options.headers['referer'];

  const proxyReq = https.request(options, (proxyRes) => {
    res.writeHead(proxyRes.statusCode, {
      ...proxyRes.headers,
      'Access-Control-Allow-Origin': '*'
    });
    proxyRes.pipe(res);
  });

  req.pipe(proxyReq);

  proxyReq.on('error', (error) => {
    console.error('Proxy request error:', error);
    res.writeHead(500);
    res.end('Proxy error');
  });
});

server.listen(PORT, () => {
  console.log(`Proxy server running on http://localhost:${PORT}`);
});
