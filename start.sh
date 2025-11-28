#!/bin/bash

################################################################################
# StarWatchX - Application Startup Script
# Starts all components: Java Engine + Dashboard
################################################################################

set -e  # Exit on error

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Get the directory where this script is located
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd "$SCRIPT_DIR"

# PID file locations
PIDS_DIR="$SCRIPT_DIR/.pids"
mkdir -p "$PIDS_DIR"
JAVA_PID_FILE="$PIDS_DIR/java-engine.pid"
DASHBOARD_PID_FILE="$PIDS_DIR/dashboard.pid"

echo -e "${BLUE}╔══════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║          StarWatchX - Starting Application       ║${NC}"
echo -e "${BLUE}╚══════════════════════════════════════════════════╝${NC}"
echo ""

# Check if already running
if [ -f "$JAVA_PID_FILE" ] || [ -f "$DASHBOARD_PID_FILE" ]; then
    echo -e "${YELLOW}⚠ Warning: Application may already be running${NC}"
    echo -e "${YELLOW}Run ./stop.sh first to stop any existing instances${NC}"
    read -p "Continue anyway? (y/n) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 1
    fi
fi

################################################################################
# 1. Start Java Engine
################################################################################

echo -e "${BLUE}[1/2]${NC} Starting Java Engine..."
echo "----------------------------------------"

cd "$SCRIPT_DIR/java-engine"

# Check if Maven wrapper exists, otherwise use maven
if [ -f "mvnw" ]; then
    MVN_CMD="./mvnw"
elif command -v mvn &> /dev/null; then
    MVN_CMD="mvn"
else
    echo -e "${RED}✗ Maven not found! Please install Maven or run setup.sh${NC}"
    exit 1
fi

# Check if Java is installed
if ! command -v java &> /dev/null; then
    echo -e "${RED}✗ Java not found! Please install Java JDK 11 or higher${NC}"
    exit 1
fi

# Compile and run Java application
echo "  Compiling Java application..."
$MVN_CMD clean compile > /dev/null 2>&1 || {
    echo -e "${RED}✗ Maven compilation failed${NC}"
    echo "  Run '$MVN_CMD clean compile' manually to see errors"
    exit 1
}

echo "  Starting Java engine in background..."
# Run in background and save PID
nohup $MVN_CMD exec:java -Dexec.mainClass="com.starwatchx.App" > "$SCRIPT_DIR/logs/java-engine.log" 2>&1 &
JAVA_PID=$!
echo $JAVA_PID > "$JAVA_PID_FILE"

echo -e "${GREEN}✓ Java Engine started (PID: $JAVA_PID)${NC}"
echo "  Log: $SCRIPT_DIR/logs/java-engine.log"
echo ""

# Wait for Java engine to initialize
sleep 3

################################################################################
# 2. Start Dashboard
################################################################################

echo -e "${BLUE}[2/2]${NC} Starting Dashboard..."
echo "----------------------------------------"

cd "$SCRIPT_DIR/dashboard"

# Check if Node.js is installed
if ! command -v node &> /dev/null; then
    echo -e "${RED}✗ Node.js not found! Please install Node.js${NC}"
    kill $JAVA_PID 2>/dev/null
    exit 1
fi

# Check if npm is installed
if ! command -v npm &> /dev/null; then
    echo -e "${RED}✗ npm not found! Please install npm${NC}"
    kill $JAVA_PID 2>/dev/null
    exit 1
fi

# Check if node_modules exists
if [ ! -d "node_modules" ]; then
    echo "  Installing dependencies..."
    npm install > /dev/null 2>&1 || {
        echo -e "${RED}✗ npm install failed${NC}"
        kill $JAVA_PID 2>/dev/null
        exit 1
    }
fi

echo "  Starting dashboard in background..."
# Run in background and save PID
nohup npm run dev > "$SCRIPT_DIR/logs/dashboard.log" 2>&1 &
DASHBOARD_PID=$!
echo $DASHBOARD_PID > "$DASHBOARD_PID_FILE"

echo -e "${GREEN}✓ Dashboard started (PID: $DASHBOARD_PID)${NC}"
echo "  Log: $SCRIPT_DIR/logs/dashboard.log"
echo ""

# Wait for dashboard to start
sleep 5

################################################################################
# Summary
################################################################################

echo -e "${GREEN}╔══════════════════════════════════════════════════╗${NC}"
echo -e "${GREEN}║        StarWatchX Started Successfully! 🚀       ║${NC}"
echo -e "${GREEN}╚══════════════════════════════════════════════════╝${NC}"
echo ""
echo -e "${BLUE}Services:${NC}"
echo "  • Java Engine:    http://localhost:8080 (WebSocket)"
echo "  • Dashboard:      http://localhost:3000"
echo ""
echo -e "${BLUE}Process IDs:${NC}"
echo "  • Java Engine:    $JAVA_PID"
echo "  • Dashboard:      $DASHBOARD_PID"
echo ""
echo -e "${BLUE}Logs:${NC}"
echo "  • Java:           tail -f logs/java-engine.log"
echo "  • Dashboard:      tail -f logs/dashboard.log"
echo ""
echo -e "${YELLOW}To stop the application, run:${NC} ./stop.sh"
echo ""
echo -e "${BLUE}Opening dashboard in browser...${NC}"

# Open browser (platform-specific)
sleep 2
if [[ "$OSTYPE" == "darwin"* ]]; then
    open http://localhost:3000
elif [[ "$OSTYPE" == "linux-gnu"* ]]; then
    xdg-open http://localhost:3000 2>/dev/null || echo "Please open http://localhost:3000 in your browser"
else
    echo "Please open http://localhost:3000 in your browser"
fi

echo ""
echo -e "${GREEN}✓ StarWatchX is running!${NC}"

