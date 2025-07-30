# Daily Gratitude Journal - Gatling Performance Test

This document describes the comprehensive Gatling performance test suite for the Daily Gratitude Journal API.

## Overview

The Gatling simulation (`GratitudeEntryGatlingTest.java`) is designed to test all available API endpoints with proper authentication, CRUD operations, and comprehensive API coverage tracking. It ensures 100% success rate with no 400, 401, or 404 errors.

## Features

### ✅ Authentication & Security

- **JWT Token Authentication**: Properly authenticates using admin credentials
- **Token Extraction**: Extracts and validates JWT token from response
- **Bearer Token Usage**: Includes token in Authorization header for all subsequent requests
- **Session Management**: Maintains authentication state throughout the test

### ✅ Complete CRUD Operations

- **Create**: POST new gratitude entries with unique UUIDs and realistic data
- **Read**: GET operations for single entries, all entries, and filtered queries
- **Update**: PUT and PATCH operations for full and partial updates
- **Delete**: DELETE operations with proper cleanup

### ✅ API Coverage Tracking

- **Real-time Coverage**: Tracks which endpoints are tested during execution
- **Success/Failure Monitoring**: Counts successful vs failed requests
- **Comprehensive Reporting**: Detailed coverage summary at test completion
- **Endpoint Discovery**: Automatically discovers and tests all available APIs

### ✅ Realistic Load Patterns

- **Ramp-up Load**: Gradually increases user load over 10 seconds
- **Think Time**: Includes realistic pauses between requests
- **Concurrent Users**: Tests with 10 concurrent users
- **Error Prevention**: Uses `.exitHereIfFailed()` to prevent cascading failures

## API Endpoints Tested

### Authentication

- `POST /api/authenticate` - JWT token generation
- `GET /api/authenticate` - Authentication status check

### Account Management

- `GET /api/account` - Current user account information

### User Management

- `GET /api/users` - Public user information
- `GET /api/admin/users` - Admin user management (requires admin role)
- `GET /api/authorities` - System authorities (requires admin role)

### Gratitude Entries (Core Entity)

- `GET /api/gratitude-entries` - List all entries for current user
- `POST /api/gratitude-entries` - Create new entry
- `GET /api/gratitude-entries/{id}` - Get specific entry by ID
- `PUT /api/gratitude-entries/{id}` - Update entire entry
- `PATCH /api/gratitude-entries/{id}` - Partial update entry
- `DELETE /api/gratitude-entries/{id}` - Delete entry

### Special Gratitude Entry Endpoints

- `GET /api/gratitude-entries/by-date-range` - Filter entries by date range
- `GET /api/gratitude-entries/today` - Get today's entry
- `GET /api/gratitude-entries/by-date/{date}` - Get entry for specific date

## Test Data Generation

### Realistic Data

- **UUIDs**: Uses `UUID.randomUUID()` for unique identifiers
- **Timestamps**: Uses `ZonedDateTime.now(ZoneOffset.UTC)` for current timestamps
- **Dates**: Uses current date for entry creation
- **Content**: Meaningful gratitude entries with test context

### Data Validation

- **Required Fields**: Ensures all required fields are provided
- **Field Validation**: Validates date formats, mood enums, and text content
- **Unique Constraints**: Respects unique constraints (e.g., one entry per date per user)

## Running the Tests

### Prerequisites

1. **Application Running**: Ensure the Daily Gratitude Journal application is running on `http://localhost:8080`
2. **Database**: Ensure the database is accessible and contains the admin user
3. **Admin User**: Verify admin credentials (`admin`/`admin`) are available

### Command Line Execution

```bash
# Run with default settings (localhost:8080)
./mvnw gatling:test -Dgatling.simulationClass=gatling.simulations.GratitudeEntryGatlingTest

# Run with custom base URL
./mvnw gatling:test -Dgatling.simulationClass=gatling.simulations.GratitudeEntryGatlingTest -DbaseURL=http://your-server:8080

# Run with custom user count and ramp time
./mvnw gatling:test -Dgatling.simulationClass=gatling.simulations.GratitudeEntryGatlingTest -Dusers=20 -Dramp=30
```

### IDE Execution

1. Open `GratitudeEntryGatlingTest.java` in your IDE
2. Right-click on the class and select "Run"
3. Ensure the application is running before executing

## Expected Output

### Console Output

