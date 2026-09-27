# Reasonix project memory

Notes the user pinned via the `#` prompt prefix. The whole file is
loaded into the immutable system prefix every session — keep it terse.

- ==========================================
# thdqwx.work — Nginx 配置
# ==========================================

server {
    listen 80;
    server_name thdqwx.work www.thdqwx.work admin.thdqwx.work api.thdqwx.work;
    return 301 https://$host$request_uri;
}

# ---- 官网 ----
server {
    listen 443 ssl;
    http2 on;
    server_name www.thdqwx.work;

    ssl_certificate     /etc/nginx/ssl/thdqwx.work/fullchain.pem;
    ssl_certificate_key /etc/nginx/ssl/thdqwx.work/private.key;

    ssl_session_timeout 10m;
    ssl_session_cache   shared:SSL:10m;
    ssl_protocols       TLSv1.2 TLSv1.3;
    ssl_ciphers         HIGH:!aNULL:!MD5;
    ssl_prefer_server_ciphers on;

    root /opt/electrical-repair-shop/official-website;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }

    location ~* \.(?:js|css|png|jpg|jpeg|gif|svg|ico|webp|woff2?)$ {
        expires 7d;
        add_header Cache-Control "public";
        try_files $uri =404;
    }
}

# ---- 后台 ----
server {
    listen 443 ssl;
    http2 on;
    server_name admin.thdqwx.work;

    ssl_certificate     /etc/nginx/ssl/thdqwx.work/fullchain.pem;
    ssl_certificate_key /etc/nginx/ssl/thdqwx.work/private.key;

    ssl_session_timeout 10m;
    ssl_session_cache   shared:SSL:10m;
    ssl_protocols       TLSv1.2 TLSv1.3;
    ssl_ciphers         HIGH:!aNULL:!MD5;
    ssl_prefer_server_ciphers on;

    root /opt/electrical-repair-shop/admin-front-end;
    index index.html;

    client_max_body_size 35m;

    location / {
        try_files $uri $uri/ /index.html;
    }

    location ~* \.(?:js|css|png|jpg|jpeg|gif|svg|ico|webp|woff2?)$ {
        expires 7d;
        add_header Cache-Control "public";
        try_files $uri =404;
    }
}

# ---- API ----
server {
    listen 443 ssl;
    http2 on;
    server_name api.thdqwx.work;

    ssl_certificate     /etc/nginx/ssl/thdqwx.work/fullchain.pem;
    ssl_certificate_key /etc/nginx/ssl/thdqwx.work/private.key;

    ssl_session_timeout 10m;
    ssl_session_cache   shared:SSL:10m;
    ssl_protocols       TLSv1.2 TLSv1.3;
    ssl_ciphers         HIGH:!aNULL:!MD5;
    ssl_prefer_server_ciphers on;

    client_max_body_size 35m;

    location / {
        proxy_pass http://127.0.0.1:8081/api/;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_read_timeout 120s;
        proxy_send_timeout 120s;
    }
}

# ---- 裸域跳转 ----
server {
    listen 443 ssl;
    http2 on;
    server_name thdqwx.work;

    ssl_certificate     /etc/nginx/ssl/thdqwx.work/fullchain.pem;
    ssl_certificate_key /etc/nginx/ssl/thdqwx.work/private.key;

    return 301 https://www.thdqwx.work$request_uri;
}
这是服务器上的nginx配置
- ==========================================
# thdqwx.work — Nginx 配置
# ==========================================

server {
    listen 80;
    server_name thdqwx.work www.thdqwx.work admin.thdqwx.work api.thdqwx.work;
    return 301 https://$host$request_uri;
}

# ---- 官网 ----
server {
    listen 443 ssl;
    http2 on;
    server_name www.thdqwx.work;

    ssl_certificate     /etc/nginx/ssl/thdqwx.work/fullchain.pem;
    ssl_certificate_key /etc/nginx/ssl/thdqwx.work/private.key;

    ssl_session_timeout 10m;
    ssl_session_cache   shared:SSL:10m;
    ssl_protocols       TLSv1.2 TLSv1.3;
    ssl_ciphers         HIGH:!aNULL:!MD5;
    ssl_prefer_server_ciphers on;

    root /opt/electrical-repair-shop/official-website;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }

    location ~* \.(?:js|css|png|jpg|jpeg|gif|svg|ico|webp|woff2?)$ {
        expires 7d;
        add_header Cache-Control "public";
        try_files $uri =404;
    }
}

