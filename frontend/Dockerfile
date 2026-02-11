# ─── Stage 1: Build ────────────────────────────────────────────
FROM node:20-alpine AS build

WORKDIR /app

# Install dependencies first (layer caching)
COPY package.json package-lock.json ./
RUN npm ci --production=false

# Copy source and build
COPY . .
RUN npm run build

# ─── Stage 2: Serve with Nginx ─────────────────────────────────
FROM nginx:1.25-alpine

# Remove default config
RUN rm /etc/nginx/conf.d/default.conf

# Custom Nginx config — SPA + API proxy
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Copy built assets
COPY --from=build /app/dist /usr/share/nginx/html

# Non-root user
RUN addgroup -g 1001 -S linkhub && \
    adduser -S -u 1001 -G linkhub linkhub && \
    chown -R linkhub:linkhub /usr/share/nginx/html && \
    chown -R linkhub:linkhub /var/cache/nginx && \
    chown -R linkhub:linkhub /var/log/nginx && \
    touch /var/run/nginx.pid && \
    chown -R linkhub:linkhub /var/run/nginx.pid

USER linkhub

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
    CMD wget -q --spider http://localhost:80/ || exit 1

CMD ["nginx", "-g", "daemon off;"]
