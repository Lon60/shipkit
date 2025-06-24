server {
    listen 80;
    server_name ${domain};

    location /.well-known/acme-challenge/ {
        root /var/www/certbot;
    }

    location /api/ {
        proxy_pass http://gateway-api:8080/;
        include /etc/nginx/snippets/proxy-headers.conf;
        include /etc/nginx/snippets/cors.conf;
    }

    location / {
        <#if forceSsl>
            return 301 https://$host$request_uri;
        </#if>
        
        proxy_pass http://frontend:3000;
        include /etc/nginx/snippets/proxy-headers.conf;
        
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection 'upgrade';
        
        proxy_connect_timeout 50s;
        proxy_send_timeout 50s;
        proxy_read_timeout 50s;
        
        proxy_buffering off;
        proxy_cache off;
    }
}

<#if sslEnabled>
server {
    listen 443 ssl http2;
    server_name ${domain};

    ssl_certificate /etc/letsencrypt/live/${domain}/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/${domain}/privkey.pem;
    
    # Recommended SSL settings
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_prefer_server_ciphers off;
    ssl_ciphers "EECDH+AESGCM:EDH+AESGCM:AES256+EECDH:AES256+EDH";
    ssl_ecdh_curve secp384r1;
    ssl_session_cache shared:SSL:10m;
    ssl_session_timeout 10m;
    ssl_session_tickets off;
    ssl_stapling on;
    ssl_stapling_verify on;
    resolver 8.8.8.8 8.8.4.4 valid=300s;
    resolver_timeout 5s;
    add_header Strict-Transport-Security "max-age=63072000; includeSubDomains; preload";
    add_header X-Frame-Options DENY;
    add_header X-Content-Type-Options nosniff;
    add_header X-XSS-Protection "1; mode=block";

    location / {
        proxy_pass http://frontend:3000;
        include /etc/nginx/snippets/proxy-headers.conf;
        
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection 'upgrade';
        
        proxy_connect_timeout 50s;
        proxy_send_timeout 50s;
        proxy_read_timeout 50s;
        
        proxy_buffering off;
        proxy_cache off;
    }

    location /api/ {
        proxy_pass http://gateway-api:8080/;
        include /etc/nginx/snippets/proxy-headers.conf;
        include /etc/nginx/snippets/cors.conf;
    }
}
</#if> 