# Deployment Guide

This guide provides step-by-step instructions for deploying the Community Services Marketplace, including both the API backend and React frontend.

## Table of Contents

- [Prerequisites](#prerequisites)
- [Quick Start with Docker Compose](#quick-start-with-docker-compose)
- [Backend Deployment](#backend-deployment)
  - [Manual Backend Deployment](#manual-backend-deployment)
  - [Backend Production Setup](#backend-production-setup)
- [Frontend Deployment](#frontend-deployment)
  - [Development Setup](#development-setup)
  - [Production Build](#production-build)
  - [Static Hosting Deployment](#static-hosting-deployment)
  - [Docker Deployment](#docker-deployment)
  - [Nginx Deployment](#nginx-deployment)
- [Full-Stack Deployment](#full-stack-deployment)
- [Cloud Platform Deployment](#cloud-platform-deployment)
- [Monitoring and Maintenance](#monitoring-and-maintenance)
- [Troubleshooting](#troubleshooting)

## Prerequisites

### Backend Requirements

- **Docker** 20.10+ and **Docker Compose** 2.0+ (for containerized deployment)
- **PostgreSQL** 16+ (if deploying without Docker)
- **JDK** 17+ (for manual builds)
- **Gradle** 8.5+ (for manual builds)

### Frontend Requirements

- **Node.js** 18+ and **npm** 9+ (or **yarn**)
- **Nginx** or similar web server (for production static hosting)
- **Docker** (optional, for containerized frontend deployment)

### Required External Services

- **vas3k.club OAuth2 credentials**: You need to register your application with vas3k.club to obtain:
  - Client ID
  - Client Secret
  - Approved redirect URLs

### System Requirements

- **CPU**: 2+ cores recommended
- **RAM**: 2GB minimum, 4GB recommended
- **Disk**: 10GB minimum for application and database
- **Network**: Port 8080 must be available (or configure alternative)

## Quick Start with Docker Compose

The fastest way to get the server running for development or testing.

### 1. Clone the Repository

```bash
git clone <repository-url>
cd service-list
```

### 2. Configure Environment Variables

Copy the example environment file and edit it:

```bash
cp .env.example .env
```

Edit `.env` and set the required variables:

```env
# Database Configuration
DATABASE_URL=jdbc:postgresql://postgres:5432/community_services
DATABASE_USER=postgres
DATABASE_PASSWORD=your_secure_password_here

# OAuth2 Configuration (vas3k.club)
VAS3K_CLIENT_ID=your_client_id_here
VAS3K_CLIENT_SECRET=your_client_secret_here
OAUTH_REDIRECT_URL=http://localhost:8080/api/v1/auth/callback

# JWT Configuration
JWT_SECRET=your-super-secret-jwt-key-change-in-production-minimum-32-chars
```

**Important**:
- Change `DATABASE_PASSWORD` to a secure password
- Generate a strong random `JWT_SECRET` (at least 32 characters)
- Obtain real `VAS3K_CLIENT_ID` and `VAS3K_CLIENT_SECRET` from vas3k.club

### 3. Start the Services

```bash
docker-compose up -d
```

This will:
- Start PostgreSQL 16 database
- Build the application Docker image
- Start the API server on port 8080
- Automatically create database tables on first run

### 4. Verify the Deployment

Check the health endpoint:

```bash
curl http://localhost:8080/health
```

Expected response:
```json
{
  "status": "ok",
  "version": "1.0.0"
}
```

View logs:

```bash
docker-compose logs -f app
```

### 5. Stop the Services

```bash
docker-compose down
```

To also remove the database volume:

```bash
docker-compose down -v
```

## Backend Deployment

### Manual Backend Deployment

For deployments without Docker or for development purposes.

### 1. Install PostgreSQL

Install PostgreSQL 16+ and create a database:

```bash
# On Ubuntu/Debian
sudo apt-get update
sudo apt-get install postgresql-16

# Start PostgreSQL service
sudo systemctl start postgresql
sudo systemctl enable postgresql

# Create database and user
sudo -u postgres psql
```

In PostgreSQL shell:

```sql
CREATE DATABASE community_services;
CREATE USER community_app WITH PASSWORD 'your_password';
GRANT ALL PRIVILEGES ON DATABASE community_services TO community_app;
\q
```

### 2. Install JDK 17

```bash
# On Ubuntu/Debian
sudo apt-get install openjdk-17-jdk

# Verify installation
java -version
```

### 3. Build the Application

```bash
# Clone repository
git clone <repository-url>
cd service-list

# Build with Gradle
./gradlew build -x test

# The JAR file will be created at:
# build/libs/community-services-all.jar
```

### 4. Configure Application

Create a custom `application.yaml` or use environment variables:

```bash
export DATABASE_URL="jdbc:postgresql://localhost:5432/community_services"
export DATABASE_USER="community_app"
export DATABASE_PASSWORD="your_password"
export VAS3K_CLIENT_ID="your_client_id"
export VAS3K_CLIENT_SECRET="your_client_secret"
export OAUTH_REDIRECT_URL="http://localhost:8080/api/v1/auth/callback"
export JWT_SECRET="your-super-secret-jwt-key-minimum-32-characters"
```

### 5. Run the Application

```bash
java -Xmx2048m -jar build/libs/community-services-all.jar
```

The server will start on `http://0.0.0.0:8080`

### 6. Create a Systemd Service (Optional)

For production deployments, create a systemd service:

```bash
sudo nano /etc/systemd/system/community-services.service
```

Add the following content:

```ini
[Unit]
Description=Community Services Marketplace API
After=postgresql.service
Requires=postgresql.service

[Service]
Type=simple
User=community
WorkingDirectory=/opt/community-services
Environment="DATABASE_URL=jdbc:postgresql://localhost:5432/community_services"
Environment="DATABASE_USER=community_app"
Environment="DATABASE_PASSWORD=your_password"
Environment="VAS3K_CLIENT_ID=your_client_id"
Environment="VAS3K_CLIENT_SECRET=your_client_secret"
Environment="OAUTH_REDIRECT_URL=https://yourdomain.com/api/v1/auth/callback"
Environment="JWT_SECRET=your-super-secret-jwt-key"
ExecStart=/usr/bin/java -Xmx2048m -jar /opt/community-services/community-services-all.jar
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
```

Enable and start the service:

```bash
sudo systemctl daemon-reload
sudo systemctl enable community-services
sudo systemctl start community-services
sudo systemctl status community-services
```

### Backend Production Setup

#### Security Checklist

- [ ] Use strong, randomly generated `JWT_SECRET` (32+ characters)
- [ ] Use secure database passwords
- [ ] Never commit `.env` file to version control
- [ ] Enable HTTPS/TLS (configure reverse proxy)
- [ ] Configure firewall rules (only expose necessary ports)
- [ ] Keep OAuth2 credentials secure
- [ ] Regular security updates for base images and dependencies
- [ ] Enable database backups
- [ ] Configure proper log retention and rotation

#### Reverse Proxy Setup (Nginx)

It's recommended to run the backend application behind a reverse proxy for TLS termination and additional security.

Create `/etc/nginx/sites-available/community-services`:

```nginx
upstream community_services {
    server 127.0.0.1:8080;
}

server {
    listen 80;
    server_name yourdomain.com;

    # Redirect HTTP to HTTPS
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl http2;
    server_name yourdomain.com;

    # SSL Configuration
    ssl_certificate /etc/letsencrypt/live/yourdomain.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/yourdomain.com/privkey.pem;
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;

    # Security Headers
    add_header X-Frame-Options "SAMEORIGIN" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-XSS-Protection "1; mode=block" always;
    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;

    # Proxy Configuration
    location / {
        proxy_pass http://community_services;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;

        # WebSocket support (if needed in future)
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
    }

    # Health check endpoint
    location /health {
        proxy_pass http://community_services/health;
        access_log off;
    }
}
```

Enable the site:

```bash
sudo ln -s /etc/nginx/sites-available/community-services /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl reload nginx
```

#### Database Configuration

##### Connection Pooling

For production, tune the HikariCP connection pool in `application.yaml`:

```yaml
database:
  maxPoolSize: 20  # Adjust based on expected concurrent connections
  minimumIdle: 5
  connectionTimeout: 30000
  idleTimeout: 600000
  maxLifetime: 1800000
```

##### Backups

Set up automated PostgreSQL backups:

```bash
# Create backup script
sudo nano /opt/scripts/backup-db.sh
```

```bash
#!/bin/bash
BACKUP_DIR="/var/backups/postgresql"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="${BACKUP_DIR}/community_services_${TIMESTAMP}.sql.gz"

mkdir -p ${BACKUP_DIR}

# Create backup
pg_dump -U community_app -d community_services | gzip > ${BACKUP_FILE}

# Keep only last 7 days of backups
find ${BACKUP_DIR} -name "*.sql.gz" -mtime +7 -delete

echo "Backup completed: ${BACKUP_FILE}"
```

Make executable and add to cron:

```bash
sudo chmod +x /opt/scripts/backup-db.sh
sudo crontab -e
```

Add daily backup at 2 AM:

```
0 2 * * * /opt/scripts/backup-db.sh >> /var/log/postgres-backup.log 2>&1
```

#### Environment Variables in Production

Never store secrets in configuration files. Use environment variables or secret management:

**Option 1: Environment file (systemd)**

See systemd service example above.

**Option 2: Docker secrets**

```yaml
# docker-compose.prod.yaml
services:
  app:
    environment:
      DATABASE_URL: jdbc:postgresql://postgres:5432/community_services
      DATABASE_USER: postgres
      DATABASE_PASSWORD_FILE: /run/secrets/db_password
      JWT_SECRET_FILE: /run/secrets/jwt_secret
      VAS3K_CLIENT_ID_FILE: /run/secrets/vas3k_client_id
      VAS3K_CLIENT_SECRET_FILE: /run/secrets/vas3k_client_secret
    secrets:
      - db_password
      - jwt_secret
      - vas3k_client_id
      - vas3k_client_secret

secrets:
  db_password:
    file: ./secrets/db_password.txt
  jwt_secret:
    file: ./secrets/jwt_secret.txt
  vas3k_client_id:
    file: ./secrets/vas3k_client_id.txt
  vas3k_client_secret:
    file: ./secrets/vas3k_client_secret.txt
```

**Option 3: Cloud provider secrets** (AWS Secrets Manager, Google Secret Manager, etc.)

## Frontend Deployment

The frontend is a React + TypeScript application built with Vite. It can be deployed in multiple ways depending on your infrastructure.

### Development Setup

For local development with the backend running:

#### 1. Install Dependencies

```bash
cd frontend
npm install
```

#### 2. Configure API Connection

The development server is pre-configured to proxy API requests to `http://localhost:8080`.

Check `frontend/vite.config.ts`:

```typescript
export default defineConfig({
  server: {
    port: 3000,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      }
    }
  }
})
```

#### 3. Start Development Server

```bash
npm run dev
```

The frontend will be available at `http://localhost:3000`.

**Development Features:**
- Hot module replacement (HMR)
- TypeScript type checking
- ESLint integration
- Automatic API proxying to backend

### Production Build

Build the frontend for production deployment:

```bash
cd frontend

# Install dependencies
npm install

# Build for production
npm run build
```

This creates an optimized production build in `frontend/dist/` with:
- Minified JavaScript and CSS
- Tree-shaken dependencies
- Optimized assets
- Source maps (optional)

**Build output:**
```
frontend/dist/
├── index.html
├── assets/
│   ├── index-[hash].js
│   ├── index-[hash].css
│   └── [other assets]
└── vite.svg
```

### Static Hosting Deployment

The built frontend is a static Single Page Application (SPA) that can be deployed to any static hosting service.

#### Netlify Deployment

1. **Build the frontend:**
   ```bash
   cd frontend
   npm run build
   ```

2. **Deploy to Netlify:**
   ```bash
   # Install Netlify CLI
   npm install -g netlify-cli

   # Deploy
   netlify deploy --prod --dir=dist
   ```

3. **Configure rewrites** for SPA routing.

   Create `frontend/dist/_redirects`:
   ```
   /*    /index.html   200
   ```

4. **Set environment variables** in Netlify dashboard:
   - No build-time env vars needed (API URL is relative)
   - Configure custom domain and HTTPS

**Netlify configuration file** (`netlify.toml`):
```toml
[build]
  base = "frontend"
  command = "npm run build"
  publish = "dist"

[[redirects]]
  from = "/*"
  to = "/index.html"
  status = 200
```

#### Vercel Deployment

1. **Install Vercel CLI:**
   ```bash
   npm install -g vercel
   ```

2. **Deploy:**
   ```bash
   cd frontend
   vercel --prod
   ```

3. **Configure rewrites** in `vercel.json`:
   ```json
   {
     "rewrites": [
       { "source": "/(.*)", "destination": "/index.html" }
     ]
   }
   ```

#### GitHub Pages Deployment

1. **Add base path** in `vite.config.ts`:
   ```typescript
   export default defineConfig({
     base: '/repository-name/',
     // ...
   })
   ```

2. **Build and deploy:**
   ```bash
   npm run build

   # Install gh-pages
   npm install -g gh-pages

   # Deploy to gh-pages branch
   gh-pages -d dist
   ```

#### AWS S3 + CloudFront

1. **Build the frontend:**
   ```bash
   npm run build
   ```

2. **Create S3 bucket and enable static website hosting**

3. **Upload build files:**
   ```bash
   aws s3 sync frontend/dist/ s3://your-bucket-name/ --delete
   ```

4. **Configure CloudFront distribution:**
   - Origin: S3 bucket
   - Default root object: `index.html`
   - Error pages: Redirect 403/404 to `/index.html` (for SPA routing)

5. **Set up custom domain and SSL certificate**

### Docker Deployment

Deploy the frontend as a containerized application with Nginx.

#### Create Dockerfile

Create `frontend/Dockerfile`:

```dockerfile
# Build stage
FROM node:18-alpine AS build

WORKDIR /app

# Copy package files
COPY package*.json ./

# Install dependencies
RUN npm ci --only=production

# Copy source code
COPY . .

# Build the application
RUN npm run build

# Production stage
FROM nginx:alpine

# Copy built files from build stage
COPY --from=build /app/dist /usr/share/nginx/html

# Copy nginx configuration
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Expose port 80
EXPOSE 80

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost/ || exit 1

CMD ["nginx", "-g", "daemon off;"]
```

#### Create Nginx Configuration

Create `frontend/nginx.conf`:

```nginx
server {
    listen 80;
    server_name _;
    root /usr/share/nginx/html;
    index index.html;

    # Gzip compression
    gzip on;
    gzip_types text/plain text/css application/json application/javascript text/xml application/xml application/xml+rss text/javascript;
    gzip_min_length 1000;

    # SPA routing - redirect all requests to index.html
    location / {
        try_files $uri $uri/ /index.html;
    }

    # Cache static assets
    location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff|woff2|ttf|eot)$ {
        expires 1y;
        add_header Cache-Control "public, immutable";
    }

    # Security headers
    add_header X-Frame-Options "SAMEORIGIN" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-XSS-Protection "1; mode=block" always;

    # Proxy API requests to backend
    location /api/ {
        proxy_pass http://backend:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

#### Build and Run Docker Image

```bash
# Build the image
cd frontend
docker build -t community-services-frontend:latest .

# Run the container
docker run -d \
  --name frontend \
  -p 3000:80 \
  community-services-frontend:latest

# View logs
docker logs -f frontend
```

### Nginx Deployment

Deploy the frontend on a server with Nginx (without Docker).

#### 1. Build the Frontend

```bash
cd frontend
npm install
npm run build
```

#### 2. Copy Files to Server

```bash
# Copy built files to server
scp -r dist/* user@your-server:/var/www/community-services/
```

#### 3. Configure Nginx

Create `/etc/nginx/sites-available/community-services-frontend`:

```nginx
server {
    listen 80;
    server_name yourdomain.com;

    root /var/www/community-services;
    index index.html;

    # Gzip compression
    gzip on;
    gzip_types text/plain text/css application/json application/javascript text/xml application/xml application/xml+rss text/javascript;

    # SPA routing
    location / {
        try_files $uri $uri/ /index.html;
    }

    # Cache static assets
    location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff|woff2|ttf|eot)$ {
        expires 1y;
        add_header Cache-Control "public, immutable";
    }

    # Proxy API requests to backend
    location /api/ {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # Security headers
    add_header X-Frame-Options "SAMEORIGIN" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-XSS-Protection "1; mode=block" always;
}
```

#### 4. Enable Site and Reload Nginx

```bash
# Create symbolic link
sudo ln -s /etc/nginx/sites-available/community-services-frontend /etc/nginx/sites-enabled/

# Test configuration
sudo nginx -t

# Reload Nginx
sudo systemctl reload nginx
```

#### 5. Configure HTTPS with Let's Encrypt

```bash
# Install Certbot
sudo apt-get install certbot python3-certbot-nginx

# Obtain certificate
sudo certbot --nginx -d yourdomain.com

# Auto-renewal is set up automatically
```

### Frontend Environment Configuration

The frontend uses relative API URLs (`/api/v1`) which are proxied to the backend.

#### Development Configuration

In `frontend/vite.config.ts`:

```typescript
export default defineConfig({
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8080',  // Backend URL
        changeOrigin: true,
      }
    }
  }
})
```

#### Production Configuration

For production, configure the proxy in your web server (Nginx/Apache) or update the API base URL in `frontend/src/services/api.ts`:

```typescript
const api = axios.create({
  baseURL: process.env.VITE_API_URL || '/api/v1',
  // ...
});
```

Set `VITE_API_URL` during build:

```bash
VITE_API_URL=https://api.yourdomain.com/api/v1 npm run build
```

Or create a `.env.production` file in `frontend/`:

```env
VITE_API_URL=https://api.yourdomain.com/api/v1
```

**Note:** Vite only exposes env vars prefixed with `VITE_` to the client.

## Full-Stack Deployment

Deploy both frontend and backend together using Docker Compose.

### Docker Compose Configuration

Create or update `docker-compose.yml` in the project root:

```yaml
version: '3.8'

services:
  postgres:
    image: postgres:16
    environment:
      POSTGRES_DB: community_services
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: ${DATABASE_PASSWORD}
    volumes:
      - postgres_data:/var/lib/postgresql/data
    ports:
      - "5432:5432"
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres"]
      interval: 10s
      timeout: 5s
      retries: 5

  backend:
    build: .
    depends_on:
      postgres:
        condition: service_healthy
    environment:
      DATABASE_URL: jdbc:postgresql://postgres:5432/community_services
      DATABASE_USER: postgres
      DATABASE_PASSWORD: ${DATABASE_PASSWORD}
      VAS3K_CLIENT_ID: ${VAS3K_CLIENT_ID}
      VAS3K_CLIENT_SECRET: ${VAS3K_CLIENT_SECRET}
      OAUTH_REDIRECT_URL: ${OAUTH_REDIRECT_URL}
      JWT_SECRET: ${JWT_SECRET}
    ports:
      - "8080:8080"
    healthcheck:
      test: ["CMD", "wget", "--quiet", "--tries=1", "--spider", "http://localhost:8080/health"]
      interval: 30s
      timeout: 10s
      retries: 3

  frontend:
    build:
      context: ./frontend
      dockerfile: Dockerfile
    depends_on:
      - backend
    ports:
      - "80:80"
    environment:
      - BACKEND_URL=http://backend:8080
    healthcheck:
      test: ["CMD", "wget", "--quiet", "--tries=1", "--spider", "http://localhost/"]
      interval: 30s
      timeout: 3s
      retries: 3

volumes:
  postgres_data:
```

### Deploy the Full Stack

```bash
# 1. Configure environment variables
cp .env.example .env
# Edit .env with your values

# 2. Start all services
docker-compose up -d

# 3. View logs
docker-compose logs -f

# 4. Check health status
docker-compose ps

# 5. Stop services
docker-compose down

# 6. Stop and remove volumes
docker-compose down -v
```

### Production Full-Stack Deployment

For production with HTTPS and domain:

1. **Set up reverse proxy (Nginx)** on the host machine
2. **Configure SSL/TLS** with Let's Encrypt
3. **Update OAuth redirect URLs** to use your domain
4. **Configure CORS** in backend to allow your domain
5. **Use production environment variables**

Example production Nginx config:

```nginx
upstream frontend {
    server localhost:80;
}

upstream backend {
    server localhost:8080;
}

server {
    listen 443 ssl http2;
    server_name yourdomain.com;

    ssl_certificate /etc/letsencrypt/live/yourdomain.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/yourdomain.com/privkey.pem;

    # Frontend
    location / {
        proxy_pass http://frontend;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # Backend API
    location /api/ {
        proxy_pass http://backend;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

## Cloud Platform Deployment

### AWS Deployment (ECS)

1. **Build and push Docker image to ECR:**

```bash
# Authenticate Docker to ECR
aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin <account-id>.dkr.ecr.us-east-1.amazonaws.com

# Build and tag image
docker build -t community-services:latest .
docker tag community-services:latest <account-id>.dkr.ecr.us-east-1.amazonaws.com/community-services:latest

# Push to ECR
docker push <account-id>.dkr.ecr.us-east-1.amazonaws.com/community-services:latest
```

2. **Set up RDS PostgreSQL instance:**
   - Create PostgreSQL 16 instance in RDS
   - Note connection endpoint
   - Configure security groups to allow ECS access

3. **Create ECS Task Definition:**
   - Use Fargate or EC2 launch type
   - Set environment variables (use AWS Secrets Manager)
   - Allocate: 2 vCPU, 4GB RAM minimum
   - Map port 8080

4. **Create ECS Service:**
   - Configure Application Load Balancer
   - Set up target group for port 8080
   - Configure health check: `/health`
   - Enable auto-scaling based on CPU/memory

### Google Cloud Platform (Cloud Run)

```bash
# Build and push to Google Container Registry
gcloud builds submit --tag gcr.io/PROJECT_ID/community-services

# Deploy to Cloud Run
gcloud run deploy community-services \
  --image gcr.io/PROJECT_ID/community-services \
  --platform managed \
  --region us-central1 \
  --allow-unauthenticated \
  --set-env-vars DATABASE_URL=jdbc:postgresql://CLOUD_SQL_IP:5432/community_services \
  --set-secrets="JWT_SECRET=jwt-secret:latest,VAS3K_CLIENT_SECRET=vas3k-secret:latest" \
  --add-cloudsql-instances PROJECT_ID:REGION:INSTANCE_NAME
```

### Kubernetes Deployment

See example manifests:

**deployment.yaml:**

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: community-services
spec:
  replicas: 3
  selector:
    matchLabels:
      app: community-services
  template:
    metadata:
      labels:
        app: community-services
    spec:
      containers:
      - name: app
        image: community-services:1.0.0
        ports:
        - containerPort: 8080
        env:
        - name: DATABASE_URL
          valueFrom:
            configMapKeyRef:
              name: app-config
              key: database-url
        - name: JWT_SECRET
          valueFrom:
            secretKeyRef:
              name: app-secrets
              key: jwt-secret
        resources:
          requests:
            memory: "2Gi"
            cpu: "1000m"
          limits:
            memory: "4Gi"
            cpu: "2000m"
        livenessProbe:
          httpGet:
            path: /health
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /health
            port: 8080
          initialDelaySeconds: 10
          periodSeconds: 5
---
apiVersion: v1
kind: Service
metadata:
  name: community-services
spec:
  selector:
    app: community-services
  ports:
  - port: 80
    targetPort: 8080
  type: LoadBalancer
```

## Monitoring and Maintenance

### Health Checks

The server provides a health endpoint:

```bash
curl http://localhost:8080/health
```

Response when healthy:
```json
{
  "status": "ok",
  "version": "1.0.0"
}
```

### Logging

Logs are configured via Logback and output to stdout by default.

**View logs (Docker):**
```bash
docker-compose logs -f app
```

**View logs (systemd):**
```bash
sudo journalctl -u community-services -f
```

**Log levels** can be configured in `src/main/resources/logback.xml`

### Metrics and Monitoring

Consider integrating with monitoring solutions:

- **Prometheus**: Add Ktor metrics plugin
- **Datadog**: Use APM agent
- **New Relic**: JVM agent
- **CloudWatch**: For AWS deployments

### Database Maintenance

Regular maintenance tasks:

```sql
-- Vacuum and analyze
VACUUM ANALYZE;

-- Check database size
SELECT pg_size_pretty(pg_database_size('community_services'));

-- Check table sizes
SELECT
    schemaname,
    tablename,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) AS size
FROM pg_tables
WHERE schemaname = 'public'
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;

-- Check active connections
SELECT count(*) FROM pg_stat_activity WHERE datname = 'community_services';
```

### Updating the Application

1. **Build new version:**
```bash
./gradlew build -x test
docker build -t community-services:1.1.0 .
```

2. **Backup database:**
```bash
pg_dump -U community_app community_services > backup_before_update.sql
```

3. **Deploy update:**

   **Docker Compose:**
   ```bash
   docker-compose pull
   docker-compose up -d
   ```

   **Systemd:**
   ```bash
   sudo systemctl stop community-services
   cp build/libs/community-services-all.jar /opt/community-services/
   sudo systemctl start community-services
   ```

4. **Verify deployment:**
```bash
curl http://localhost:8080/health
```

## Troubleshooting

### Application Won't Start

**Check logs:**
```bash
docker-compose logs app
# or
sudo journalctl -u community-services -n 100
```

**Common issues:**

1. **Database connection failed:**
   - Verify PostgreSQL is running: `docker-compose ps` or `sudo systemctl status postgresql`
   - Check DATABASE_URL, DATABASE_USER, DATABASE_PASSWORD
   - Verify network connectivity: `telnet postgres 5432`

2. **Port 8080 already in use:**
   - Check what's using the port: `sudo lsof -i :8080`
   - Change port in `application.yaml` or docker-compose.yaml

3. **Out of memory:**
   - Increase heap size: `-Xmx4096m`
   - Check Docker memory limits

### OAuth2 Authentication Issues

1. **Invalid redirect_uri:**
   - Ensure OAUTH_REDIRECT_URL matches registered callback in vas3k.club
   - Check for trailing slashes

2. **Invalid client credentials:**
   - Verify VAS3K_CLIENT_ID and VAS3K_CLIENT_SECRET
   - Check credentials haven't expired

3. **User validation failed:**
   - Only active vas3k.club members with payment status can authenticate
   - Check vas3k.club API response in logs

### Database Issues

1. **Tables not created:**
   - Tables are auto-created on startup via SchemaUtils
   - Check logs for schema creation errors
   - Verify database user has CREATE privileges

2. **Connection pool exhausted:**
   - Increase maxPoolSize in application.yaml
   - Check for connection leaks (unclosed transactions)
   - Monitor active connections: `SELECT * FROM pg_stat_activity;`

3. **Slow queries:**
   - Enable PostgreSQL query logging
   - Analyze slow queries with EXPLAIN ANALYZE
   - Consider adding indexes

### Performance Issues

1. **High CPU usage:**
   - Check for infinite loops in application code
   - Monitor JVM garbage collection
   - Profile with VisualVM or JProfiler

2. **High memory usage:**
   - Increase heap size if needed
   - Check for memory leaks
   - Review connection pool settings

3. **Slow API responses:**
   - Enable request logging to identify slow endpoints
   - Check database query performance
   - Consider adding caching layer (Redis)

### Frontend Issues

1. **Build fails:**
   - Check Node.js version (18+ required)
   - Clear node_modules and reinstall: `rm -rf node_modules package-lock.json && npm install`
   - Check for TypeScript errors: `npm run build`

2. **API requests fail (404, CORS errors):**
   - Verify backend is running on port 8080
   - Check Vite proxy configuration in `vite.config.ts`
   - In production, ensure Nginx is proxying `/api/` to backend
   - Check CORS configuration in backend allows your frontend domain

3. **Blank page in production:**
   - Check browser console for errors
   - Verify all assets loaded (check Network tab)
   - Ensure SPA routing is configured (try_files in Nginx)
   - Check base path in `vite.config.ts` matches deployment path

4. **Authentication not working:**
   - Check OAuth redirect URL includes your frontend domain
   - Verify JWT token is stored in localStorage
   - Check browser console for auth errors
   - Ensure backend allows CORS from your frontend domain

### Getting Help

1. Check application logs first (backend and frontend)
2. Verify all environment variables are set correctly
3. Test database connectivity independently
4. Check browser console for frontend errors
5. Review the [Configuration Guide](CONFIGURATION.md)
6. Check GitHub issues for similar problems
7. Create a new issue with logs and environment details

## Next Steps

- Review [CONFIGURATION.md](CONFIGURATION.md) for detailed configuration options
- Set up monitoring and alerting
- Configure automated backups
- Implement CI/CD pipeline
- Review security best practices
- Set up staging environment

---

**Version:** 1.0.0
**Last Updated:** 2025-12-24
