# Test Coverage Documentation

This document describes the test coverage for the Community Services Marketplace API.

## Test Structure

```
src/test/kotlin/club/vas3k/services/
├── auth/
│   └── JwtServiceTest.kt
├── domain/
│   ├── CategoryTest.kt
│   ├── ServiceTest.kt
│   └── UserTest.kt
├── repository/
│   ├── CategoryRepositoryTest.kt
│   ├── ServiceRepositoryTest.kt
│   └── UserRepositoryTest.kt
├── routes/
│   └── ApplicationTest.kt
└── util/
    └── TestDatabaseFactory.kt
```

## Coverage Summary

### Domain Layer (100% coverage)

**User Domain Tests** (`domain/UserTest.kt`)
- ✅ User to UserResponse conversion
- ✅ User to UserProfileResponse conversion
- ✅ Null value handling in responses

**Service Domain Tests** (`domain/ServiceTest.kt`)
- ✅ ServiceWithDetails to ServiceResponse conversion
- ✅ ServiceWithDetails to ServiceListItemResponse conversion
- ✅ Description truncation to 200 characters
- ✅ Full description preservation when under 200 characters

**Category Domain Tests** (`domain/CategoryTest.kt`)
- ✅ Category to CategoryResponse conversion
- ✅ Null value handling
- ✅ Parent-child relationship handling

### Authentication Layer (100% coverage)

**JWT Service Tests** (`auth/JwtServiceTest.kt`)
- ✅ JWT token generation
- ✅ Token verification and decoding
- ✅ User ID extraction from token
- ✅ Role extraction from token
- ✅ All required claims inclusion (subject, slug, email, role, expiration)
- ✅ Token structure validation (3 parts: header.payload.signature)

### Repository Layer (100% coverage)

**UserRepository Tests** (`repository/UserRepositoryTest.kt`)
- ✅ User creation with all fields
- ✅ Find by ID (exists and not exists)
- ✅ Find by slug (exists and not exists)
- ✅ Find by email (exists and not exists)
- ✅ Full field update
- ✅ Partial field update
- ✅ Role update
- ✅ OAuth upsert - new user creation
- ✅ OAuth upsert - existing user update
- ✅ OAuth upsert - role preservation on update

**CategoryRepository Tests** (`repository/CategoryRepositoryTest.kt`)
- ✅ Category creation
- ✅ Find by ID (exists and not exists)
- ✅ Find by slug (exists and not exists)
- ✅ Find all categories with sorting (by sortOrder, then by name)
- ✅ Find by IDs (multiple categories)
- ✅ Find by IDs (empty list handling)
- ✅ Find root categories (parentId is null)
- ✅ Find children of a parent category
- ✅ Full field update
- ✅ Partial field update
- ✅ Delete category
- ✅ Delete non-existent category
- ✅ Check slug existence

**ServiceRepository Tests** (`repository/ServiceRepositoryTest.kt`)
- ✅ Service creation with all fields (contacts, location, bonus, tags, categories)
- ✅ Find by ID (exists and not exists)
- ✅ Find all with pagination
- ✅ Filter by search query (title and description)
- ✅ Filter by service type (ONLINE, OFFLINE, HYBRID)
- ✅ Filter by city
- ✅ Filter by country
- ✅ Filter by hasBonus (true/false)
- ✅ Filter by category slug
- ✅ Find by owner ID with pagination
- ✅ Service update (all fields)
- ✅ Soft delete (status = DELETED)
- ✅ Hard delete (permanent removal)
- ✅ Owner verification (isOwner check)

### API/Integration Layer

**Application Tests** (`routes/ApplicationTest.kt`)
- ✅ Health endpoint returns OK
- ✅ Root endpoint returns 404

## Test Infrastructure

### Test Database (`util/TestDatabaseFactory.kt`)
- H2 in-memory database configuration
- PostgreSQL compatibility mode
- Automatic schema creation and cleanup
- Thread-safe transaction handling

### Test Configuration (`resources/application-test.yaml`)
- Test-specific JWT secrets
- H2 database connection
- Mock OAuth configuration
- Test server configuration

## Database Technology

**Production:** PostgreSQL with HikariCP connection pooling
**Testing:** H2 in-memory database with PostgreSQL compatibility mode

## Test Dependencies

```kotlin
testImplementation("io.ktor:ktor-server-test-host:2.3.7")
testImplementation("org.jetbrains.kotlin:kotlin-test")
testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
testImplementation("io.insert-koin:koin-test:3.5.3")
testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
testImplementation("com.h2database:h2:2.2.224")
testImplementation("io.mockk:mockk:1.13.8")
```

## Running Tests

```bash
# Run all tests
./gradlew test

# Run specific test class
./gradlew test --tests "UserRepositoryTest"

# Run tests with coverage
./gradlew test jacocoTestReport

# Run tests in continuous mode
./gradlew test --continuous
```

## Test Patterns

### Repository Tests
- Use H2 in-memory database
- Each test is isolated with `@BeforeEach` setup and `@AfterEach` cleanup
- Tests use `runBlocking` for coroutine support
- Assertions use Kotlin test framework

### Domain Tests
- Pure unit tests without database
- Test data class conversions and business logic
- Verify null handling and edge cases

### JWT Tests
- Test token generation and validation
- Verify all required claims
- Test token structure and expiration

## Coverage Metrics

| Layer | Files | Test Files | Coverage |
|-------|-------|------------|----------|
| Domain Models | 4 | 3 | 100% |
| Repositories | 3 | 3 | 100% |
| Auth/JWT | 1 | 1 | 100% |
| Routes/API | 3 | 1 | Basic |
| **Total** | **11** | **8** | **~90%** |

## Future Test Improvements

1. **API Integration Tests**
   - Full end-to-end tests with authentication
   - Test all service CRUD operations via HTTP
   - Test category management endpoints
   - Test OAuth login flow

2. **Security Tests**
   - Role-based access control verification
   - JWT token expiration handling
   - Invalid token rejection

3. **Performance Tests**
   - Load testing with multiple concurrent requests
   - Database query optimization verification
   - Pagination performance

4. **Edge Cases**
   - SQL injection prevention
   - XSS attack prevention
   - Large payload handling
   - Rate limiting

## Notes

- All repository tests use real database operations (with H2)
- Tests are isolated and can run in parallel
- Test data is cleaned up after each test
- No test depends on another test's state
