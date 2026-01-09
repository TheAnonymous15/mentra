-s#!/bin/bash

# Shizuku Installation Script for Mentra
# Device: 24117RN76G (nvf66t6tuohit8rk)

DEVICE="nvf66t6tuohit8rk"
BLUE='\033[0;34m'
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo ""
echo -e "${BLUE}════════════════════════════════════════${NC}"
echo -e "${BLUE}   Shizuku Installation for Mentra${NC}"
echo -e "${BLUE}════════════════════════════════════════${NC}"
echo ""

# Check device connection
echo "Checking device connection..."
CONNECTED=$(adb devices | grep "$DEVICE")

if [ -z "$CONNECTED" ]; then
    echo -e "${RED}❌ Device not connected${NC}"
    echo "Please ensure your phone (24117RN76G) is connected via USB"
    exit 1
fi

echo -e "${GREEN}✓ Device connected: 24117RN76G${NC}"
echo ""

# Check if Shizuku is already installed
echo "Checking if Shizuku is installed..."
INSTALLED=$(adb -s "$DEVICE" shell pm list packages | grep "moe.shizuku.privileged.api")

if [ -n "$INSTALLED" ]; then
    echo -e "${GREEN}✓ Shizuku is already installed!${NC}"
    echo ""
    echo "Next steps:"
    echo "1. Enable USB Debugging in Settings → Developer Options"
    echo "2. Run: ./setup-shizuku.sh"
    echo "   OR manually: adb -s $DEVICE shell sh /data/user/0/moe.shizuku.privileged.api/start.sh"
    echo "3. Open Shizuku app and verify it's running"
    echo "4. Grant Mentra permission in Shizuku's 'Authorized Apps'"
    exit 0
fi

echo -e "${YELLOW}⚠️  Shizuku not installed${NC}"
echo ""

# Check for APK in Downloads
echo "Looking for Shizuku APK..."
APK_PATH=$(find ~/Downloads -name "shizuku*.apk" -o -name "Shizuku*.apk" 2>/dev/null | head -1)

if [ -z "$APK_PATH" ]; then
    echo -e "${RED}❌ No Shizuku APK found in ~/Downloads/${NC}"
    echo ""
    echo "Please install Shizuku using one of these methods:"
    echo ""
    echo -e "${YELLOW}Method 1: Play Store (Easiest)${NC}"
    echo "  1. Open Play Store on your phone"
    echo "  2. Search for 'Shizuku'"
    echo "  3. Install the app"
    echo ""
    echo -e "${YELLOW}Method 2: Download APK${NC}"
    echo "  1. Visit: https://github.com/RikkaApps/Shizuku/releases/latest"
    echo "  2. Download the APK to ~/Downloads/"
    echo "  3. Run this script again"
    echo ""
    echo -e "${YELLOW}Method 3: Direct URL${NC}"
    echo "  curl -L -o ~/Downloads/shizuku.apk 'https://github.com/RikkaApps/Shizuku/releases/download/v13.5.4.r1044.27e2f81/shizuku-v13.5.4.r1044.27e2f81-release.apk'"
    echo "  Then run this script again"
    echo ""
    exit 1
fi

echo -e "${GREEN}✓ Found APK: $APK_PATH${NC}"
echo ""

# Install APK
echo "Installing Shizuku on device..."
adb -s "$DEVICE" install -r "$APK_PATH"

if [ $? -eq 0 ]; then
    echo ""
    echo -e "${GREEN}════════════════════════════════════════${NC}"
    echo -e "${GREEN}✓ Shizuku installed successfully!${NC}"
    echo -e "${GREEN}════════════════════════════════════════${NC}"
    echo ""
    echo "Next steps:"
    echo ""
    echo "1. Enable Developer Options on your phone:"
    echo "   Settings → About Phone → Tap 'Build Number' 7 times"
    echo ""
    echo "2. Enable USB Debugging:"
    echo "   Settings → Developer Options → USB Debugging → ON"
    echo ""
    echo "3. Start Shizuku service:"
    echo "   Run: ./setup-shizuku.sh"
    echo "   OR: adb -s $DEVICE shell sh /data/user/0/moe.shizuku.privileged.api/start.sh"
    echo ""
    echo "4. Open Shizuku app on your phone"
    echo "   • Verify it shows 'Running'"
    echo ""
    echo "5. Grant Mentra permission:"
    echo "   • In Shizuku app → 'Authorized Apps' tab"
    echo "   • Enable 'Mentra'"
    echo ""
    echo "6. Test in Mentra shell:"
    echo "   • show battery (works without Shizuku)"
    echo "   • brightness 200 (requires Shizuku)"
    echo ""
else
    echo ""
    echo -e "${RED}❌ Installation failed${NC}"
    echo "Please check the error messages above"
    exit 1
fi

