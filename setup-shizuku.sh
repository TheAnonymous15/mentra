#!/bin/bash

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo ""
echo -e "${BLUE}════════════════════════════════════════${NC}"
echo -e "${BLUE}   Mentra - Shizuku Setup Helper${NC}"
echo -e "${BLUE}════════════════════════════════════════${NC}"
echo ""

# Check if device is connected
DEVICE=$(adb devices | grep -v "List" | grep "device$" | head -1 | awk '{print $1}')

if [ -z "$DEVICE" ]; then
    echo -e "${RED}❌ No device connected via ADB${NC}"
    echo "Please connect your device and enable USB debugging"
    exit 1
fi

DEVICE_NAME=$(adb -s "$DEVICE" shell getprop ro.product.model 2>/dev/null | tr -d '\r')
ANDROID_VERSION=$(adb -s "$DEVICE" shell getprop ro.build.version.release 2>/dev/null | tr -d '\r')

echo -e "${GREEN}✓ Device connected${NC}"
echo "📱 Device: $DEVICE_NAME"
echo "🤖 Android: $ANDROID_VERSION"
echo ""

# Check if Shizuku is installed
SHIZUKU_INSTALLED=$(adb -s "$DEVICE" shell pm list packages | grep "moe.shizuku.privileged.api")

if [ -z "$SHIZUKU_INSTALLED" ]; then
    echo -e "${RED}❌ Shizuku not installed${NC}"
    echo ""
    echo "Please install Shizuku:"
    echo "1. Play Store: https://play.google.com/store/apps/details?id=moe.shizuku.privileged.api"
    echo "2. GitHub: https://github.com/RikkaApps/Shizuku/releases"
    echo ""
    exit 1
fi

echo -e "${GREEN}✓ Shizuku is installed${NC}"
echo ""

# Check if Shizuku is already running
SHIZUKU_RUNNING=$(adb -s "$DEVICE" shell ps | grep "shizuku")

if [ -n "$SHIZUKU_RUNNING" ]; then
    echo -e "${GREEN}✓ Shizuku is already running!${NC}"
    echo ""
    echo "Next steps:"
    echo "1. Open Shizuku app on your phone"
    echo "2. Go to 'Authorized Apps' tab"
    echo "3. Enable 'Mentra'"
    echo "4. Test in Mentra shell: show battery"
    echo ""
    exit 0
fi

# Start Shizuku
echo "Starting Shizuku service..."
adb -s "$DEVICE" shell sh /data/user/0/moe.shizuku.privileged.api/start.sh 2>&1

sleep 2

# Check if Shizuku is now running
SHIZUKU_RUNNING=$(adb -s "$DEVICE" shell ps | grep "shizuku")

if [ -z "$SHIZUKU_RUNNING" ]; then
    echo -e "${YELLOW}⚠️  Shizuku may not be running${NC}"
    echo ""
    echo "Please try these steps:"
    echo "1. Open Shizuku app on your phone"
    echo "2. Tap 'Start via Wireless Debugging' or 'Start via USB'"
    echo "3. If using wireless, enable 'Wireless Debugging' in Developer Options first"
    echo ""
    echo "For detailed setup, see: SHIZUKU-SETUP-GUIDE.md"
    exit 1
else
    echo -e "${GREEN}✓ Shizuku service started successfully!${NC}"
fi

echo ""
echo -e "${BLUE}════════════════════════════════════════${NC}"
echo -e "${GREEN}Setup Complete!${NC}"
echo -e "${BLUE}════════════════════════════════════════${NC}"
echo ""
echo "Next steps:"
echo "1. Open Shizuku app on your phone"
echo "2. Verify it shows 'Running'"
echo "3. Go to 'Authorized Apps' tab"
echo "4. Enable 'Mentra'"
echo "5. Open Mentra and test commands!"
echo ""
echo -e "${YELLOW}Test commands:${NC}"
echo "  show battery         (works without Shizuku)"
echo "  brightness 200       (requires Shizuku)"
echo "  wifi --state=on      (requires Shizuku)"
echo ""
echo -e "${BLUE}════════════════════════════════════════${NC}"

