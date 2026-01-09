# 🔥 Shizuku Setup Guide for Mentra

## What is Shizuku?

Shizuku allows apps to use system APIs directly with elevated permissions WITHOUT root. It's like having root access, but safer and reversible.

---

## 📋 Installation Steps

### **Step 1: Install Shizuku App**

1. **Download Shizuku** from one of these sources:
   - Google Play Store: https://play.google.com/store/apps/details?id=moe.shizuku.privileged.api
   - GitHub Releases: https://github.com/RikkaApps/Shizuku/releases
   - F-Droid (if available)

2. **Install the APK**

---

### **Step 2: Start Shizuku Service**

#### **Method 1: Wireless ADB (Recommended - No PC needed after initial setup)**

1. **Enable Developer Options** on your phone:
   - Go to Settings → About Phone
   - Tap "Build Number" 7 times
   - Developer Options will appear

2. **Enable Wireless Debugging**:
   - Settings → Developer Options → Wireless Debugging
   - Turn it ON
   - Note the port number (e.g., 5555)

3. **Connect via ADB from PC** (one-time setup):
   ```bash
   # Get your phone's IP address (Settings → About Phone → Status)
   # Example IP: 192.168.1.100
   
   adb pair 192.168.1.100:PAIRING_PORT
   # Enter the pairing code shown on your phone
   
   adb connect 192.168.1.100:5555
   # Should say "connected"
   ```

4. **Start Shizuku**:
   - Open Shizuku app
   - Tap "Start via Wireless Debugging"
   - Shizuku should start and show "Running"

#### **Method 2: USB ADB (Requires PC each time)**

1. **Enable USB Debugging**:
   - Settings → Developer Options → USB Debugging
   - Turn it ON

2. **Connect phone to PC via USB**

3. **Start Shizuku**:
   ```bash
   # On your PC
   adb shell sh /data/user/0/moe.shizuku.privileged.api/start.sh
   ```

4. **Open Shizuku app** - Should show "Running"

#### **Method 3: Rooted Device (If you have root)**

1. Open Shizuku app
2. Tap "Start via Root"
3. Grant root permission
4. Done!

---

### **Step 3: Grant Mentra Permission in Shizuku**

1. **Open Shizuku app**
2. Go to **"Authorized Apps"** tab
3. You should see **Mentra** in the list
4. If not listed, open Mentra app first, then check again
5. **Enable the toggle** next to Mentra
6. Grant permission when prompted

---

### **Step 4: Verify in Mentra**

1. Open Mentra app
2. Go to AI Shell
3. Try a system command:
   ```bash
   show battery
   ```
   
4. If it works without "Shizuku not available" error, you're good!

5. Try a privileged command:
   ```bash
   brightness 200
   wifi --state=on
   ```

---

## 🚀 Quick Setup Script (For PC Users)

Save this as `setup-shizuku.sh`:

```bash
#!/bin/bash

echo "🔥 Mentra - Shizuku Setup Script"
echo "================================"
echo ""

# Check if device is connected
DEVICE=$(adb devices | grep -v "List" | grep "device$" | head -1 | awk '{print $1}')

if [ -z "$DEVICE" ]; then
    echo "❌ No device connected via ADB"
    echo "Please connect your device and enable USB debugging"
    exit 1
fi

echo "✓ Device connected: $DEVICE"
echo ""

# Check if Shizuku is installed
SHIZUKU_INSTALLED=$(adb -s "$DEVICE" shell pm list packages | grep "moe.shizuku.privileged.api")

if [ -z "$SHIZUKU_INSTALLED" ]; then
    echo "❌ Shizuku not installed"
    echo "Please install Shizuku from Play Store or GitHub"
    exit 1
fi

echo "✓ Shizuku is installed"
echo ""

# Start Shizuku
echo "Starting Shizuku service..."
adb -s "$DEVICE" shell sh /data/user/0/moe.shizuku.privileged.api/start.sh

sleep 2

# Check if Shizuku is running
SHIZUKU_RUNNING=$(adb -s "$DEVICE" shell ps | grep "shizuku")

if [ -z "$SHIZUKU_RUNNING" ]; then
    echo "⚠️  Shizuku may not be running"
    echo "Please open Shizuku app and check status"
else
    echo "✓ Shizuku service started"
fi

echo ""
echo "================================"
echo "Next steps:"
echo "1. Open Shizuku app on your phone"
echo "2. Verify it shows 'Running'"
echo "3. Go to 'Authorized Apps' tab"
echo "4. Enable Mentra"
echo "5. Open Mentra and test commands!"
echo ""
echo "Test command: show battery"
echo "================================"
```

