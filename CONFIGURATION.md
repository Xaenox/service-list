# Configuration Guide

This guide provides detailed information about all configuration options for the Community Services Marketplace, including both backend API and frontend application.

## Table of Contents

- [Backend Configuration](#backend-configuration)
  - [Configuration Files](#configuration-files)
  - [Environment Variables](#environment-variables)
  - [Application Configuration](#application-configuration)
  - [Database Configuration](#database-configuration)
  - [OAuth2 Configuration](#oauth2-configuration)
  - [JWT Configuration](#jwt-configuration)
  - [Server Configuration](#server-configuration)
  - [Logging Configuration](#logging-configuration)
  - [CORS Configuration](#cors-configuration)
- [Frontend Configuration](#frontend-configuration)
  - [Environment Variables](#frontend-environment-variables)
  - [Vite Configuration](#vite-configuration)
  - [API URL Configuration](#api-url-configuration)
  - [Build Configuration](#build-configuration)
  - [Runtime Configuration](#runtime-configuration)
- [Security Best Practices](#security-best-practices)

## Backend Configuration

### Configuration Files

The backend application uses multiple configuration sources with the following priority (highest to lowest):

1. **Environment variables** (highest priority)
2. **application.yaml** (default configuration)
3. **Application defaults** (hardcoded fallbacks)

### Main Configuration File

Location: `src/main/resources/application.yaml`

This file contains all default configuration values and supports environment variable substitution using the `${VAR_NAME}` syntax.

### Environment File

Location: `.env` (not committed to version control)

Create from template:
```bash
cp .env.example .env
```

This file is loaded by Docker Compose and should contain all sensitive configuration values.

### Environment Variables

#### Required Environment Variables

These variables **must** be set for the application to run:

| Variable | Description | Example |
|----------|-------------|---------|
| `DATABASE_URL` | JDBC connection URL for PostgreSQL | `jdbc:postgresql://localhost:5432/community_services` |
| `DATABASE_USER` | Database username | `postgres` |
| `DATABASE_PASSWORD` | Database password | `secure_password_123` |
| `VAS3K_CLIENT_ID` | OAuth2 client ID from vas3k.club | `your_client_id` |
| `VAS3K_CLIENT_SECRET` | OAuth2 client secret from vas3k.club | `your_client_secret` |
| `OAUTH_REDIRECT_URL` | OAuth2 callback URL | `http://localhost:8080/api/v1/auth/callback` |
| `JWT_SECRET` | Secret key for signing JWT tokens (min 32 chars) | `your-super-secret-jwt-key-change-in-production` |

#### Optional Environment Variables

These variables have defaults but can be overridden:

| Variable | Description | Default |
|----------|-------------|---------|
| `SERVER_PORT` | HTTP server port | `8080` |
| `SERVER_HOST` | HTTP server bind address | `0.0.0.0` |
| `DATABASE_MAX_POOL_SIZE` | Maximum database connections | `10` |
| `JWT_ISSUER` | JWT token issuer | `community-services` |
| `JWT_AUDIENCE` | JWT token audience | `community-services-api` |
| `JWT_EXPIRATION_DAYS` | JWT token validity period | `30` |
| `LOG_LEVEL` | Application log level | `INFO` |

#### Setting Environment Variables

**Linux/macOS:**
```bash
export DATABASE_URL="jdbc:postgresql://localhost:5432/community_services"
export DATABASE_PASSWORD="secure_password"
```

**Windows (PowerShell):**
```powershell
$env:DATABASE_URL="jdbc:postgresql://localhost:5432/community_services"
$env:DATABASE_PASSWORD="secure_password"
```

**Docker Compose (.env file):**
```env
DATABASE_URL=jdbc:postgresql://postgres:5432/community_services
DATABASE_PASSWORD=secure_password
```

**Systemd Service:**
```ini
[Service]
Environment="DATABASE_URL=jdbc:postgresql://localhost:5432/community_services"
Environment="DATABASE_PASSWORD=secure_password"
```

### Application Configuration

#### application.yaml Structure

```yaml
ktor:
  deployment:
    port: ${SERVER_PORT:-8080}
    host: ${SERVER_HOST:-0.0.0.0}
  application:
    modules:
      - club.vas3k.services.ApplicationKt.module

database:
  url: ${DATABASE_URL}
  driver: org.postgresql.Driver
  user: ${DATABASE_USER}
  password: ${DATABASE_PASSWORD}
  maxPoolSize: ${DATABASE_MAX_POOL_SIZE:-10}

oauth:
  vas3k:
    clientId: ${VAS3K_CLIENT_ID}
    clientSecret: ${VAS3K_CLIENT_SECRET}
    authorizeUrl: https://vas3k.club/auth/o/authorize/
    accessTokenUrl: https://vas3k.club/auth/o/token/
    userInfoUrl: https://vas3k.club/auth/o/userinfo/
    redirectUrl: ${OAUTH_REDIRECT_URL}

jwt:
  secret: ${JWT_SECRET}
  issuer: ${JWT_ISSUER:-community-services}
  audience: ${JWT_AUDIENCE:-community-services-api}
  realm: Community Services
  expirationDays: ${JWT_EXPIRATION_DAYS:-30}
```

#### Modifying application.yaml

To customize configuration without environment variables, edit `src/main/resources/application.yaml`:

```yaml
# Example: Change server port permanently
ktor:
  deployment:
    port: 9090  # Changed from 8080
```

After changes, rebuild the application:
```bash
./gradlew build
```

### Database Configuration

#### Connection Settings

The application uses HikariCP for database connection pooling.

**Basic Configuration:**

```yaml
database:
  url: jdbc:postgresql://localhost:5432/community_services
  driver: org.postgresql.Driver
  user: postgres
  password: postgres
  maxPoolSize: 10
```

#### Advanced Connection Pool Settings

For production environments, you may want to tune the connection pool:

```yaml
database:
  url: jdbc:postgresql://localhost:5432/community_services
  driver: org.postgresql.Driver
  user: postgres
  password: postgres

  # Connection Pool Configuration
  maxPoolSize: 20                    # Maximum number of connections
  minimumIdle: 5                     # Minimum idle connections
  connectionTimeout: 30000           # 30 seconds
  idleTimeout: 600000                # 10 minutes
  maxLifetime: 1800000               # 30 minutes
  leakDetectionThreshold: 60000      # 60 seconds
```

**Connection Pool Parameters:**

| Parameter | Description | Default | Recommended |
|-----------|-------------|---------|-------------|
| `maxPoolSize` | Maximum connections in pool | 10 | 20-50 for production |
| `minimumIdle` | Minimum idle connections | maxPoolSize | 25% of maxPoolSize |
| `connectionTimeout` | Max wait for connection (ms) | 30000 | 30000 |
| `idleTimeout` | Max idle time before close (ms) | 600000 | 600000 |
| `maxLifetime` | Max connection lifetime (ms) | 1800000 | 1800000 |

#### Connection String Options

PostgreSQL JDBC supports additional connection parameters:

```
jdbc:postgresql://host:port/database?param1=value1&param2=value2
```

**Common parameters:**

```
# SSL connection
jdbc:postgresql://localhost:5432/community_services?ssl=true&sslmode=require

# Connection timeout
jdbc:postgresql://localhost:5432/community_services?connectTimeout=10

# Application name (appears in pg_stat_activity)
jdbc:postgresql://localhost:5432/community_services?ApplicationName=CommunityServices

# Schema search path
jdbc:postgresql://localhost:5432/community_services?currentSchema=public
```

#### Database Schema Management

The application uses Exposed ORM with automatic schema creation:

**Schema Initialization** (occurs on application startup):

```kotlin
// From DatabaseConfig.kt
SchemaUtils.createMissingTablesAndColumns(
    Users,
    Categories,
    Services,
    ServiceCategories
)
```

This will:
- Create tables if they don't exist
- Add missing columns to existing tables
- **NOT** modify or delete existing columns
- **NOT** drop tables

#### Manual Schema Management

To manually manage schema:

**Export current schema:**
```bash
pg_dump -U postgres -d community_services --schema-only > schema.sql
```

**Apply migrations manually:**
```sql
-- Add index example
CREATE INDEX IF NOT EXISTS idx_services_owner ON services(owner_id);

-- Add column example
ALTER TABLE users ADD COLUMN IF NOT EXISTS last_login TIMESTAMP;
```

#### Database Backup and Restore

**Backup:**
```bash
# Full backup
pg_dump -U postgres -d community_services > backup.sql

# Compressed backup
pg_dump -U postgres -d community_services | gzip > backup.sql.gz

# Custom format (faster restore)
pg_dump -U postgres -d community_services -Fc -f backup.dump
```

**Restore:**
```bash
# From SQL file
psql -U postgres -d community_services < backup.sql

# From compressed SQL
gunzip < backup.sql.gz | psql -U postgres -d community_services

# From custom format
pg_restore -U postgres -d community_services backup.dump
```

### OAuth2 Configuration

The backend application integrates with vas3k.club for authentication using OAuth2.

#### OAuth2 Settings

```yaml
oauth:
  vas3k:
    clientId: ${VAS3K_CLIENT_ID}
    clientSecret: ${VAS3K_CLIENT_SECRET}
    authorizeUrl: https://vas3k.club/auth/o/authorize/
    accessTokenUrl: https://vas3k.club/auth/o/token/
    userInfoUrl: https://vas3k.club/auth/o/userinfo/
    redirectUrl: ${OAUTH_REDIRECT_URL}
```

#### Obtaining OAuth2 Credentials

1. Register your application at vas3k.club
2. Configure authorized redirect URIs:
   - Development: `http://localhost:8080/api/v1/auth/callback`
   - Production: `https://yourdomain.com/api/v1/auth/callback`
3. Obtain Client ID and Client Secret
4. Set environment variables

#### OAuth2 Flow

The authentication flow:

```
1. User → GET /api/v1/auth/login?return_url=https://frontend.com/dashboard
2. Server → Redirect to vas3k.club authorization
3. User authenticates on vas3k.club
4. vas3k.club → Redirect to /api/v1/auth/callback?code=...&state=...
5. Server validates state and exchanges code for access token
6. Server fetches user info from vas3k.club
7. Server creates/updates user in local database
8. Server generates JWT token
9. Server → Redirect to return_url with JWT token
```

#### User Validation

The application validates users from vas3k.club:

- **Required:** Active membership with payment status
- **User data synced:**
  - slug (username)
  - email
  - full_name
  - avatar_url
  - country
  - city

**User Role Assignment:**

- Default role: `USER`
- Moderator role: Set manually in database
- Admin role: Set manually in database

To promote a user to admin:
```sql
UPDATE users SET role = 'ADMIN' WHERE slug = 'username';
```

#### Customizing OAuth2 Endpoints

If vas3k.club endpoints change, update `application.yaml`:

```yaml
oauth:
  vas3k:
    authorizeUrl: https://new-domain.com/oauth/authorize
    accessTokenUrl: https://new-domain.com/oauth/token
    userInfoUrl: https://new-domain.com/api/user
```

### JWT Configuration

JSON Web Tokens (JWT) are used for API authentication after OAuth2 login.

#### JWT Settings

```yaml
jwt:
  secret: ${JWT_SECRET}
  issuer: community-services
  audience: community-services-api
  realm: Community Services
  expirationDays: 30
```

#### JWT Secret Generation

The JWT secret should be:
- At least 32 characters long
- Random and unpredictable
- Different for each environment
- Kept secure and never committed to version control

**Generate a secure secret:**

```bash
# Using OpenSSL
openssl rand -base64 48

# Using Python
python3 -c "import secrets; print(secrets.token_urlsafe(48))"

# Using Node.js
node -e "console.log(require('crypto').randomBytes(48).toString('base64'))"
```

#### JWT Token Structure

Generated tokens include these claims:

```json
{
  "sub": "user-uuid",
  "slug": "username",
  "email": "user@example.com",
  "role": "USER",
  "iss": "community-services",
  "aud": "community-services-api",
  "exp": 1735689600
}
```

#### Token Expiration

Default: 30 days

To change expiration:

```yaml
jwt:
  expirationDays: 7  # Change to 7 days
```

Or via environment variable:
```bash
export JWT_EXPIRATION_DAYS=7
```

#### Authentication Strategies

The application uses multiple authentication strategies:

| Strategy | Description | Used For |
|----------|-------------|----------|
| `jwt` | Requires valid JWT token | Most API endpoints |
| `jwt-moderator` | Requires MODERATOR or ADMIN role | Service moderation |
| `jwt-admin` | Requires ADMIN role only | Category management |
| `jwt-optional` | Optional authentication | Public service listing |

#### Using JWT Tokens

**Obtaining a token:**
```bash
# Login via OAuth2 flow
curl http://localhost:8080/api/v1/auth/login

# After OAuth flow, you'll receive a JWT token
```

**Using the token:**
```bash
curl -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  http://localhost:8080/api/v1/auth/me
```

#### Token Validation

Tokens are validated for:
- Valid signature (using JWT_SECRET)
- Correct issuer and audience
- Not expired
- User exists in database

### Server Configuration

#### HTTP Server Settings

```yaml
ktor:
  deployment:
    port: 8080        # Server port
    host: 0.0.0.0     # Bind address (0.0.0.0 = all interfaces)
```

#### Port Configuration

**Change port via environment variable:**
```bash
export SERVER_PORT=9090
```

**Change port in application.yaml:**
```yaml
ktor:
  deployment:
    port: 9090
```

#### Host Configuration

**Listen only on localhost:**
```yaml
ktor:
  deployment:
    host: 127.0.0.1
```

**Listen on all network interfaces (default):**
```yaml
ktor:
  deployment:
    host: 0.0.0.0
```

#### JVM Configuration

JVM settings are configured in `gradle.properties`:

```properties
org.gradle.jvmargs=-Xmx2048m
```

**For production, you may want to tune:**

```bash
java -Xmx4096m \           # Maximum heap size
     -Xms2048m \           # Initial heap size
     -XX:+UseG1GC \        # Use G1 garbage collector
     -XX:MaxGCPauseMillis=200 \  # GC pause target
     -jar community-services-all.jar
```

### Logging Configuration

#### Log Levels

Logging is configured via Logback. Default configuration in `src/main/resources/logback.xml`.

**Available log levels:**
- `TRACE` - Very detailed
- `DEBUG` - Debug information
- `INFO` - General information (default)
- `WARN` - Warning messages
- `ERROR` - Error messages only

#### Configuring Log Level

**Via environment variable:**
```bash
export LOG_LEVEL=DEBUG
```

**Via logback.xml:**
```xml
<configuration>
    <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <root level="INFO">
        <appender-ref ref="STDOUT" />
    </root>

    <!-- Set specific logger levels -->
    <logger name="club.vas3k.services" level="DEBUG" />
    <logger name="io.ktor" level="INFO" />
    <logger name="Exposed" level="DEBUG" />
</configuration>
```

#### Request Logging

The application logs all HTTP requests:

```
2025-12-24 10:30:45.123 [eventLoopGroupProxy-4-1] INFO  Application - GET /api/v1/services - 200 OK (15ms)
```

**Disable request logging:**

Comment out in `src/main/kotlin/club/vas3k/services/plugins/Logging.kt`:

```kotlin
install(CallLogging) {
    level = Level.INFO
    // format { call -> ... }
}
```

#### Database Query Logging

Exposed ORM can log SQL queries:

```xml
<!-- In logback.xml -->
<logger name="Exposed" level="DEBUG" />
```

This will output:
```
SQL: SELECT users.id, users.slug, ... FROM users WHERE users.id = ?
```

**For production**, set to `INFO` to reduce log volume.

### CORS Configuration

Cross-Origin Resource Sharing (CORS) is configured to allow frontend applications to access the API.

#### Current CORS Settings

Location: `src/main/kotlin/club/vas3k/services/plugins/HTTP.kt`

```kotlin
install(CORS) {
    allowMethod(HttpMethod.Options)
    allowMethod(HttpMethod.Get)
    allowMethod(HttpMethod.Post)
    allowMethod(HttpMethod.Put)
    allowMethod(HttpMethod.Delete)
    allowMethod(HttpMethod.Patch)
    allowHeader(HttpHeaders.Authorization)
    allowHeader(HttpHeaders.ContentType)
    allowCredentials = true
    anyHost() // Development only
}
```

#### Production CORS Configuration

**Security Warning:** `anyHost()` allows all origins and should **NOT** be used in production.

**For production**, specify allowed origins:

```kotlin
install(CORS) {
    allowMethod(HttpMethod.Options)
    allowMethod(HttpMethod.Get)
    allowMethod(HttpMethod.Post)
    allowMethod(HttpMethod.Put)
    allowMethod(HttpMethod.Delete)
    allowMethod(HttpMethod.Patch)

    allowHeader(HttpHeaders.Authorization)
    allowHeader(HttpHeaders.ContentType)

    allowCredentials = true

    // Specify allowed origins
    allowHost("yourdomain.com", schemes = listOf("https"))
    allowHost("www.yourdomain.com", schemes = listOf("https"))
    allowHost("app.yourdomain.com", schemes = listOf("https"))

    // Allow localhost for development
    allowHost("localhost:3000", schemes = listOf("http"))
}
```

#### Custom Headers

To allow additional headers:

```kotlin
allowHeader("X-Custom-Header")
allowHeader("X-Request-ID")
```

#### Preflight Request Caching

Configure max age for preflight requests:

```kotlin
maxAgeInSeconds = 3600 // Cache for 1 hour
```

## Frontend Configuration

The frontend is a React + TypeScript application built with Vite. Configuration is handled through environment variables and configuration files.

### Frontend Environment Variables

Vite uses environment variables prefixed with `VITE_` to expose values to the client-side code.

#### Available Environment Variables

| Variable | Description | Default | Required |
|----------|-------------|---------|----------|
| `VITE_API_URL` | Backend API base URL | `/api/v1` | No |
| `NODE_ENV` | Build environment | `development` | No |

#### Setting Environment Variables

**Development (.env.development):**

Create `frontend/.env.development`:

```env
# API URL for development (uses proxy)
VITE_API_URL=/api/v1

# Other development-specific settings
NODE_ENV=development
```

**Production (.env.production):**

Create `frontend/.env.production`:

```env
# API URL for production
VITE_API_URL=https://api.yourdomain.com/api/v1

# Or use relative URL if frontend and backend are on same domain
VITE_API_URL=/api/v1

NODE_ENV=production
```

**Build-time variables:**

Set environment variables during build:

```bash
# Set API URL during build
VITE_API_URL=https://api.example.com/api/v1 npm run build

# Multiple variables
VITE_API_URL=https://api.example.com/api/v1 NODE_ENV=production npm run build
```

**Important Notes:**
- Only variables prefixed with `VITE_` are exposed to client code
- Environment variables are embedded at build time, not runtime
- Never put secrets in `VITE_` variables (they're visible in the browser)
- Use different `.env` files for different environments

### Vite Configuration

Main configuration file: `frontend/vite.config.ts`

#### Development Server Configuration

```typescript
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],

  // Development server settings
  server: {
    port: 3000,              // Dev server port
    host: 'localhost',       // Bind address
    open: true,              // Auto-open browser

    // API proxy configuration
    proxy: {
      '/api': {
        target: 'http://localhost:8080',  // Backend URL
        changeOrigin: true,
        secure: false,
        // rewrite: (path) => path.replace(/^\/api/, ''),  // Optional path rewrite
      }
    },

    // CORS settings for dev server
    cors: true,
  },

  // Build settings
  build: {
    outDir: 'dist',          // Output directory
    sourcemap: false,        // Enable for debugging
    minify: 'terser',        // or 'esbuild'
    target: 'es2015',        // Browser compatibility

    // Chunk splitting for better caching
    rollupOptions: {
      output: {
        manualChunks: {
          vendor: ['react', 'react-dom', 'react-router-dom'],
          api: ['axios'],
        },
      },
    },
  },
})
```

#### Production Build Configuration

```typescript
export default defineConfig({
  // Production-specific settings
  base: '/',  // Base public path (change for subdirectory deployment)

  build: {
    outDir: 'dist',
    assetsDir: 'assets',
    sourcemap: false,        // Disable sourcemaps in production
    minify: 'terser',

    // Advanced optimization
    terserOptions: {
      compress: {
        drop_console: true,  // Remove console.log in production
        drop_debugger: true,
      },
    },

    // Size warnings
    chunkSizeWarningLimit: 1000,  // KB
  },
})
```

### API URL Configuration

The frontend communicates with the backend API. Configure the API URL based on deployment:

#### Development Setup

In `frontend/src/services/api.ts`:

```typescript
import axios from 'axios';

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || '/api/v1',
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 10000,  // 10 seconds
});
```

With Vite proxy in development:
- Frontend: `http://localhost:3000`
- API requests to `/api/v1/*` → proxied to `http://localhost:8080/api/v1/*`
- No CORS issues

#### Production Setup

**Option 1: Same Domain Deployment (Recommended)**

Deploy frontend and backend on the same domain using Nginx:

```nginx
server {
    listen 443 ssl;
    server_name yourdomain.com;

    # Frontend
    location / {
        root /var/www/frontend/dist;
        try_files $uri $uri/ /index.html;
    }

    # Backend API
    location /api/ {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

In this setup, use relative URL:
```typescript
baseURL: '/api/v1'  // No CORS needed
```

**Option 2: Separate Domains**

Frontend: `https://app.yourdomain.com`
Backend: `https://api.yourdomain.com`

Configure API URL:
```typescript
baseURL: 'https://api.yourdomain.com/api/v1'
```

**Important:** Backend must allow CORS from frontend domain.

**Option 3: Environment-Based Configuration**

```typescript
const getApiUrl = () => {
  if (import.meta.env.DEV) {
    return '/api/v1';  // Development proxy
  }
  return import.meta.env.VITE_API_URL || '/api/v1';  // Production
};

const api = axios.create({
  baseURL: getApiUrl(),
});
```

### Build Configuration

#### TypeScript Configuration

File: `frontend/tsconfig.json`

```json
{
  "compilerOptions": {
    "target": "ES2020",
    "useDefineForClassFields": true,
    "lib": ["ES2020", "DOM", "DOM.Iterable"],
    "module": "ESNext",
    "skipLibCheck": true,

    /* Bundler mode */
    "moduleResolution": "bundler",
    "allowImportingTsExtensions": true,
    "resolveJsonModule": true,
    "isolatedModules": true,
    "noEmit": true,
    "jsx": "react-jsx",

    /* Linting */
    "strict": true,
    "noUnusedLocals": true,
    "noUnusedParameters": true,
    "noFallthroughCasesInSwitch": true
  },
  "include": ["src"],
  "references": [{ "path": "./tsconfig.node.json" }]
}
```

#### Package.json Scripts

File: `frontend/package.json`

```json
{
  "scripts": {
    "dev": "vite",
    "build": "tsc && vite build",
    "preview": "vite preview",
    "lint": "eslint . --ext ts,tsx",
    "type-check": "tsc --noEmit"
  }
}
```

**Script descriptions:**
- `npm run dev` - Start development server with HMR
- `npm run build` - Type check + production build
- `npm run preview` - Preview production build locally
- `npm run lint` - Run ESLint for code quality
- `npm run type-check` - TypeScript type checking without building

#### Build Optimization

**Code Splitting:**

```typescript
// Lazy load routes for better initial load time
import { lazy, Suspense } from 'react';

const HomePage = lazy(() => import('./pages/HomePage'));
const ServicesPage = lazy(() => import('./pages/ServicesPage'));

function App() {
  return (
    <Suspense fallback={<div>Loading...</div>}>
      <Routes>
        <Route path="/" element={<HomePage />} />
        <Route path="/services" element={<ServicesPage />} />
      </Routes>
    </Suspense>
  );
}
```

**Asset Optimization:**

```typescript
// vite.config.ts
export default defineConfig({
  build: {
    // Image optimization
    assetsInlineLimit: 4096,  // Inline assets < 4KB as base64

    // CSS code splitting
    cssCodeSplit: true,

    // Rollup options
    rollupOptions: {
      output: {
        // Separate chunks for better caching
        assetFileNames: 'assets/[name]-[hash][extname]',
        chunkFileNames: 'assets/[name]-[hash].js',
        entryFileNames: 'assets/[name]-[hash].js',
      },
    },
  },
})
```

### Runtime Configuration

Unlike backend configuration, frontend environment variables are embedded at build time. For runtime configuration:

#### Option 1: API Endpoint Discovery

Fetch configuration from backend on app load:

```typescript
// src/config.ts
interface AppConfig {
  apiUrl: string;
  features: {
    oauth: boolean;
    // ... other feature flags
  };
}

export async function loadConfig(): Promise<AppConfig> {
  // For static builds, use embedded env vars
  if (import.meta.env.VITE_API_URL) {
    return {
      apiUrl: import.meta.env.VITE_API_URL,
      features: { oauth: true },
    };
  }

  // Or fetch from backend
  const response = await fetch('/api/v1/config');
  return response.json();
}
```

#### Option 2: Build-Time Configuration Per Environment

Create multiple build commands:

```json
{
  "scripts": {
    "build:dev": "vite build --mode development",
    "build:staging": "vite build --mode staging",
    "build:prod": "vite build --mode production"
  }
}
```

With corresponding `.env` files:
- `.env.development`
- `.env.staging`
- `.env.production`

#### Option 3: Configuration File

Create `public/config.js` that's loaded at runtime:

```javascript
// public/config.js (not bundled)
window.APP_CONFIG = {
  apiUrl: 'https://api.yourdomain.com/api/v1',
  environment: 'production',
};
```

Load in `index.html`:
```html
<script src="/config.js"></script>
```

Access in app:
```typescript
const apiUrl = (window as any).APP_CONFIG?.apiUrl || '/api/v1';
```

### Frontend Environment Checklist

Before deploying frontend to production:

- [ ] Set correct `VITE_API_URL` for your environment
- [ ] Verify API URL is reachable from frontend
- [ ] Configure CORS in backend for frontend domain
- [ ] Test OAuth flow with production redirect URLs
- [ ] Enable production build optimizations
- [ ] Disable source maps in production (or secure them)
- [ ] Configure CDN for static assets (optional)
- [ ] Set up HTTPS/SSL certificate
- [ ] Configure SPA routing (try_files in Nginx)
- [ ] Test build locally: `npm run build && npm run preview`
- [ ] Verify all environment-specific features work

## Security Best Practices

### Environment-Specific Configuration

Use different configurations for each environment:

**Development (.env.dev):**
```env
DATABASE_URL=jdbc:postgresql://localhost:5432/community_services_dev
JWT_SECRET=dev-secret-do-not-use-in-production
OAUTH_REDIRECT_URL=http://localhost:8080/api/v1/auth/callback
```

**Production (.env.prod):**
```env
DATABASE_URL=jdbc:postgresql://prod-db.example.com:5432/community_services
JWT_SECRET=<strong-random-secret-48-chars-minimum>
OAUTH_REDIRECT_URL=https://api.yourdomain.com/api/v1/auth/callback
```

### Secrets Management

**Never commit secrets to version control:**

```bash
# Add to .gitignore
.env
.env.*
*.env
application-prod.yaml
secrets/
```

**Use secret management services in production:**

- AWS Secrets Manager
- Google Cloud Secret Manager
- HashiCorp Vault
- Kubernetes Secrets
- Azure Key Vault

### Database Security

- Use strong passwords
- Limit database user privileges (no need for SUPERUSER)
- Enable SSL connections in production
- Restrict database network access (firewall rules)
- Regular security updates

```sql
-- Create restricted database user
CREATE USER community_app WITH PASSWORD 'strong_password';
GRANT CONNECT ON DATABASE community_services TO community_app;
GRANT USAGE ON SCHEMA public TO community_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO community_app;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO community_app;
```

### JWT Security

- Use strong random secrets (48+ characters)
- Rotate secrets periodically
- Set reasonable expiration times (30 days default)
- Validate all token claims
- Never log JWT tokens

### HTTPS/TLS

- Always use HTTPS in production
- Configure TLS termination at reverse proxy (Nginx, ALB, Cloud Load Balancer)
- Use HTTP/2 for better performance
- Enable HSTS headers

### Security Headers

Configure via reverse proxy or add to application:

```
Strict-Transport-Security: max-age=31536000; includeSubDomains
X-Frame-Options: SAMEORIGIN
X-Content-Type-Options: nosniff
X-XSS-Protection: 1; mode=block
Content-Security-Policy: default-src 'self'
```

### Rate Limiting

Consider implementing rate limiting for production:

- Per IP address
- Per user/token
- Per endpoint

Use reverse proxy (Nginx) or API gateway for this.

### Monitoring Secrets

Monitor for exposed secrets:

- Use GitHub secret scanning
- Use tools like truffleHog, git-secrets
- Implement pre-commit hooks

## Configuration Checklist

Before deploying to production:

- [ ] All environment variables set
- [ ] Strong JWT_SECRET generated (48+ chars)
- [ ] Strong database password
- [ ] OAuth2 credentials obtained and tested
- [ ] OAUTH_REDIRECT_URL set to production URL (HTTPS)
- [ ] CORS configured with specific allowed origins
- [ ] HTTPS/TLS configured
- [ ] Database backups configured
- [ ] Log level set to INFO or WARN
- [ ] Connection pool tuned for expected load
- [ ] Health check endpoint tested
- [ ] Monitoring and alerting configured
- [ ] Secrets stored securely (not in code)
- [ ] .env file not committed to version control
- [ ] Security headers configured
- [ ] Rate limiting considered

---

**Version:** 1.0.0
**Last Updated:** 2025-12-24