# ---- 后台 ----
server {
    listen 443 ssl;
    http2 on;
    server_name admin.thdqwx.work;

    ssl_certificate     /etc/nginx/ssl/thdqwx.work/fullchain.pem;
    ssl_certificate_key /etc/nginx/ssl/thdqwx.work/private.key;

    ssl_session_timeout 10m;
    ssl_session_cache   shared:SSL:10m;
    ssl_protocols       TLSv1.2 TLSv1.3;
    ssl_ciphers         HIGH:!aNULL:!MD5;
    ssl_prefer_server_ciphers on;

    root /opt/electrical-repair-shop/admin-front-end;
    index index.html;

    client_max_body_size 35m;

    location / {
        try_files $uri $uri/ /index.html;
    }

    location ~* \.(?:js|css|png|jpg|jpeg|gif|svg|ico|webp|woff2?)$ {
        expires 7d;
        add_header Cache-Control "public";
        try_files $uri =404;
    }
}

# ---- API ----
server {
    listen 443 ssl;
    http2 on;
    server_name api.thdqwx.work;

    ssl_certificate     /etc/nginx/ssl/thdqwx.work/fullchain.pem;
    ssl_certificate_key /etc/nginx/ssl/thdqwx.work/private.key;

    ssl_session_timeout 10m;
    ssl_session_cache   shared:SSL:10m;
    ssl_protocols       TLSv1.2 TLSv1.3;
    ssl_ciphers         HIGH:!aNULL:!MD5;
    ssl_prefer_server_ciphers on;

    client_max_body_size 35m;

    location / {
        proxy_pass http://127.0.0.1:8081/api/;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_read_timeout 120s;
        proxy_send_timeout 120s;
    }
}

# ---- 裸域跳转 ----
server {
    listen 443 ssl;
    http2 on;
    server_name thdqwx.work;

    ssl_certificate     /etc/nginx/ssl/thdqwx.work/fullchain.pem;
    ssl_certificate_key /etc/nginx/ssl/thdqwx.work/private.key;

    return 301 https://www.thdqwx.work$request_uri;
}
这是服务器上的nginx配置
- ==========================================
# thdqwx.work — Nginx 配置
# ==========================================

server {
    listen 80;
    server_name thdqwx.work www.thdqwx.work admin.thdqwx.work api.thdqwx.work;
    return 301 https://$host$request_uri;
}

# ---- 官网 ----
server {
    listen 443 ssl;
    http2 on;
    server_name www.thdqwx.work;

    ssl_certificate     /etc/nginx/ssl/thdqwx.work/fullchain.pem;
    ssl_certificate_key /etc/nginx/ssl/thdqwx.work/private.key;

    ssl_session_timeout 10m;
    ssl_session_cache   shared:SSL:10m;
    ssl_protocols       TLSv1.2 TLSv1.3;
    ssl_ciphers         HIGH:!aNULL:!MD5;
    ssl_prefer_server_ciphers on;

    root /opt/electrical-repair-shop/official-website;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }

    location ~* \.(?:js|css|png|jpg|jpeg|gif|svg|ico|webp|woff2?)$ {
        expires 7d;
        add_header Cache-Control "public";
        try_files $uri =404;
    }
}

# ---- 后台 ----
server {
    listen 443 ssl;
    http2 on;
    server_name admin.thdqwx.work;

    ssl_certificate     /etc/nginx/ssl/thdqwx.work/fullchain.pem;
    ssl_certificate_key /etc/nginx/ssl/thdqwx.work/private.key;

    ssl_session_timeout 10m;
    ssl_session_cache   shared:SSL:10m;
    ssl_protocols       TLSv1.2 TLSv1.3;
    ssl_ciphers         HIGH:!aNULL:!MD5;
    ssl_prefer_server_ciphers on;

    root /opt/electrical-repair-shop/admin-front-end;
    index index.html;

    client_max_body_size 35m;

    location / {
        try_files $uri $uri/ /index.html;
    }

    location ~* \.(?:js|css|png|jpg|jpeg|gif|svg|ico|webp|woff2?)$ {
        expires 7d;
        add_header Cache-Control "public";
        try_files $uri =404;
    }
}