---

## 🔧 Troubleshooting

### **Shizuku won't start**
- Make sure USB Debugging is enabled
- Try disconnecting and reconnecting USB
- Restart ADB: `adb kill-server && adb start-server`
- Try Method 2 (USB ADB) instead of wireless

### **Mentra not showing in Shizuku authorized apps**
- Open Mentra app first
- Close and reopen Shizuku app
- The app should appear in the list

### **Commands still show "Shizuku not available"**
- Check Shizuku app - is it showing "Running"?
- Check if Mentra is enabled in "Authorized Apps"
- Try restarting Mentra app
- Try restarting Shizuku service

### **Shizuku stops after phone reboot**
- You need to restart Shizuku after every reboot
- Use Method 1 (Wireless) for persistent access
- Or use Method 3 (Root) if you have root

---

## 📱 Testing Commands

### **After Shizuku is set up, test these**:

```bash
# Should work WITHOUT Shizuku (always work)
show battery
show storage
show device
open chrome

# Should work WITH Shizuku (system control)
brightness 150
wifi --state=on
performance high
clearram
```

### **Expected Results**:

**Without Shizuku**:
```
$ brightness 150
Failed to set brightness: Shizuku not available - requires setup
```

**With Shizuku Running**:
```
$ brightness 150
Brightness set to 150
```

---

## 🎯 What You Get with Shizuku

Once Shizuku is running, you unlock:

### ⚡ **Power Control**
- Reboot device
- Shutdown device
- Sleep/lock screen

### 🌐 **Network Management**
- Toggle WiFi
- Toggle mobile data
- Airplane mode
- Bluetooth control

### 💡 **Display Control**
- Set brightness (0-255)
- Screen timeout
- Auto-brightness

### 🔊 **Audio Control**
- Set volume for all streams
- Mute/unmute

### 📱 **App Management**
- Freeze/unfreeze apps
- Hide/unhide apps
- Install/uninstall APKs

### 🚀 **Performance**
- Performance modes
- Battery saver
- Clear RAM
- Clear all caches

### ⚙️ **Settings**
- Developer mode
- USB debugging
- Animations
- Location services
- And much more!

---

## 🔒 Security Notes

- Shizuku is safe - it doesn't modify system files
- All operations are reversible
- Uninstalling Shizuku removes all permissions
- Mentra only uses Shizuku when you explicitly run commands
- No background operations
- All command history is local

---

## 📚 Additional Resources

- **Shizuku GitHub**: https://github.com/RikkaApps/Shizuku
- **Shizuku Documentation**: https://shizuku.rikka.app/
- **Mentra Commands Guide**: See SYSTEM-COMMANDS-GUIDE.md

---

## ✅ Quick Checklist

- [ ] Install Shizuku app
- [ ] Start Shizuku service (Method 1, 2, or 3)
- [ ] Verify Shizuku shows "Running"
- [ ] Grant Mentra permission in Shizuku
- [ ] Test `show battery` in Mentra (should work)
- [ ] Test `brightness 200` in Mentra (should work with Shizuku)
- [ ] Enjoy total system control! 🔥

---

**Once setup is complete, you have the most powerful Android shell ever created!** 🎉

