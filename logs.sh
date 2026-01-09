#!/bin/bash

# Colors
BLUE='\033[0;34m'
NC='\033[0m'

PACKAGE_NAME="com.example.mentra"

echo ""
echo -e "${BLUE}════════════════════════════════════════${NC}"
echo -e "${BLUE}   Mentra - Logcat Viewer${NC}"
echo -e "${BLUE}════════════════════════════════════════${NC}"
echo ""

# Get device
DEVICE=$(adb devices | grep -v "List" | grep "device$" | head -1 | awk '{print $1}')

if [ -z "$DEVICE" ]; then
    echo "❌ No device connected"
    exit 1
fi

DEVICE_NAME=$(adb -s "$DEVICE" shell getprop ro.product.model 2>/dev/null | tr -d '\r')

echo "📦 Package: $PACKAGE_NAME"
echo "📱 Device: $DEVICE_NAME"
echo ""
echo "════════════════════════════════════════"
echo "Press Ctrl+C to stop"
echo ""

# Clear logcat first
adb -s "$DEVICE" logcat -c

# Show logs with filters
adb -s "$DEVICE" logcat \
    -s "Mentra:*" \
    "MainActivity:*" \
    "PermissionManager:*" \
    "AndroidRuntime:E" \
    "System.err:*" \
    "*:F"

NC='\033[0m'
BLUE='\033[0;34m'
# Colors

PACKAGE_NAME="com.example.mentra"

# Usage: ./logs.sh
# Logcat viewer for Mentra app


