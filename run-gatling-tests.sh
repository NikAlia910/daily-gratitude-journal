#!/bin/bash

# Daily Gratitude Journal - Gatling Test Runner
# This script provides easy execution of Gatling performance tests

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Default configuration
DEFAULT_BASE_URL="http://localhost:8080"
DEFAULT_USERS=10
DEFAULT_RAMP=10
SIMULATION_CLASS="gatling.simulations.GratitudeEntryGatlingTest"

# Function to print colored output
print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Function to show usage
show_usage() {
    echo "Daily Gratitude Journal - Gatling Test Runner"
    echo ""
    echo "Usage: $0 [OPTIONS]"
    echo ""
    echo "Options:"
    echo "  -h, --help              Show this help message"
    echo "  -u, --url URL           Base URL for the application (default: $DEFAULT_BASE_URL)"
    echo "  -n, --users NUMBER      Number of concurrent users (default: $DEFAULT_USERS)"
    echo "  -r, --ramp SECONDS      Ramp-up time in seconds (default: $DEFAULT_RAMP)"
    echo "  -c, --class CLASS       Simulation class (default: $SIMULATION_CLASS)"
    echo "  -d, --dry-run           Show command without executing"
    echo "  -v, --verbose           Enable verbose output"
    echo ""
    echo "Examples:"
    echo "  $0                                    # Run with default settings"
    echo "  $0 -u http://localhost:8080          # Run with custom URL"
    echo "  $0 -n 20 -r 30                       # Run with 20 users, 30s ramp"
    echo "  $0 -d                                 # Show command without running"
    echo ""
}

# Function to check if application is running
check_application() {
    local url=$1
    print_status "Checking if application is running at $url..."
    
    if curl -s --max-time 5 "$url/management/health" > /dev/null 2>&1; then
        print_success "Application is running and healthy"
        return 0
    else
        print_error "Application is not running or not accessible at $url"
        print_warning "Please start the application before running tests"
        return 1
    fi
}

# Function to run the test
run_test() {
    local base_url=$1
    local users=$2
    local ramp=$3
    local simulation_class=$4
    local dry_run=$5
    local verbose=$6
    
    # Build Maven command
    local mvn_cmd="mvn io.gatling:gatling-maven-plugin:test"
    local args="-Dgatling.simulationClass=$simulation_class"
    
    if [ "$base_url" != "$DEFAULT_BASE_URL" ]; then
        args="$args -DbaseURL=$base_url"
    fi
    
    if [ "$users" != "$DEFAULT_USERS" ]; then
        args="$args -Dusers=$users"
    fi
    
    if [ "$ramp" != "$DEFAULT_RAMP" ]; then
        args="$args -Dramp=$ramp"
    fi
    
    if [ "$verbose" = "true" ]; then
        mvn_cmd="$mvn_cmd -X"
    fi
    
    local full_cmd="$mvn_cmd $args"
    
    print_status "Configuration:"
    echo "  Base URL: $base_url"
    echo "  Users: $users"
    echo "  Ramp Time: ${ramp}s"
    echo "  Simulation: $simulation_class"
    echo ""
    
    if [ "$dry_run" = "true" ]; then
        print_warning "DRY RUN - Command that would be executed:"
        echo "$full_cmd"
        return 0
    fi
    
    # Check if application is running
    if ! check_application "$base_url"; then
        exit 1
    fi
    
    print_status "Starting Gatling performance test..."
    echo ""
    
    # Execute the test
    if eval "$full_cmd"; then
        print_success "Gatling test completed successfully!"
        print_status "Check the generated reports in target/gatling/results/"
    else
        print_error "Gatling test failed!"
        exit 1
    fi
}

# Parse command line arguments
BASE_URL="$DEFAULT_BASE_URL"
USERS="$DEFAULT_USERS"
RAMP="$DEFAULT_RAMP"
SIMULATION_CLASS="$SIMULATION_CLASS"
DRY_RUN=false
VERBOSE=false

while [[ $# -gt 0 ]]; do
    case $1 in
        -h|--help)
            show_usage
            exit 0
            ;;
        -u|--url)
            BASE_URL="$2"
            shift 2
            ;;
        -n|--users)
            USERS="$2"
            shift 2
            ;;
        -r|--ramp)
            RAMP="$2"
            shift 2
            ;;
        -c|--class)
            SIMULATION_CLASS="$2"
            shift 2
            ;;
        -d|--dry-run)
            DRY_RUN=true
            shift
            ;;
        -v|--verbose)
            VERBOSE=true
            shift
            ;;
        *)
            print_error "Unknown option: $1"
            show_usage
            exit 1
            ;;
    esac
done

# Validate numeric inputs
if ! [[ "$USERS" =~ ^[0-9]+$ ]] || [ "$USERS" -lt 1 ]; then
    print_error "Users must be a positive integer"
    exit 1
fi

if ! [[ "$RAMP" =~ ^[0-9]+$ ]] || [ "$RAMP" -lt 1 ]; then
    print_error "Ramp time must be a positive integer"
    exit 1
fi

# Run the test
run_test "$BASE_URL" "$USERS" "$RAMP" "$SIMULATION_CLASS" "$DRY_RUN" "$VERBOSE" 