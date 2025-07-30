# Gatling Test with Automatic Password Reset

This directory contains scripts that run the Gatling performance test and automatically reset the password back to the default after completion.

## Problem Solved

The Gatling test changes the admin password to `newpassword123` during testing. These scripts automatically reset it back to the default `admin` password after the test completes, so you don't have to manually reset it.

## Available Scripts

### Windows Users

1. **Batch Script (Recommended)**: `reset-password-after-gatling.bat`

   ```cmd
   reset-password-after-gatling.bat
   ```

2. **PowerShell Script**: `reset-password-after-gatling.ps1`
   ```powershell
   .\reset-password-after-gatling.ps1
   ```

### Linux/Mac Users

**Shell Script**: `reset-password-after-gatling.sh`

```bash
./reset-password-after-gatling.sh
```

## What These Scripts Do

1. **Step 1**: Run the Gatling performance test using `./mvnw gatling:test`
2. **Step 2**: Automatically reset the password back to default:
   - Authenticate with the test password (`newpassword123`)
   - Get JWT token
   - Change password back to default (`admin`)
   - Confirm success

## Default Credentials

After running any of these scripts, your application will have the default credentials:

- **Admin**: `admin` / `admin` (Primary account used in tests)
- **User**: `user` / `user` (Backup account)

## Prerequisites

- Your JHipster application must be running on `http://localhost:8080`
- The application must be accessible and healthy
- The admin account must exist in the database

## Troubleshooting

### If Password Reset Fails

If the automatic password reset fails, you can manually reset it:

1. **Authenticate with test password**:

   ```bash
   curl -X POST http://localhost:8080/api/authenticate \
     -H "Content-Type: application/json" \
     -d '{"username":"admin", "password":"newpassword123"}'
   ```

2. **Use the JWT token to reset password**:
   ```bash
   curl -X POST http://localhost:8080/api/account/change-password \
     -H "Content-Type: application/json" \
     -H "Authorization: Bearer YOUR_JWT_TOKEN" \
     -d '{"currentPassword":"newpassword123","newPassword":"admin"}'
   ```

### Common Issues

- **401 Unauthorized**: Application not running or wrong credentials
- **500 Internal Server Error**: Application configuration issue
- **Connection refused**: Application not started on port 8080

## Manual Alternative

If you prefer to run the test manually and reset the password yourself:

1. Run the test:

   ```bash
   ./mvnw gatling:test
   ```

2. Reset password manually using the steps above

## Files Created

- `reset-password-after-gatling.bat` - Windows batch script
- `reset-password-after-gatling.ps1` - Windows PowerShell script
- `reset-password-after-gatling.sh` - Linux/Mac shell script
- `GATLING_PASSWORD_RESET_README.md` - This documentation

## Benefits

✅ **Automated workflow** - No manual password reset needed  
✅ **Consistent testing** - Always starts with default credentials  
✅ **Cross-platform** - Works on Windows, Linux, and Mac  
✅ **Error handling** - Provides clear feedback on success/failure  
✅ **Development friendly** - Doesn't interfere with normal development workflow
✅ **Admin privileges** - Uses admin account for full API access
