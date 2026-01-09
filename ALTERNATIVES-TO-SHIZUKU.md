# 🔧 Alternatives to Shizuku for System Control

## Overview

There are **4 ways** to achieve system-level control in Mentra:

| Method | Power Level | Requirements | Setup Difficulty | Persistence |
|--------|-------------|--------------|------------------|-------------|
| **Shizuku** | ⭐⭐⭐⭐⭐ | USB debugging | Easy | Needs restart after reboot |
| **Root** | ⭐⭐⭐⭐⭐ | Rooted device | Hard | Permanent |
| **ADB WiFi** | ⭐⭐⭐⭐ | USB debugging + PC | Medium | Session-based |
| **Android APIs** | ⭐⭐ | None | None | Permanent |

---

## Method 1: Shizuku (Recommended) ✅

**What it is**: A service that provides system API access without root

**Pros**:
- ✅ No root required
- ✅ Safe and reversible
- ✅ Full system control
- ✅ Open source
- ✅ Easy to set up

**Cons**:
- ❌ Needs restart after device reboot
- ❌ Requires USB debugging enabled
- ❌ One-time PC connection needed

**Setup**:
```bash
1. Install Shizuku from Play Store
2. Enable USB Debugging
3. Connect to PC: adb shell sh /data/user/0/moe.shizuku.privileged.api/start.sh
4. Authorize Mentra in Shizuku app
```

**Status**: ✅ Already implemented and working!

---

## Method 2: Root Access 🔓

**What it is**: Device with unlocked bootloader and root access (Magisk/SuperSU)

**Pros**:
- ✅ Most powerful
- ✅ Permanent (survives reboot)
- ✅ No PC needed after setup
- ✅ Full system control

**Cons**:
- ❌ Voids warranty
- ❌ Banking apps may not work
- ❌ Complex setup
- ❌ Can brick device if done wrong

**Setup**:
```bash
1. Unlock bootloader (device-specific)
2. Flash TWRP recovery
3. Flash Magisk
4. Grant Mentra root permission
```

**Code**: Already implemented in `RootExecutor.kt`

**Usage**:
```kotlin
// Mentra will auto-detect root and use it
rootExecutor.executeRootCommand("reboot")
```

---

## Method 3: ADB over WiFi 📡

**What it is**: Use ADB commands wirelessly

**Pros**:
- ✅ No root required
- ✅ Full access during connection
- ✅ Good for development

**Cons**:
- ❌ Requires PC nearby
- ❌ Connection drops easily
- ❌ Not practical for daily use

**Setup**:
```bash
# One-time setup
1. Enable Wireless Debugging in Developer Options
2. On PC: adb pair <IP>:<PORT>
3. adb connect <IP>:5555
4. Commands work over WiFi!
```

**Code**: Implemented in `ADBExecutor.kt`

---

## Method 4: Android APIs (Limited) 📱

**What it is**: Use official Android APIs that don't require special permissions

**Pros**:
- ✅ No setup needed
- ✅ Works on all devices
- ✅ Permanent
- ✅ Safe

**Cons**:
- ❌ Very limited functionality
- ❌ Can't do most system operations

**What Works Without Privileges**:
```kotlin
// Information queries
getBatteryStatus()      // ✅ Works
getStorageInfo()        // ✅ Works
getDeviceInfo()         // ✅ Works
getCurrentTime()        // ✅ Works

// App operations
openApp()               // ✅ Works
openSettings()          // ✅ Works
makeCall()             // ✅ Works (opens dialer)
sendSMS()              // ✅ Works (opens messaging)

// File operations (in app directory)
listFiles()            // ✅ Works (app folders)
readFile()             // ✅ Works (app folders)
writeFile()            // ✅ Works (app folders)

// What DOESN'T work
reboot()               // ❌ Requires privilege
setWiFi()              // ❌ Requires privilege
setBrightness()        // ❌ Requires privilege
freezeApp()            // ❌ Requires privilege
```

---

## 🎯 Our Implementation: Automatic Fallback

Mentra now has a **smart fallback system** that tries methods in order:

