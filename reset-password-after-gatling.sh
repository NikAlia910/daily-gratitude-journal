#!/bin/bash

echo "========================================"
echo "Running Gatling Test with Password Reset"
echo "========================================"

echo ""
echo "Step 1: Running Gatling Performance Test..."
echo ""

# Run the Gatling test
./mvnw gatling:test

echo ""
echo "Step 2: Resetting passwords back to defaults..."
echo ""

# Function to reset password for a specific user
reset_user_password() {
    local username=$1
    local current_password=$2
    local new_password=$3
    
    echo "Resetting password for $username..."
    
    # First authenticate to get JWT token
    AUTH_RESPONSE=$(curl -s -w "%{http_code}" -X POST http://localhost:8080/api/authenticate \
        -H "Content-Type: application/json" \
        -d "{\"username\":\"$username\", \"password\":\"$current_password\"}")
    
    HTTP_CODE="${AUTH_RESPONSE: -3}"
    AUTH_BODY="${AUTH_RESPONSE%???}"
    
    if [ "$HTTP_CODE" -eq 200 ]; then
        echo "JWT token obtained for $username"
        
        # Extract JWT token
        JWT_TOKEN=$(echo "$AUTH_BODY" | grep -o '"id_token":"[^"]*"' | cut -d'"' -f4)
        
        if [ -n "$JWT_TOKEN" ]; then
            # Reset password back to default
            RESET_RESPONSE=$(curl -s -w "%{http_code}" -X POST http://localhost:8080/api/account/change-password \
                -H "Content-Type: application/json" \
                -H "Authorization: Bearer $JWT_TOKEN" \
                -d "{\"currentPassword\":\"$current_password\",\"newPassword\":\"$new_password\"}")
            
            RESET_HTTP_CODE="${RESET_RESPONSE: -3}"
            
            if [ "$RESET_HTTP_CODE" -eq 200 ]; then
                echo "✅ Password successfully reset for $username ($username/$new_password)"
                return 0
            else
                echo "❌ Failed to reset password for $username. Status: $RESET_HTTP_CODE"
                return 1
            fi
        else
            echo "❌ Failed to extract JWT token for $username from response"
            return 1
        fi
    else
        echo "❌ Failed to authenticate $username for password reset. Status: $HTTP_CODE"
        return 1
    fi
}

# Reset admin password (admin/admin) - This is the primary account used in the test
admin_success=false
if reset_user_password "admin" "newpassword123" "admin"; then
    admin_success=true
fi

# Reset user password (user/user) - Keep this as backup
user_success=false
if reset_user_password "user" "newpassword123" "user"; then
    user_success=true
fi

echo ""
echo "========================================"
echo "Gatling Test and Password Reset Complete"
echo "========================================"
echo ""
echo "Default credentials restored:"
echo "Admin - Username: admin, Password: admin"
echo "User  - Username: user,  Password: user"
echo ""

if [ "$admin_success" = true ] && [ "$user_success" = true ]; then
    echo "✅ All passwords reset successfully!"
else
    echo "⚠️ Some password resets may have failed. Check the output above."
fi 