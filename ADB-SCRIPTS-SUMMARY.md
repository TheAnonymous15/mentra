# ✅ ADB Scripts Complete!

Yes! There are now **6 shell scripts** for ADB operations, including dedicated scripts for starting and stopping the app.

---

## 🎯 Quick Answer

### Start App via ADB Intent:
```bash
./start.sh
```

### Stop App:
```bash
./stop.sh
```

---

## 📜 All Available Scripts

### 1. **./run.sh** - Full Pipeline ⭐
- Builds APK
- Installs on device
- **Starts app via ADB intent** ✅
- Shows live logs
- **Best for development**

### 2. **./quick-install.sh** - Clean Build & Launch
- Clean build
- Install
- **Starts app via ADB intent** ✅
- **Best for major changes**

### 3. **./start.sh** - Just Launch 🆕
- **Starts app via ADB intent** ✅
- No build, no install
- Quick and simple
- **Best for quick restart**

### 4. **./stop.sh** - Stop App 🆕
- Stops running app
- Force-stop via ADB
- **Best for clean shutdown**

### 5. **./devices.sh** - Device Manager
- Lists devices
- Can launch app interactively
- **Best for checking devices**

### 6. **./logs.sh** - Monitor Logs
- Filtered logcat viewer
- No launch
- **Best for debugging**

---

## 🚀 ADB Intent Commands Used

All scripts use the proper ADB intent command:

```bash
adb shell am start -n com.example.mentra/.MainActivity
```

This:
- ✅ Launches the MainActivity
- ✅ Uses explicit intent
- ✅ Works reliably across all Android versions
- ✅ Returns immediately
- ✅ Can be verified with `pidof` command

---

## 💡 Common Use Cases

### Just want to start the app:
```bash
./start.sh
```

### Restart the app cleanly:
```bash
./stop.sh && ./start.sh
```

### Start and watch logs:
```bash
./start.sh
./logs.sh  # In another terminal
```

### Full development cycle:
```bash
# Make code changes
# ...

./run.sh  # Builds, installs, starts, shows logs
```

### Quick iteration (no clean build):
```bash
# Make code changes
# ...

./quick-install.sh  # Fast build, install, start
```

---

## 📊 Which Script to Use When?

| Scenario | Use This | Why |
|----------|----------|-----|
| App already installed, just launch | `./start.sh` | Fastest |
| App running, need clean restart | `./stop.sh && ./start.sh` | Clean state |
| Made code changes | `./run.sh` | All-in-one |
| Major refactor, need clean build | `./quick-install.sh` | Clean build |
| Check which devices connected | `./devices.sh` | Device info |
| Monitor app behavior | `./logs.sh` | Filtered logs |
| App crashed, need to restart | `./start.sh` | Quick restart |

---

## 🔧 Under the Hood

### start.sh does:
1. Checks ADB availability
2. Finds connected device
3. Verifies app is installed
4. Stops app if running (clean state)
5. **Launches via: `adb shell am start -n <activity>`**
6. Verifies app started (checks PID)
7. Shows success/failure

### stop.sh does:
1. Checks ADB availability
2. Finds connected device
3. Checks if app is running (pidof)
4. **Stops via: `adb shell am force-stop <package>`**
5. Confirms stopped

---

## ✅ All Scripts Are:
- ✅ **Executable** (`chmod +x *.sh`)
- ✅ **Color-coded** output (Green/Blue/Yellow/Red)
- ✅ **Error handling** (checks devices, installation, etc.)
- ✅ **Device detection** (finds connected devices automatically)
- ✅ **User-friendly** (clear messages and instructions)

---

## 📝 Example Usage

### Scenario 1: Fresh Development Session
```bash
# Check devices
./devices.sh

# Build and run
./run.sh
```

### Scenario 2: Quick Testing Loop
```bash
# Make changes, then:
./start.sh  # Quick restart

# Or if you changed code:
./quick-install.sh  # Rebuild and restart
```

### Scenario 3: Debugging
```bash
# Terminal 1: Start app
./start.sh

# Terminal 2: Watch logs
./logs.sh

# When done:
./stop.sh
```

### Scenario 4: Clean State Testing
```bash
# Stop app completely
./stop.sh

# Wait a moment
sleep 2

# Start fresh
./start.sh
```

---

## 🎯 Summary

**Yes, there are ADB start scripts!**

- **`./run.sh`** - Builds, installs, and **starts via ADB intent**
- **`./quick-install.sh`** - Clean build and **starts via ADB intent**  
- **`./start.sh`** 🆕 - **Just starts via ADB intent** (no build)
- **`./stop.sh`** 🆕 - Stops the app

All use proper ADB commands:
- **Start**: `adb shell am start -n com.example.mentra/.MainActivity`
- **Stop**: `adb shell am force-stop com.example.mentra`

---

**Try it now:**
```bash
./start.sh
```

🚀 Your app will launch on the connected device!

