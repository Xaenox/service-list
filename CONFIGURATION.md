# Configuration Guide

This guide provides detailed information about all configuration options for the Community Services Marketplace API.

## Table of Contents

- [Configuration Files](#configuration-files)
- [Environment Variables](#environment-variables)
- [Application Configuration](#application-configuration)
- [Database Configuration](#database-configuration)
- [OAuth2 Configuration](#oauth2-configuration)
- [JWT Configuration](#jwt-configuration)
- [Server Configuration](#server-configuration)
- [Logging Configuration](#logging-configuration)
- [CORS Configuration](#cors-configuration)
- [Security Best Practices](#security-best-practices)

## Configuration Files

The application uses multiple configuration sources with the following priority (highest to lowest):

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

## Environment Variables

### Required Environment Variables

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

### Optional Environment Variables

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

### Setting Environment Variables

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

## Application Configuration

### application.yaml Structure

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

### Modifying application.yaml

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

## Database Configuration

### Connection Settings

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

### Advanced Connection Pool Settings

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

### Connection String Options

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

### Database Schema Management

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

### Manual Schema Management

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

### Database Backup and Restore

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

## OAuth2 Configuration

The application integrates with vas3k.club for authentication using OAuth2.

### OAuth2 Settings

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

### Obtaining OAuth2 Credentials

1. Register your application at vas3k.club
2. Configure authorized redirect URIs:
   - Development: `http://localhost:8080/api/v1/auth/callback`
   - Production: `https://yourdomain.com/api/v1/auth/callback`
3. Obtain Client ID and Client Secret
4. Set environment variables

### OAuth2 Flow

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

### User Validation

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

### Customizing OAuth2 Endpoints

If vas3k.club endpoints change, update `application.yaml`:

```yaml
oauth:
  vas3k:
    authorizeUrl: https://new-domain.com/oauth/authorize
    accessTokenUrl: https://new-domain.com/oauth/token
    userInfoUrl: https://new-domain.com/api/user
```

## JWT Configuration

JSON Web Tokens (JWT) are used for API authentication after OAuth2 login.

### JWT Settings

```yaml
jwt:
  secret: ${JWT_SECRET}
  issuer: community-services
  audience: community-services-api
  realm: Community Services
  expirationDays: 30
```

### JWT Secret Generation

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

### JWT Token Structure

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

### Token Expiration

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

### Authentication Strategies

The application uses multiple authentication strategies:

| Strategy | Description | Used For |
|----------|-------------|----------|
| `jwt` | Requires valid JWT token | Most API endpoints |
| `jwt-moderator` | Requires MODERATOR or ADMIN role | Service moderation |
| `jwt-admin` | Requires ADMIN role only | Category management |
| `jwt-optional` | Optional authentication | Public service listing |

### Using JWT Tokens

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

### Token Validation

Tokens are validated for:
- Valid signature (using JWT_SECRET)
- Correct issuer and audience
- Not expired
- User exists in database

## Server Configuration

### HTTP Server Settings

```yaml
ktor:
  deployment:
    port: 8080        # Server port
    host: 0.0.0.0     # Bind address (0.0.0.0 = all interfaces)
```

### Port Configuration

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

### Host Configuration

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

### JVM Configuration

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

## Logging Configuration

### Log Levels

Logging is configured via Logback. Default configuration in `src/main/resources/logback.xml`.

**Available log levels:**
- `TRACE` - Very detailed
- `DEBUG` - Debug information
- `INFO` - General information (default)
- `WARN` - Warning messages
- `ERROR` - Error messages only

### Configuring Log Level

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

### Request Logging

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

### Database Query Logging

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

## CORS Configuration

Cross-Origin Resource Sharing (CORS) is configured to allow frontend applications to access the API.

### Current CORS Settings

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

### Production CORS Configuration

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

### Custom Headers

To allow additional headers:

```kotlin
allowHeader("X-Custom-Header")
allowHeader("X-Request-ID")
```

### Preflight Request Caching

Configure max age for preflight requests:

```kotlin
maxAgeInSeconds = 3600 // Cache for 1 hour
```

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
