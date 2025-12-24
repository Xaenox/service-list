# Community Services Marketplace

A full-stack platform for vas3k.club community members to list and discover services offered by fellow members. Includes a Kotlin/Ktor backend API and a modern React frontend.

## Overview

This project consists of two main components:

### Backend (Kotlin + Ktor)
A robust REST API built with Ktor framework that provides:

- **User Authentication**: OAuth2 integration with vas3k.club
- **Service Management**: CRUD operations for community services (online, offline, hybrid)
- **Category Management**: Hierarchical service categorization
- **Role-Based Access Control**: User, Moderator, and Admin roles
- **Service Discovery**: Search, filtering, and pagination

### Frontend (React + TypeScript)
A modern, responsive web interface inspired by vas3k.club design:

- **Service Browsing**: Browse and search services with filters
- **User Interface**: Clean, card-based layout with sidebar navigation
- **OAuth Integration**: Seamless authentication flow
- **Responsive Design**: Works on desktop and mobile devices

See [frontend/README.md](frontend/README.md) for detailed frontend documentation.

## Features

### Backend
- JWT-based API authentication
- PostgreSQL database with Exposed ORM
- RESTful API design
- Multi-tier authorization (User/Moderator/Admin)
- Service types: Online, Offline, Hybrid
- Hierarchical categories
- Comprehensive filtering and search
- Docker support
- OAuth2 authentication with vas3k.club

### Frontend
- React 18 with TypeScript
- Vite for fast development
- Custom CSS inspired by vas3k.club
- Client-side routing with React Router
- API integration with Axios

## Tech Stack

### Backend
- **Language**: Kotlin 1.9.22
- **Framework**: Ktor 2.3.7
- **Database**: PostgreSQL 16
- **ORM**: Exposed 0.46.0
- **Authentication**: JWT + OAuth2
- **DI**: Koin 3.5.3
- **Build**: Gradle 8.5

### Frontend
- **Framework**: React 18
- **Language**: TypeScript 5
- **Build Tool**: Vite 5
- **Routing**: React Router 6
- **HTTP Client**: Axios

## Quick Start

### Using Docker Compose (Recommended)

1. **Clone the repository**:
   ```bash
   git clone <repository-url>
   cd service-list
   ```

2. **Configure environment**:
   ```bash
   cp .env.example .env
   # Edit .env and set your configuration
   ```

3. **Start the services**:
   ```bash
   docker-compose up -d
   ```

4. **Verify backend is running**:
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

5. **Start the frontend** (in a new terminal):
   ```bash
   cd frontend
   npm install
   npm run dev
   ```

   The frontend will be available at `http://localhost:3000`

### Manual Setup

#### Backend

1. **Prerequisites**:
   - JDK 17+
   - PostgreSQL 16+
   - Gradle 8.5+

2. **Build the application**:
   ```bash
   ./gradlew build
   ```

3. **Configure environment variables** (see [Configuration Guide](CONFIGURATION.md))

4. **Run the application**:
   ```bash
   java -jar build/libs/community-services-all.jar
   ```

#### Frontend

1. **Prerequisites**:
   - Node.js 18+
   - npm or yarn

2. **Install dependencies**:
   ```bash
   cd frontend
   npm install
   ```

3. **Start development server**:
   ```bash
   npm run dev
   ```

   The frontend will be available at `http://localhost:3000`

4. **Build for production**:
   ```bash
   npm run build
   ```

   See [frontend/README.md](frontend/README.md) for detailed frontend setup and deployment instructions.

## Documentation

- **[Frontend README](frontend/README.md)** - Frontend documentation
  - Setup and installation
  - Project structure
  - Development guide
  - Deployment instructions
  - API integration

- **[Deployment Guide](DEPLOYMENT.md)** - Backend deployment instructions
  - Docker Compose deployment
  - Manual deployment
  - Production deployment with Nginx
  - Cloud platform deployment (AWS, GCP, Kubernetes)
  - Monitoring and maintenance
  - Troubleshooting

- **[Configuration Guide](CONFIGURATION.md)** - Backend configuration options
  - Environment variables
  - Database configuration
  - OAuth2 setup
  - JWT configuration
  - Security best practices

## API Endpoints

### Health Check

```
GET /health
```

Returns server health status.

### Authentication

```
GET  /api/v1/auth/login           # Initiate OAuth2 login
GET  /api/v1/auth/callback        # OAuth2 callback handler
GET  /api/v1/auth/me              # Get current user (requires authentication)
```

### Services

```
GET    /api/v1/services           # List services (with filters and pagination)
POST   /api/v1/services           # Create service (requires authentication)
GET    /api/v1/services/:id       # Get service by ID
PUT    /api/v1/services/:id       # Update service (owner or moderator)
DELETE /api/v1/services/:id       # Delete service (owner or admin)
```

**Query parameters for listing**:
- `page` - Page number (default: 1)
- `limit` - Items per page (default: 20, max: 100)
- `type` - Filter by type: ONLINE, OFFLINE, HYBRID
- `status` - Filter by status: ACTIVE, INACTIVE, DELETED
- `category` - Filter by category slug
- `country` - Filter by country
- `city` - Filter by city
- `search` - Search in title and description

### Categories

