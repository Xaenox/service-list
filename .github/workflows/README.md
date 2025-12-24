# GitHub Actions Workflows

This directory contains GitHub Actions workflows for CI/CD automation.

## Workflows Overview

### 1. CI Workflow (`ci.yml`)

Runs on every pull request and push to main branches.

**Jobs:**
- **Test**: Runs all unit and integration tests with PostgreSQL
- **Build**: Builds the application JAR file
- **Code Quality**: Runs code quality checks (ktlint, dependencies)

**Triggers:**
- Pull requests to `main`, `master`, or `develop`
- Direct pushes to `main`, `master`, or `develop`

### 2. Deploy Workflow (`deploy.yml`)

Automatically builds and pushes Docker images when code is merged to main.

**Jobs:**
- **Test**: Runs tests before deployment
- **Build and Push**: Builds Docker image and pushes to GitHub Container Registry

**Triggers:**
- Push to `main` or `master` branch
- Git tags starting with `v` (e.g., `v1.0.0`)

**Output:**
- Docker images pushed to `ghcr.io/<owner>/<repo>`
- Tagged with branch name, commit SHA, and `latest` for main branch

### 3. Security Workflow (`security.yml`)

Performs security scanning and vulnerability detection.

**Jobs:**
- **Dependency Check**: Scans dependencies for known vulnerabilities
- **CodeQL Analysis**: Static code analysis for security issues
- **Docker Security Scan**: Scans Docker images with Trivy

**Triggers:**
- Pull requests and pushes to main branches
- Weekly schedule (Mondays at 00:00 UTC)

## Setup Instructions

### Required Secrets

No additional secrets required for basic CI/CD! The workflows use `GITHUB_TOKEN` which is automatically provided.

### Optional Deployment Setup

To enable automatic deployment to a server, uncomment and configure the deployment job in `deploy.yml`:

#### Option 1: Deploy via SSH

Add these secrets to your repository:
- `DEPLOY_HOST`: Your server hostname or IP
- `DEPLOY_USER`: SSH username
- `DEPLOY_SSH_KEY`: Private SSH key for authentication
- `DEPLOY_URL`: Public URL for health checks

#### Option 2: Deploy to Google Cloud Run

Add these secrets:
- `GCP_SA_KEY`: Google Cloud service account JSON key
- `DATABASE_URL`: Production database connection string
- Configure Cloud Run and Cloud SQL as per DEPLOYMENT.md

### Docker Image Access

The workflows push images to GitHub Container Registry. To use these images:

1. **Authenticate with GitHub Container Registry:**
   ```bash
   echo $GITHUB_TOKEN | docker login ghcr.io -u <username> --password-stdin
   ```

2. **Pull the image:**
   ```bash
   docker pull ghcr.io/<owner>/<repo>:latest
   ```

3. **Use in docker-compose.yml:**
   ```yaml
   services:
     app:
       image: ghcr.io/<owner>/<repo>:latest
   ```

### Branch Protection Rules

Configure branch protection for `main` to require:
- ✅ Status checks to pass before merging
- ✅ Require branches to be up to date
- ✅ Require pull request reviews

Recommended status checks:
- `test` (CI Workflow)
- `build` (CI Workflow)
- `codeql-analysis` (Security Workflow)

### Monitoring Workflow Runs

1. Go to the **Actions** tab in your repository
2. View workflow runs and logs
3. Check test reports and build artifacts
4. Review security scan results

## Workflow Permissions

The workflows use minimal permissions:
- **CI**: Read repository content, upload artifacts
- **Deploy**: Read content + write packages (for Docker registry)
- **Security**: Read content + write security events (for CodeQL)

## Customization

### Changing Test Database

Edit the PostgreSQL service configuration in `ci.yml` and `deploy.yml`:

```yaml
services:
  postgres:
    image: postgres:16
    env:
      POSTGRES_DB: your_db_name
      POSTGRES_USER: your_user
      POSTGRES_PASSWORD: your_password
```

### Adding Additional Checks

Add new jobs or steps to `ci.yml`:

```yaml
jobs:
  lint:
    name: Kotlin Linting
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - run: ./gradlew ktlintCheck
```

### Changing Docker Registry

To use Docker Hub instead of GitHub Container Registry:

```yaml
env:
  REGISTRY: docker.io
  IMAGE_NAME: your-dockerhub-username/community-services
```

Then add Docker Hub credentials as secrets:
- `DOCKERHUB_USERNAME`
- `DOCKERHUB_TOKEN`

## Troubleshooting

### Tests Failing in CI

- Check PostgreSQL connection settings
- Verify environment variables match test requirements
- Review test logs in the Actions tab

### Docker Build Failures

- Ensure Dockerfile is valid
- Check if all required files are included in the build context
- Verify base image availability

### Security Scan Alerts

- Review the Security tab for vulnerability details
- Update dependencies with `./gradlew dependencyUpdates`
- Check Trivy reports for Docker image vulnerabilities

## Performance Tips

### Caching

Workflows use Gradle caching to speed up builds:
```yaml
uses: actions/setup-java@v4
with:
  cache: 'gradle'
```

### Parallel Execution

CI jobs run in parallel when possible. Test, build, and code quality checks execute concurrently.

### Docker Layer Caching

Deploy workflow uses GitHub Actions cache for Docker layers:
```yaml
cache-from: type=gha
cache-to: type=gha,mode=max
```

## Next Steps

1. **Enable branch protection** with required status checks
2. **Configure deployment** by uncommenting the deployment job
3. **Set up monitoring** for deployed applications
4. **Create release tags** to trigger versioned deployments
5. **Review security alerts** regularly in the Security tab

For deployment configuration details, see [DEPLOYMENT.md](../../DEPLOYMENT.md).
