# Mentra Development Scripts

Convenient shell scripts for building, installing, and managing the Mentra Android app.

---

## 📜 Available Scripts

### 1. `./run.sh` - **Full Build, Install & Launch** ⭐ Recommended

The main development script that does everything in one command.

**What it does:**
1. ✅ Detects all connected devices
2. ✅ Prioritizes devices: **Cable > WiFi > Emulator**
3. ✅ Builds debug APK
4. ✅ Installs on selected device
5. ✅ Stops any running instance
6. ✅ Launches the app
7. ✅ Shows live logcat

**Usage:**
```bash
./run.sh
```

**Output Example:**
```
════════════════════════════════════════
   Mentra - Build & Launch Script
════════════════════════════════════════

✓ Found 1 device(s)
ℹ Cable devices: 24117RN76G

✓ Selected device: SM-S711W (Cable)
ℹ Android version: 15
ℹ Device ID: 24117RN76G

ℹ Building debug APK...
✓ Build successful

ℹ APK size: 8.2M
ℹ Installing app on SM-S711W...
✓ Installation successful

ℹ Launching Mentra...
✓ App launched successfully!

ℹ Opening logcat (Press Ctrl+C to stop)...
```

---

### 2. `./quick-install.sh` - **Clean Build & Install**

Fast script for clean builds and installation with auto-launch.

**What it does:**
1. ✅ Cleans previous build
2. ✅ Builds fresh debug APK
3. ✅ Installs on device
4. ✅ **Launches the app automatically**

**Usage:**
```bash
./quick-install.sh
```

**When to use:**
- After major code changes
- When you get build errors
- To ensure a clean slate

---

### 3. `./devices.sh` - **Device Manager**

Interactive device information and app launcher.

**What it does:**
1. ✅ Lists all connected devices
2. ✅ Shows device details (model, Android version, connection type)
3. ✅ Checks if Mentra is installed
4. ✅ **Offers to launch app on selected device**

**Usage:**
```bash
./devices.sh
```

**Output Example:**
```
════════════════════════════════════════
   Connected Android Devices
════════════════════════════════════════

Device 1: 24117RN76G
  📱 Model: samsung SM-S711W
  🤖 Android: 15 (API 35)
  🔌 Connection: USB Cable
  ✅ Mentra: Installed (v1.0)

════════════════════════════════════════
Total devices: 1

Launch Mentra on a device? (y/n)
> y
🚀 Launching Mentra on 24117RN76G...
✅ App launched!
```

**When to use:**
- Check connected devices
- Verify installation status
- Launch app on specific device
- See device information

---

### 4. `./logs.sh` - **Logcat Viewer**

Real-time log viewer for Mentra app.

**What it does:**
1. ✅ Clears old logs
2. ✅ Shows filtered logs for Mentra
3. ✅ Highlights errors and crashes

**Usage:**
```bash
./logs.sh
```

**Filters:**
- `Mentra:*` - All Mentra logs
- `MainActivity:*` - Activity logs
- `PermissionManager:*` - Permission logs
- `AndroidRuntime:E` - Runtime errors
- `*:F` - Fatal errors

**When to use:**
- Debugging issues
- Monitoring app behavior
- Checking for crashes
- Viewing permission requests

---

### 5. `./start.sh` - **Quick App Launcher** 🆕

Quickly start the already-installed app without building.

**What it does:**
1. ✅ Checks if app is installed
2. ✅ Stops running instance (if any)
3. ✅ Launches app via ADB intent
4. ✅ Verifies app started

**Usage:**
```bash
./start.sh
```

**Output Example:**
```
════════════════════════════════════════
   Mentra - App Launcher
════════════════════════════════════════

📱 Device: SM-S711W
🤖 Android: 15

ℹ Starting app...
✓ App started successfully!
ℹ PID: 12345

View logs with: ./logs.sh
```

**When to use:**
- App is already installed, just want to launch it
- Quick restart during testing
- Testing app startup behavior
- Don't need to rebuild

---

### 6. `./stop.sh` - **Stop App** 🆕

Stop the running Mentra app.

**What it does:**
1. ✅ Checks if app is running
2. ✅ Force-stops the app via ADB

**Usage:**
```bash
./stop.sh
```

**Output Example:**
```
════════════════════════════════════════
   Mentra - Stop App
════════════════════════════════════════

📱 Device: SM-S711W

ℹ Stopping app (PID: 12345)...
✓ App stopped
```

**When to use:**
- Need to completely stop the app
- Testing app restart scenarios
- Clearing app from memory
- Before running clean tests

---

## 🚀 Quick Start Guide

### First Time Setup

1. **Connect your device:**
   ```bash
   # Check if device is connected
   adb devices
   ```

2. **Check device info:**
   ```bash
   ./devices.sh
   ```

3. **Build and run:**
   ```bash
   ./run.sh
   ```

