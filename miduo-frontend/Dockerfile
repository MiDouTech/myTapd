# ============================================================
# Stage 1: Build the Vue 3 app
# ============================================================
FROM node:20-alpine AS builder

WORKDIR /app

# Copy dependency manifests first for layer caching
COPY package*.json ./
RUN npm ci

# Copy source code
COPY . .

# Build-time environment variables injected by CI
ARG VITE_APP_TITLE=米多工单系统
ARG VITE_API_BASE_URL=/api
ARG VITE_USE_PROXY=false
ARG VITE_WECOM_OAUTH_URL=

ENV VITE_APP_TITLE=$VITE_APP_TITLE
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL
ENV VITE_USE_PROXY=$VITE_USE_PROXY
ENV VITE_WECOM_OAUTH_URL=$VITE_WECOM_OAUTH_URL

RUN npm run build

# ============================================================
# Stage 2: Serve with Nginx
# ============================================================
FROM nginx:1.27-alpine

LABEL maintainer="miduo-team"

# Remove default nginx config
RUN rm /etc/nginx/conf.d/default.conf

# Copy custom nginx config
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Copy built static assets
COPY --from=builder /app/dist /usr/share/nginx/html

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