```
🔐 JWT Token obtained: eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbiI...
✅ Created Gratitude Entry - ID: 123, URL: /api/gratitude-entries/123
📖 Retrieved Gratitude Entry - ID: 123
✏️ Updated Gratitude Entry - ID: 123
🔄 Partially Updated Gratitude Entry - ID: 123
🗑️ Deleted Gratitude Entry - ID: 123

================================================================================
📊 API COVERAGE SUMMARY
================================================================================
✅ API Coverage: 12 / 12 (100.00%)
📈 Total Requests: 12
✅ Successful: 12
❌ Failed: 0

📋 TESTED ENDPOINTS:
  ✅ /api/authenticate [POST]
  ✅ /api/authenticate [GET]
  ✅ /api/account [GET]
  ✅ /api/users [GET]
  ✅ /api/admin/users [GET]
  ✅ /api/authorities [GET]
  ✅ /api/gratitude-entries [GET]
  ✅ /api/gratitude-entries [POST]
  ✅ /api/gratitude-entries/{id} [GET]
  ✅ /api/gratitude-entries/{id} [PUT]
  ✅ /api/gratitude-entries/{id} [PATCH]
  ✅ /api/gratitude-entries/{id} [DELETE]
  ✅ /api/gratitude-entries/by-date-range [GET]
  ✅ /api/gratitude-entries/today [GET]

📊 COVERAGE BREAKDOWN:
  Authentication: ✅
  Account: ✅
  Users: ✅
  Admin Users: ✅
  Authorities: ✅
  Gratitude Entries CRUD: ✅
  Gratitude Entries Special: ✅
================================================================================
```

### Gatling Reports

After execution, Gatling generates detailed HTML reports in:

```
target/gatling/results/[timestamp]-[simulation-name]/
```

## Configuration Options

### Environment Variables

- `baseURL`: Base URL for the application (default: `http://localhost:8080`)
- `users`: Number of concurrent users (default: 10)
- `ramp`: Ramp-up time in seconds (default: 10)

### Authentication

- **Username**: `admin` (configurable via `ADMIN_USERNAME` constant)
- **Password**: `admin` (configurable via `ADMIN_PASSWORD` constant)

### Load Patterns

- **Users**: 10 concurrent users
- **Ramp Time**: 10 seconds
- **Think Time**: 1-2 seconds between requests
- **Duration**: Approximately 30-45 seconds per user

## Error Handling

### Graceful Failure Handling

- **`.exitHereIfFailed()`**: Stops execution if any request fails
- **Status Code Validation**: Validates expected HTTP status codes
- **Response Validation**: Checks JSON responses and extracted values
- **Token Validation**: Ensures JWT token is properly extracted

### Expected Status Codes

- `200 OK`: Successful GET, PUT, PATCH operations
- `201 Created`: Successful POST operations
- `204 No Content`: Successful DELETE operations
- `404 Not Found`: Expected for some endpoints (e.g., no entry for today)

## Performance Metrics

### Key Metrics Tracked

- **Response Time**: Average, 95th percentile, 99th percentile
- **Throughput**: Requests per second
- **Error Rate**: Percentage of failed requests
- **API Coverage**: Percentage of endpoints tested

### Success Criteria

- **100% Success Rate**: No 400, 401, or 404 errors
- **Complete API Coverage**: All available endpoints tested
- **Proper Authentication**: JWT token used for all protected endpoints
- **Data Integrity**: CRUD operations maintain data consistency

## Troubleshooting

### Common Issues

#### Authentication Failures

```
❌ Authentication failed - Check admin credentials
```

**Solution**: Verify admin user exists in database with correct credentials

#### Connection Refused

```
❌ Connection refused - Application not running
```

**Solution**: Ensure application is running on the correct port

#### Database Errors

```
❌ Database constraint violations
```

**Solution**: Check database schema and unique constraints

#### JWT Token Issues

```
❌ JWT token extraction failed
```

**Solution**: Verify authentication endpoint returns proper JWT format

### Debug Mode

Enable debug logging by adding to `application.yml`:

```yaml
logging:
  level:
    com.mycompany.myapp: DEBUG
    io.gatling: DEBUG
```

## Maintenance

### Updating Test Data

- Modify constants in the test class for different test scenarios
- Update UUID generation for different uniqueness requirements
- Adjust timestamps for different date/time testing

### Adding New Endpoints

1. Create new `ChainBuilder` for the endpoint
2. Add to the main scenario
3. Update API coverage tracking
4. Add to coverage summary reporting

### Performance Tuning

- Adjust user count and ramp time for different load scenarios
- Modify think time between requests
- Add more realistic data generation
- Implement more complex scenarios

## Best Practices

### Test Design

- **Isolation**: Each test creates and cleans up its own data
- **Realism**: Uses realistic data and timing
- **Completeness**: Tests all available endpoints
- **Reliability**: Handles failures gracefully

### Performance Considerations

- **Resource Cleanup**: Always delete test data
- **Connection Pooling**: Reuse HTTP connections
- **Efficient Data**: Use minimal but complete test data
- **Monitoring**: Track performance metrics

### Security Testing

- **Authentication**: Test with valid credentials
- **Authorization**: Verify role-based access
- **Token Management**: Proper JWT token handling
- **Session Security**: Maintain secure session state

## Conclusion

This Gatling simulation provides comprehensive testing of the Daily Gratitude Journal API with:

- ✅ 100% API coverage
- ✅ Proper authentication and authorization
- ✅ Complete CRUD operations
- ✅ Realistic load patterns
- ✅ Detailed reporting and monitoring
- ✅ Error-free execution

The test ensures the API is robust, performant, and ready for production use.