### Daily Development Workflow

**Option A: Quick iteration** (incremental build)
```bash
./run.sh
```

**Option B: Clean build** (after major changes)
```bash
./quick-install.sh
```

**Option C: Just check devices**
```bash
./devices.sh
```

**Option D: Just view logs**
```bash
./logs.sh
```

---

## 📱 Device Priority (run.sh)

When multiple devices are connected, `run.sh` automatically selects:

1. **🔌 USB Cable** (highest priority)
   - Most reliable
   - Fastest data transfer
   - Best for development

2. **📡 WiFi**
   - Good for wireless debugging
   - Requires `adb connect <IP>`

3. **💻 Emulator** (lowest priority)
   - Always available
   - Good for testing

**Example:**
```
Connected devices:
✓ Cable: Samsung Galaxy (Selected)
  WiFi: OnePlus 9
  Emulator: Pixel 6 API 35

Selected: Samsung Galaxy (Cable)
```

---

## 🔧 Troubleshooting

### No devices detected
```bash
# Check ADB is installed
adb version

# List devices
adb devices

# Restart ADB server
adb kill-server
adb start-server
```

### App won't launch
```bash
# Check if installed
adb shell pm list packages | grep mentra

# Check logcat for errors
./logs.sh
```

### Build fails
```bash
# Clean build
./gradlew clean

# Or use quick-install
./quick-install.sh
```

### Permission denied on scripts
```bash
# Make scripts executable
chmod +x *.sh
```

---

## 📊 What Each Script Launches

| Script | Builds APK | Installs | Launches App | Shows Logs | Stops App |
|--------|-----------|----------|--------------|------------|-----------|
| `run.sh` | ✅ | ✅ | ✅ | ✅ | ❌ |
| `quick-install.sh` | ✅ | ✅ | ✅ | ❌ | ❌ |
| `start.sh` 🆕 | ❌ | ❌ | ✅ | ❌ | ❌ |
| `stop.sh` 🆕 | ❌ | ❌ | ❌ | ❌ | ✅ |
| `devices.sh` | ❌ | ❌ | ✅* | ❌ | ❌ |
| `logs.sh` | ❌ | ❌ | ❌ | ✅ | ❌ |

*Interactive - asks user confirmation

---

## 💡 Pro Tips

### Combine Scripts
```bash
# Start app and monitor logs in one terminal
./start.sh && ./logs.sh

# Stop, then start (clean restart)
./stop.sh && sleep 1 && ./start.sh

# Build, install, and monitor logs separately
./quick-install.sh  # Terminal 1
./logs.sh           # Terminal 2
```
```bash
# Build, install, and monitor logs
./run.sh

# In another terminal window
./logs.sh
```

### WiFi Debugging
```bash
# Connect device via WiFi
adb tcpip 5555
adb connect 192.168.1.100:5555

# Now run.sh will detect it
./run.sh
```

### Quick Reinstall
```bash
# Uninstall
adb uninstall com.example.mentra

# Clean install
./quick-install.sh
```

### Monitor Specific Component
```bash
# Edit logs.sh to add your tags
# Example: Add "HealthTracker:*"
```

---

## 🎯 Common Workflows

### 1. Feature Development
```bash
# Make code changes
# ...

# Test on device
./run.sh
```

### 2. Bug Fixing
```bash
# Reproduce bug
./logs.sh          # Watch logs

# Make fix
# ...

# Test fix
./run.sh
```

### 3. Clean Build
```bash
# When things get weird
./quick-install.sh
```

### 4. Multi-Device Testing
```bash
# Check all devices
./devices.sh

# Will show all devices and let you choose
```

---

## 📝 Notes

- All scripts stop the app before launching to ensure clean start
- `run.sh` shows live logcat (Ctrl+C to stop)
- Scripts work with USB, WiFi, and emulator devices
- APK location: `app/build/outputs/apk/debug/app-debug.apk`
- Package name: `com.example.mentra`
- Main activity: `.MainActivity`

---

## 🚦 Script Exit Codes

- **0**: Success
- **1**: Error (device not found, build failed, etc.)

---

## 🔄 What Happens When You Launch

### run.sh Flow:
```
Detect devices
    ↓
Prioritize (Cable > WiFi > Emulator)
    ↓
Show device info
    ↓
Build APK (./gradlew assembleDebug)
    ↓
Install APK (adb install -r)
    ↓
Stop any running instance
    ↓
Launch app (am start)
    ↓
Verify app is running
    ↓
Show live logcat
```

### quick-install.sh Flow:
```
Clean build (./gradlew clean)
    ↓
Build APK (./gradlew assembleDebug)
    ↓
Install (./gradlew installDebug)
    ↓
Launch app on device
    ↓
Done!
```

---

**Happy Coding! 🚀**

For issues or questions, check the logs with `./logs.sh`

