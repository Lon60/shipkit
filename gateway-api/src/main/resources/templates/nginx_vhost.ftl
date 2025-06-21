server {
    listen 80;
    server_name ${domain};

    location / {
        proxy_pass http://frontend:3000;
        include /etc/nginx/snippets/proxy-headers.conf;
    }

    location /graphql {
        proxy_pass http://gateway-api:8080/graphql;
        include /etc/nginx/snippets/proxy-headers.conf;
        include /etc/nginx/snippets/cors.conf;
    }
} 