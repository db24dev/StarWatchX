#!/bin/bash

################################################################################
# StarWatchX - Application Stop Script
# Stops all running components gracefully
################################################################################

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
JAVA_PID_FILE="$PIDS_DIR/java-engine.pid"
DASHBOARD_PID_FILE="$PIDS_DIR/dashboard.pid"

echo -e "${BLUE}╔══════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║          StarWatchX - Stopping Application       ║${NC}"
echo -e "${BLUE}╚══════════════════════════════════════════════════╝${NC}"
echo ""

STOPPED_ANY=false

################################################################################
# Stop Java Engine
################################################################################

if [ -f "$JAVA_PID_FILE" ]; then
    JAVA_PID=$(cat "$JAVA_PID_FILE")
    echo -e "${BLUE}Stopping Java Engine (PID: $JAVA_PID)...${NC}"
    
    if ps -p $JAVA_PID > /dev/null 2>&1; then
        kill $JAVA_PID 2>/dev/null
        
        # Wait for graceful shutdown (max 10 seconds)
        for i in {1..10}; do
            if ! ps -p $JAVA_PID > /dev/null 2>&1; then
                break
            fi
            sleep 1
        done
        
        # Force kill if still running
        if ps -p $JAVA_PID > /dev/null 2>&1; then
            echo -e "${YELLOW}  Force killing Java Engine...${NC}"
            kill -9 $JAVA_PID 2>/dev/null
        fi
        
        echo -e "${GREEN}✓ Java Engine stopped${NC}"
        STOPPED_ANY=true
    else
        echo -e "${YELLOW}  Java Engine process not running${NC}"
    fi
    
    rm -f "$JAVA_PID_FILE"
else
    echo -e "${YELLOW}Java Engine PID file not found${NC}"
fi

echo ""

################################################################################
# Stop Dashboard
################################################################################

if [ -f "$DASHBOARD_PID_FILE" ]; then
    DASHBOARD_PID=$(cat "$DASHBOARD_PID_FILE")
    echo -e "${BLUE}Stopping Dashboard (PID: $DASHBOARD_PID)...${NC}"
    
    if ps -p $DASHBOARD_PID > /dev/null 2>&1; then
        kill $DASHBOARD_PID 2>/dev/null
        
        # Wait for graceful shutdown (max 10 seconds)
        for i in {1..10}; do
            if ! ps -p $DASHBOARD_PID > /dev/null 2>&1; then
                break
            fi
            sleep 1
        done
        
        # Force kill if still running
        if ps -p $DASHBOARD_PID > /dev/null 2>&1; then
            echo -e "${YELLOW}  Force killing Dashboard...${NC}"
            kill -9 $DASHBOARD_PID 2>/dev/null
        fi
        
        echo -e "${GREEN}✓ Dashboard stopped${NC}"
        STOPPED_ANY=true
    else
        echo -e "${YELLOW}  Dashboard process not running${NC}"
    fi
    
    rm -f "$DASHBOARD_PID_FILE"
else
    echo -e "${YELLOW}Dashboard PID file not found${NC}"
fi

echo ""

################################################################################
# Kill any remaining processes
################################################################################

echo -e "${BLUE}Checking for any remaining processes...${NC}"

# Kill any Java processes running StarWatchX
JAVA_PIDS=$(ps aux | grep "com.starwatchx.App" | grep -v grep | awk '{print $2}')
if [ ! -z "$JAVA_PIDS" ]; then
    echo "  Found remaining Java processes: $JAVA_PIDS"
    kill $JAVA_PIDS 2>/dev/null
    STOPPED_ANY=true
fi

# Kill any node processes running the dashboard
DASHBOARD_PIDS=$(ps aux | grep "next dev" | grep -v grep | awk '{print $2}')
if [ ! -z "$DASHBOARD_PIDS" ]; then
    echo "  Found remaining Dashboard processes: $DASHBOARD_PIDS"
    kill $DASHBOARD_PIDS 2>/dev/null
    STOPPED_ANY=true
fi

echo ""

################################################################################
# Summary
################################################################################

if [ "$STOPPED_ANY" = true ]; then
    echo -e "${GREEN}╔══════════════════════════════════════════════════╗${NC}"
    echo -e "${GREEN}║        StarWatchX Stopped Successfully! ✓        ║${NC}"
    echo -e "${GREEN}╚══════════════════════════════════════════════════╝${NC}"
else
    echo -e "${YELLOW}╔══════════════════════════════════════════════════╗${NC}"
    echo -e "${YELLOW}║         No StarWatchX processes found            ║${NC}"
    echo -e "${YELLOW}╚══════════════════════════════════════════════════╝${NC}"
fi

echo ""
echo -e "${BLUE}To start the application again, run:${NC} ./start.sh"
echo ""