```
GET    /api/v1/categories         # List all categories
POST   /api/v1/categories         # Create category (admin only)
GET    /api/v1/categories/:id     # Get category by ID
PUT    /api/v1/categories/:id     # Update category (admin only)
DELETE /api/v1/categories/:id     # Delete category (admin only)
```

## Authentication Flow

1. User navigates to `/api/v1/auth/login?return_url=<frontend_url>`
2. Server redirects to vas3k.club OAuth2 authorization
3. User authenticates on vas3k.club
4. vas3k.club redirects back to `/api/v1/auth/callback`
5. Server validates and creates/updates user
6. Server generates JWT token
7. Server redirects to `return_url` with JWT token

## Authorization Levels

- **USER** (default): Can create and manage own services
- **MODERATOR**: Can edit/delete any service
- **ADMIN**: Full access including category management

To promote users to moderator or admin:
```sql
UPDATE users SET role = 'MODERATOR' WHERE slug = 'username';
UPDATE users SET role = 'ADMIN' WHERE slug = 'username';
```

## Environment Variables

Required variables:

```env
DATABASE_URL=jdbc:postgresql://localhost:5432/community_services
DATABASE_USER=postgres
DATABASE_PASSWORD=secure_password
VAS3K_CLIENT_ID=your_vas3k_client_id
VAS3K_CLIENT_SECRET=your_vas3k_client_secret
OAUTH_REDIRECT_URL=http://localhost:8080/api/v1/auth/callback
JWT_SECRET=your-super-secret-jwt-key-minimum-32-characters
```

See [Configuration Guide](CONFIGURATION.md) for all options.

## Development

### Running Tests

```bash
./gradlew test
```

### Building

```bash
./gradlew build
```

The fat JAR will be created at `build/libs/community-services-all.jar`

### Code Structure

```
src/main/kotlin/club/vas3k/services/
├── Application.kt           # Entry point
├── auth/                    # Authentication & OAuth2
├── database/                # Database configuration & schema
├── domain/                  # Data models and DTOs
├── repository/              # Data access layer
├── routes/                  # API route handlers
├── plugins/                 # Ktor plugins (routing, serialization, etc.)
└── di/                      # Dependency injection setup
```

### Database Schema

The application manages these tables:

- **users** - User accounts from vas3k.club
- **categories** - Hierarchical service categories
- **services** - Service listings
- **service_categories** - Many-to-many relationship

Schema is auto-created on application startup.

## Docker

### Build Docker Image

```bash
docker build -t community-services:1.0.0 .
```

### Run with Docker

```bash
docker run -d \
  -p 8080:8080 \
  -e DATABASE_URL="jdbc:postgresql://host.docker.internal:5432/community_services" \
  -e DATABASE_USER="postgres" \
  -e DATABASE_PASSWORD="password" \
  -e VAS3K_CLIENT_ID="client_id" \
  -e VAS3K_CLIENT_SECRET="client_secret" \
  -e OAUTH_REDIRECT_URL="http://localhost:8080/api/v1/auth/callback" \
  -e JWT_SECRET="your-secret-key" \
  community-services:1.0.0
```

### Docker Compose

```bash
# Start
docker-compose up -d

# View logs
docker-compose logs -f app

# Stop
docker-compose down
```

## Deployment

See the [Deployment Guide](DEPLOYMENT.md) for detailed instructions on:

- Production deployment with systemd
- Nginx reverse proxy setup
- Cloud platform deployment (AWS ECS, Google Cloud Run, Kubernetes)
- Database backup and maintenance
- Monitoring and troubleshooting

## Security

- All passwords and secrets should be stored in environment variables
- JWT tokens expire after 30 days (configurable)
- OAuth2 integration requires active vas3k.club membership
- CORS is configured (update for production)
- Use HTTPS in production
- Regular database backups recommended

See [Configuration Guide](CONFIGURATION.md) for security best practices.

## Monitoring

### Health Check

```bash
curl http://localhost:8080/health
```

### Logs

**Docker Compose**:
```bash
docker-compose logs -f app
```

**Systemd**:
```bash
sudo journalctl -u community-services -f
```

### Database Monitoring

```sql
-- Active connections
SELECT count(*) FROM pg_stat_activity WHERE datname = 'community_services';

-- Table sizes
SELECT
    schemaname,
    tablename,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) AS size
FROM pg_tables
WHERE schemaname = 'public'
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;
```

## Troubleshooting

### Application won't start

1. Check logs for errors
2. Verify all environment variables are set
3. Ensure PostgreSQL is running and accessible
4. Verify database credentials

### OAuth2 authentication fails

1. Verify OAuth2 credentials are correct
2. Check OAUTH_REDIRECT_URL matches registered callback
3. Ensure user has active vas3k.club membership

### Database connection issues

1. Verify PostgreSQL is running
2. Check database credentials
3. Verify network connectivity
4. Check connection pool settings

See [Deployment Guide](DEPLOYMENT.md) for more troubleshooting tips.

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Run tests: `./gradlew test`
5. Submit a pull request

## License

[Add your license here]

## Support

For issues and questions:
- Check the [Deployment Guide](DEPLOYMENT.md)
- Check the [Configuration Guide](CONFIGURATION.md)
- Create an issue in the repository

---

**Version**: 1.0.0
**Built with**: Kotlin + Ktor + PostgreSQL
