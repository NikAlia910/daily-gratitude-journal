@echo off
echo ========================================
echo Running Gatling Test with Password Reset
echo ========================================

echo.
echo Step 1: Running Gatling Performance Test...
echo.

REM Run the Gatling test
call mvnw gatling:test

echo.
echo Step 2: Resetting passwords back to defaults...
echo.

REM Reset passwords back to default using the JWT token from the test
powershell -Command "& {
    # Function to reset password for a specific user
    function Reset-UserPassword {
        param(
            [string]`$Username,
            [string]`$CurrentPassword,
            [string]`$NewPassword
        )
        
        try {
            Write-Host 'Resetting password for ' `$Username '...' -ForegroundColor Green
            
            # First authenticate to get JWT token
            `$authResponse = Invoke-WebRequest -Uri 'http://localhost:8080/api/authenticate' -Method POST -Headers @{'Content-Type'='application/json'} -Body '{\"username\":\"' `$Username '\", \"password\":\"' `$CurrentPassword '\"}' -UseBasicParsing
            
            if (`$authResponse.StatusCode -eq 200) {
                `$authData = `$authResponse.Content | ConvertFrom-Json
                `$jwtToken = `$authData.id_token
                
                Write-Host 'JWT token obtained for ' `$Username -ForegroundColor Green
                
                # Reset password back to default
                `$resetBody = '{\"currentPassword\":\"' `$CurrentPassword '\",\"newPassword\":\"' `$NewPassword '\"}'
                `$headers = @{
                    'Content-Type' = 'application/json'
                    'Authorization' = 'Bearer ' + `$jwtToken
                }
                
                `$resetResponse = Invoke-WebRequest -Uri 'http://localhost:8080/api/account/change-password' -Method POST -Headers `$headers -Body `$resetBody -UseBasicParsing
                
                if (`$resetResponse.StatusCode -eq 200) {
                    Write-Host '✅ Password successfully reset for ' `$Username ' (' `$Username '/' `$NewPassword ')' -ForegroundColor Green
                    return `$true
                } else {
                    Write-Host '❌ Failed to reset password for ' `$Username '. Status: ' `$resetResponse.StatusCode -ForegroundColor Red
                    return `$false
                }
            } else {
                Write-Host '❌ Failed to authenticate ' `$Username ' for password reset. Status: ' `$authResponse.StatusCode -ForegroundColor Red
                return `$false
            }
        } catch {
            Write-Host '❌ Error during password reset for ' `$Username ': ' `$_.Exception.Message -ForegroundColor Red
            return `$false
        }
    }

    # Reset admin password (admin/admin) - This is the primary account used in the test
    `$adminSuccess = Reset-UserPassword -Username 'admin' -CurrentPassword 'newpassword123' -NewPassword 'admin'
    
    # Reset user password (user/user) - Keep this as backup
    `$userSuccess = Reset-UserPassword -Username 'user' -CurrentPassword 'newpassword123' -NewPassword 'user'
    
    if (`$adminSuccess -and `$userSuccess) {
        Write-Host '✅ All passwords reset successfully!' -ForegroundColor Green
    } else {
        Write-Host '⚠️ Some password resets may have failed. Check the output above.' -ForegroundColor Yellow
    }
}"

echo.
echo ========================================
echo Gatling Test and Password Reset Complete
echo ========================================
echo.
echo Default credentials restored:
echo Admin - Username: admin, Password: admin
echo User  - Username: user,  Password: user
echo.
pause 