# ---- API ----
server {
    listen 443 ssl;
    http2 on;
    server_name api.thdqwx.work;

    ssl_certificate     /etc/nginx/ssl/thdqwx.work/fullchain.pem;
    ssl_certificate_key /etc/nginx/ssl/thdqwx.work/private.key;

    ssl_session_timeout 10m;
    ssl_session_cache   shared:SSL:10m;
    ssl_protocols       TLSv1.2 TLSv1.3;
    ssl_ciphers         HIGH:!aNULL:!MD5;
    ssl_prefer_server_ciphers on;

    client_max_body_size 35m;

    location / {
        proxy_pass http://127.0.0.1:8081/api/;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_read_timeout 120s;
        proxy_send_timeout 120s;
    }
}

# ---- 裸域跳转 ----
server {
    listen 443 ssl;
    http2 on;
    server_name thdqwx.work;

    ssl_certificate     /etc/nginx/ssl/thdqwx.work/fullchain.pem;
    ssl_certificate_key /etc/nginx/ssl/thdqwx.work/private.key;

    return 301 https://www.thdqwx.work$request_uri;
}
这个是nginx的配置，你看一下有问题吗
- ==========================================
# thdqwx.work — Nginx 配置
# ==========================================

server {
    listen 80;
    server_name thdqwx.work www.thdqwx.work admin.thdqwx.work api.thdqwx.work;
    return 301 https://$host$request_uri;
}

# ---- 官网 ----
server {
    listen 443 ssl;
    http2 on;
    server_name www.thdqwx.work;

    ssl_certificate     /etc/nginx/ssl/thdqwx.work/fullchain.pem;
    ssl_certificate_key /etc/nginx/ssl/thdqwx.work/private.key;

    ssl_session_timeout 10m;
    ssl_session_cache   shared:SSL:10m;
    ssl_protocols       TLSv1.2 TLSv1.3;
    ssl_ciphers         HIGH:!aNULL:!MD5;
    ssl_prefer_server_ciphers on;

    root /opt/electrical-repair-shop/official-website;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }

    location ~* \.(?:js|css|png|jpg|jpeg|gif|svg|ico|webp|woff2?)$ {
        expires 7d;
        add_header Cache-Control "public";
        try_files $uri =404;
    }
}

# ---- 后台 ----
server {
    listen 443 ssl;
    http2 on;
    server_name admin.thdqwx.work;

    ssl_certificate     /etc/nginx/ssl/thdqwx.work/fullchain.pem;
    ssl_certificate_key /etc/nginx/ssl/thdqwx.work/private.key;

    ssl_session_timeout 10m;
    ssl_session_cache   shared:SSL:10m;
    ssl_protocols       TLSv1.2 TLSv1.3;
    ssl_ciphers         HIGH:!aNULL:!MD5;
    ssl_prefer_server_ciphers on;

    root /opt/electrical-repair-shop/admin-front-end;
    index index.html;

    client_max_body_size 35m;

    location / {
        try_files $uri $uri/ /index.html;
    }

    location ~* \.(?:js|css|png|jpg|jpeg|gif|svg|ico|webp|woff2?)$ {
        expires 7d;
        add_header Cache-Control "public";
        try_files $uri =404;
    }
}

# ---- API ----
server {
    listen 443 ssl;
    http2 on;
    server_name api.thdqwx.work;

    ssl_certificate     /etc/nginx/ssl/thdqwx.work/fullchain.pem;
    ssl_certificate_key /etc/nginx/ssl/thdqwx.work/private.key;

    ssl_session_timeout 10m;
    ssl_session_cache   shared:SSL:10m;
    ssl_protocols       TLSv1.2 TLSv1.3;
    ssl_ciphers         HIGH:!aNULL:!MD5;
    ssl_prefer_server_ciphers on;

    client_max_body_size 35m;

    location / {
        proxy_pass http://127.0.0.1:8081/api/;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_read_timeout 120s;
        proxy_send_timeout 120s;
    }
}

# ---- 裸域跳转 ----
server {
    listen 443 ssl;
    http2 on;
    server_name thdqwx.work;

    ssl_certificate     /etc/nginx/ssl/thdqwx.work/fullchain.pem;
    ssl_certificate_key /etc/nginx/ssl/thdqwx.work/private.key;

    return 301 https://www.thdqwx.work$request_uri;
}
这个是nginx的配置，你看一下有问题吗
