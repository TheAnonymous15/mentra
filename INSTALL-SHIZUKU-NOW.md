# 🚀 Quick Shizuku Installation Guide

## Your Devices
- **Physical Phone**: 24117RN76G (nvf66t6tuohit8rk) - USB connected ✅
- **Emulator**: emulator-5554

---

## 📥 Installation Methods

### **Method 1: Play Store (Easiest)**
```
1. On your phone, open Play Store
2. Search "Shizuku"
3. Install
4. Done!
```

### **Method 2: Automated Script**
```bash
cd /Users/danielkinyua/Downloads/projects/mentra
./install-shizuku.sh
```
The script will:
- Check if Shizuku is already installed
- Look for APK in ~/Downloads
- Install on your device
- Show next steps

### **Method 3: Manual ADB Install**
```bash
# 1. Download APK
curl -L -o ~/Downloads/shizuku.apk \
  'https://github.com/RikkaApps/Shizuku/releases/download/v13.5.4.r1044.27e2f81/shizuku-v13.5.4.r1044.27e2f81-release.apk'

# 2. Install on your device
adb -s nvf66t6tuohit8rk install ~/Downloads/shizuku.apk

# 3. Check installation
adb -s nvf66t6tuohit8rk shell pm list packages | grep shizuku
```

---

## ⚙️ After Installation

### **1. Enable Developer Options**
```
Settings → About Phone → Tap "Build Number" 7 times
```

### **2. Enable USB Debugging**
```
Settings → Developer Options → USB Debugging → ON
```

### **3. Start Shizuku Service**
```bash
# Run the setup script
cd /Users/danielkinyua/Downloads/projects/mentra
./setup-shizuku.sh

# OR manually
adb -s nvf66t6tuohit8rk shell sh /data/user/0/moe.shizuku.privileged.api/start.sh
```

### **4. Verify in Shizuku App**
```
Open Shizuku app → Should show "Running"
```

### **5. Grant Mentra Permission**
```
Shizuku app → "Authorized Apps" → Enable "Mentra"
```

---

## ✅ Testing

### **In Mentra Shell:**
```bash
# Test without Shizuku (always works)
show battery

# Test with Shizuku (requires Shizuku)
brightness 200
wifi --state=on
```

### **Expected Results:**

**Without Shizuku:**
```
$ brightness 200
Failed to set brightness: Shizuku not available
```

**With Shizuku:**
```
$ brightness 200
Brightness set to 200
✓ SUCCESS!
```

---

## 🔧 Quick Commands Reference

```bash
# Check if Shizuku is installed
adb -s nvf66t6tuohit8rk shell pm list packages | grep shizuku

# Start Shizuku service
adb -s nvf66t6tuohit8rk shell sh /data/user/0/moe.shizuku.privileged.api/start.sh

# Check if Shizuku is running
adb -s nvf66t6tuohit8rk shell ps | grep shizuku

# Launch Shizuku app
adb -s nvf66t6tuohit8rk shell am start -n moe.shizuku.privileged.api/.ShizukuActivity
```

---

## 📱 Your Device Selector

When using adb commands, use: **`-s nvf66t6tuohit8rk`**

```bash
# Examples:
adb -s nvf66t6tuohit8rk shell [command]
adb -s nvf66t6tuohit8rk install [apk]
```

---

## 🎯 Complete Workflow

```bash
# 1. Install Shizuku
./install-shizuku.sh

# 2. Setup Shizuku
./setup-shizuku.sh

# 3. Start Mentra
./start.sh

# 4. Test in shell
# Type: help
# Type: show battery
# Type: brightness 200
```

---

## 📚 Full Documentation

- **SHIZUKU-SETUP-GUIDE.md** - Complete setup instructions
- **SHIZUKU-READY.md** - What you can do with Shizuku
- **SYSTEM-COMMANDS-GUIDE.md** - All 75+ commands

---

**Ready to install? Run:**
```bash
cd /Users/danielkinyua/Downloads/projects/mentra
./install-shizuku.sh
```

