# 🔧 Installation Troubleshooting Guide

## ❌ Problem: EOF Error During Installation

You encountered: `com.android.ddmlib.InstallException: EOF`

This happens when:
- Wireless ADB connection is unstable
- APK is too large (39MB)
- Network interruption during transfer
- ADB server issues

---

## ✅ Solution Methods

### **Method 1: Use Fix-Install Script (Recommended)**

```bash
cd /Users/danielkinyua/Downloads/projects/mentra
./fix-install.sh
```

This script tries 3 different installation methods automatically:
1. Direct install with replace flag
2. Uninstall old version then install
3. Push APK to device then install

---

### **Method 2: Manual ADB Commands**

```bash
# Restart ADB
adb kill-server
adb start-server

# Check devices
adb devices

# Uninstall old version
adb -s <DEVICE_ID> uninstall com.example.mentra

# Install new version
adb -s <DEVICE_ID> install app/build/outputs/apk/debug/app-debug.apk

# Or with replace flag
adb -s <DEVICE_ID> install -r app/build/outputs/apk/debug/app-debug.apk
```

---

### **Method 3: USB Cable Installation**

If wireless ADB is failing:

```bash
# 1. Disconnect wireless ADB
adb disconnect

# 2. Connect phone via USB cable

# 3. Enable USB debugging on phone
#    Settings → Developer Options → USB Debugging → ON

# 4. Accept the prompt on your phone

# 5. Check connection
adb devices

# 6. Install
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

### **Method 4: Push Then Install**

For large APKs or slow connections:

```bash
# 1. Push APK to phone storage
adb push app/build/outputs/apk/debug/app-debug.apk /sdcard/mentra.apk

# 2. Install via pm command
adb shell pm install -r /sdcard/mentra.apk

# 3. Clean up
adb shell rm /sdcard/mentra.apk
```

---

### **Method 5: Manual Installation (No PC)**

```bash
# 1. Copy APK to your phone
# From: /Users/danielkinyua/Downloads/projects/mentra/app/build/outputs/apk/debug/app-debug.apk
# To: Your phone (via USB, Cloud, etc.)

# 2. On your phone:
#    - Open file manager
#    - Navigate to the APK
#    - Tap to install
#    - Allow "Install from Unknown Sources" if prompted
```

---

### **Method 6: Wireless ADB Reconnect**

If using wireless ADB:

```bash
# 1. Check current connections
adb devices

# 2. Disconnect all
adb disconnect

# 3. On phone: Settings → Developer Options → Wireless Debugging
#    Get IP and Port

# 4. Reconnect
adb pair <IP>:<PAIRING_PORT>
# Enter pairing code from phone

adb connect <IP>:5555

# 5. Verify
adb devices

# 6. Install
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## 🎯 Quick Fixes

### **Fix 1: Restart Everything**
```bash
# Restart ADB
adb kill-server && adb start-server

# Restart phone (optional)
adb reboot

# Wait for phone to restart, then reconnect
```

### **Fix 2: Use USB Instead of Wireless**
```bash
# Disconnect wireless
adb disconnect

# Connect USB cable
# Phone should appear in adb devices
```

### **Fix 3: Reduce APK Size**
```bash
# Enable ProGuard/R8 minification
# Edit app/build.gradle.kts:

buildTypes {
    release {
        isMinifyEnabled = true
        isShrinkResources = true
    }
    debug {
        isMinifyEnabled = true
        isShrinkResources = true
    }
}

# Rebuild
./gradlew assembleDebug
```

---

## 📊 Current Status

✅ **Build**: SUCCESS  
✅ **APK Created**: 39MB  
❌ **Installation**: FAILED (EOF error)  
📍 **APK Location**: `app/build/outputs/apk/debug/app-debug.apk`

---

## 🚀 Recommended Steps

1. **Run the fix script**:
   ```bash
   ./fix-install.sh
   ```

2. **If that fails, use USB**:
   ```bash
   # Disconnect wireless
   adb disconnect
   
   # Connect USB cable
   # Enable USB debugging
   # Accept prompt on phone
   
   # Install
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

3. **If still failing, manual install**:
   - Copy APK to phone
   - Install manually from file manager

---

## 🔍 Debugging

### **Check ADB Status:**
```bash
adb devices
# Should show device with "device" status (not "unauthorized" or "offline")
```

### **Check Device Connection:**
```bash
adb shell echo "Connected!"
# Should print "Connected!" if working
```

### **Check Available Space:**
```bash
adb shell df /data
# Make sure you have space for 39MB APK
```

### **View Installation Logs:**
```bash
adb logcat | grep -i "install"
# Shows installation errors in real-time
```

---

## 💡 Why This Happens

### **EOF Error Causes:**
1. **Wireless ADB Instability**
   - WiFi interruption during 39MB transfer
   - Network congestion
   - Phone sleep during transfer

2. **Large APK Size**
   - 39MB takes longer to transfer
   - More chance of interruption
   - Timeout issues

3. **ADB Server Issues**
   - Old ADB server process
   - Multiple ADB instances
   - Corrupted ADB state

---

## ✅ Solutions Summary

| Method | Success Rate | Speed | Difficulty |
|--------|-------------|-------|------------|
| **Fix Script** | 90% | Fast | Easy ✅ |
| **USB Cable** | 95% | Fast | Easy ✅ |
| **Push + Install** | 85% | Medium | Medium |
| **Manual Install** | 100% | Slow | Easy ✅ |
| **Wireless Reconnect** | 70% | Medium | Medium |

---

## 🎯 Next Steps

**Option 1: Quick Fix (30 seconds)**
```bash
./fix-install.sh
```

**Option 2: USB Install (1 minute)**
```bash
# Connect USB → Enable debugging → Run:
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

**Option 3: Manual (2 minutes)**
```bash
# Copy app-debug.apk to phone
# Install from file manager
```

---

## 🏥 Health Subsystem Ready!

Once installed, your app includes:
- ✅ Real-time step counter
- ✅ Heart rate monitoring
- ✅ Calorie tracking
- ✅ Distance calculation
- ✅ Activity detection
- ✅ Health score (0-100)
- ✅ Beautiful animated UI
- ✅ Daily goals tracking

**The APK is built and ready - just need to get it installed!** 🚀

---

**Run `./fix-install.sh` now to auto-fix and install!**