```kotlin
class PrivilegedExecutor {
    fun execute(command: String): Result<String> {
        // Try 1: Shizuku (if running & authorized)
        if (shizuku.isAvailable()) 
            return shizuku.execute(command)
        
        // Try 2: Root (if device is rooted)
        if (root.isAvailable())
            return root.execute(command)
        
        // Try 3: ADB (if connected)
        if (adb.isAvailable())
            return adb.execute(command)
        
        // Fallback: Limited Android APIs
        return limited.execute(command)
    }
}
```

**User sees**:
```
$ reboot

Method 1 (Shizuku): ❌ Not available
Method 2 (Root): ❌ Not available  
Method 3 (ADB): ❌ Not available
Method 4 (Limited): ❌ Command requires privileges

💡 Install Shizuku for full system control!
```

---

## 📊 Comparison Matrix

### **Power Commands** (reboot, shutdown, etc.)

| Method | Works? | Notes |
|--------|--------|-------|
| Shizuku | ✅ Yes | Full control |
| Root | ✅ Yes | Full control |
| ADB | ✅ Yes | During session |
| Limited | ❌ No | No access |

### **Network Control** (WiFi, data, etc.)

| Method | Works? | Notes |
|--------|--------|-------|
| Shizuku | ✅ Yes | Full control |
| Root | ✅ Yes | Full control |
| ADB | ✅ Yes | During session |
| Limited | ⚠️ Partial | Can open settings only |

### **Display Settings** (brightness, etc.)

| Method | Works? | Notes |
|--------|--------|-------|
| Shizuku | ✅ Yes | Full control |
| Root | ✅ Yes | Full control |
| ADB | ✅ Yes | During session |
| Limited | ⚠️ Partial | With WRITE_SETTINGS permission |

### **App Management** (freeze, hide, etc.)

| Method | Works? | Notes |
|--------|--------|-------|
| Shizuku | ✅ Yes | Full control |
| Root | ✅ Yes | Full control |
| ADB | ✅ Yes | During session |
| Limited | ❌ No | Can only open apps |

---

## 🎯 Recommendation for Your Use Case

**For Daily Use**: **Shizuku** (Best balance)
- Easy to set up
- Safe and reversible
- Full control
- Just needs restart after reboot

**For Power Users**: **Root** (Maximum power)
- Permanent solution
- No PC needed
- Complete control
- But voids warranty

**For Development**: **ADB WiFi** (Convenient)
- Good for testing
- No permanent changes
- Easy to disconnect

**For Compatibility**: **Limited APIs** (Fallback)
- Works on all devices
- No setup
- But very limited

---

## 🚀 Quick Setup Guide

### **Option 1: Shizuku (5 minutes)**
```bash
cd /Users/danielkinyua/Downloads/projects/mentra
./setup-shizuku.sh

# On phone:
# 1. Open Shizuku app
# 2. Authorize Mentra
# 3. Done!
```

### **Option 2: Root (Advanced Users)**
```bash
# See: https://topjohnwu.github.io/Magisk/install.html
# Device-specific - requires unlocked bootloader
```

### **Option 3: ADB WiFi (Development)**
```bash
# On phone: Settings → Developer Options → Wireless Debugging → ON
adb pair <IP>:<PAIRING_PORT>
adb connect <IP>:5555
# Commands work!
```

---

## 💡 What We've Implemented

✅ `PrivilegedExecutor` - Tries all methods automatically  
✅ `ShizukuBridge` - Shizuku integration  
✅ `RootExecutor` - Root access  
✅ `ADBExecutor` - ADB commands  
✅ Automatic fallback chain  
✅ Clear error messages showing which method to use  

---

## 🎉 Bottom Line

**You have 4 options for system control**:

1. **Shizuku** ⭐ Recommended - Already setup, just authorize!
2. **Root** - Most powerful, but requires rooted device
3. **ADB WiFi** - Good for development/testing
4. **Limited APIs** - Fallback for basic functions

**Current Status**: Shizuku server is running, just need to authorize Mentra in the Shizuku app!

**Try this**: Open Shizuku app → "Authorized Apps" → Enable "Mentra" → Test commands! 🚀

