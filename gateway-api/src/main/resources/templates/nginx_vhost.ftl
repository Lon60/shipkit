server {
    listen 80;
    server_name ${domain};

    location /api/ {
        proxy_pass http://gateway-api:8080/;
        include /etc/nginx/snippets/proxy-headers.conf;
        include /etc/nginx/snippets/cors.conf;
    }

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
} 