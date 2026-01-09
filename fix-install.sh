#!/bin/bash

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo ""
echo -e "${BLUE}════════════════════════════════════════${NC}"
echo -e "${BLUE}   Mentra - Fix & Install${NC}"
echo -e "${BLUE}════════════════════════════════════════${NC}"
echo ""

APK_PATH="app/build/outputs/apk/debug/app-debug.apk"

# Check if APK exists
if [ ! -f "$APK_PATH" ]; then
    echo -e "${RED}❌ APK not found!${NC}"
    echo "Building APK first..."
    ./gradlew assembleDebug --no-daemon
    if [ $? -ne 0 ]; then
        echo -e "${RED}Build failed!${NC}"
        exit 1
    fi
fi

echo -e "${GREEN}✓ APK built: $(ls -lh $APK_PATH | awk '{print $5}')${NC}"
echo ""

# Restart ADB
echo "🔄 Restarting ADB server..."
adb kill-server > /dev/null 2>&1
sleep 1
adb start-server > /dev/null 2>&1
sleep 2

# List devices
echo "📱 Checking connected devices..."
DEVICES=$(adb devices | grep -v "List" | grep "device$")

if [ -z "$DEVICES" ]; then
    echo -e "${RED}❌ No devices connected!${NC}"
    echo ""
    echo "Please connect your device via:"
    echo "1. USB cable"
    echo "2. Enable USB debugging"
    echo "3. Accept the prompt on your phone"
    echo ""
    echo "Or use wireless ADB:"
    echo "  adb pair <IP>:<PORT>"
    echo "  adb connect <IP>:5555"
    exit 1
fi

echo -e "${GREEN}Connected devices:${NC}"
echo "$DEVICES"
echo ""

# Get first device
DEVICE=$(echo "$DEVICES" | head -1 | awk '{print $1}')
echo "📲 Installing on: $DEVICE"
echo ""

# Try different installation methods

# Method 1: Direct install with -r flag
echo "Method 1: Direct install with replace flag..."
adb -s "$DEVICE" install -r "$APK_PATH" 2>&1 | tee /tmp/install.log

if grep -q "Success" /tmp/install.log; then
    echo ""
    echo -e "${GREEN}════════════════════════════════════════${NC}"
    echo -e "${GREEN}✓ Installation successful!${NC}"
    echo -e "${GREEN}════════════════════════════════════════${NC}"
    echo ""
    echo "🚀 Starting app..."
    adb -s "$DEVICE" shell am start -n com.example.mentra/.launcher.LauncherActivity
    exit 0
fi

# Method 2: Uninstall then install
echo ""
echo -e "${YELLOW}Method 2: Uninstall old version first...${NC}"
adb -s "$DEVICE" uninstall com.example.mentra > /dev/null 2>&1
sleep 1
adb -s "$DEVICE" install "$APK_PATH" 2>&1 | tee /tmp/install.log

if grep -q "Success" /tmp/install.log; then
    echo ""
    echo -e "${GREEN}════════════════════════════════════════${NC}"
    echo -e "${GREEN}✓ Installation successful!${NC}"
    echo -e "${GREEN}════════════════════════════════════════${NC}"
    echo ""
    echo "🚀 Starting app..."
    adb -s "$DEVICE" shell am start -n com.example.mentra/.launcher.LauncherActivity
    exit 0
fi

# Method 3: Install via pm command
echo ""
echo -e "${YELLOW}Method 3: Push and install manually...${NC}"
adb -s "$DEVICE" push "$APK_PATH" /sdcard/mentra.apk
if [ $? -eq 0 ]; then
    adb -s "$DEVICE" shell pm install -r /sdcard/mentra.apk
    adb -s "$DEVICE" shell rm /sdcard/mentra.apk

    echo ""
    echo -e "${GREEN}════════════════════════════════════════${NC}"
    echo -e "${GREEN}✓ Installation successful!${NC}"
    echo -e "${GREEN}════════════════════════════════════════${NC}"
    echo ""
    echo "🚀 Starting app..."
    adb -s "$DEVICE" shell am start -n com.example.mentra/.launcher.LauncherActivity
    exit 0
fi

# All methods failed
echo ""
echo -e "${RED}════════════════════════════════════════${NC}"
echo -e "${RED}❌ Installation failed!${NC}"
echo -e "${RED}════════════════════════════════════════${NC}"
echo ""
echo "Troubleshooting:"
echo "1. Reconnect USB cable"
echo "2. Restart ADB: adb kill-server && adb start-server"
echo "3. Restart your phone"
echo "4. Try wireless ADB"
echo "5. Manual install: Copy APK to phone and install manually"
echo ""
echo "APK Location: $APK_PATH"
echo "APK Size: $(ls -lh $APK_PATH | awk '{print $5}')"
echo ""
exit 1

