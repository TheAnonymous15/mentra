# Mentra System Shell - Super Administrator Commands

## 🔥 SYSTEM-WIDE CONTROL - Complete Command Reference

This shell gives you TOTAL system control via Shizuku. Use with caution!

---

## ⚡ POWER MANAGEMENT

```bash
shutdown                  # Shutdown device
reboot                    # Reboot device
reboot --mode=recovery    # Reboot to recovery mode
reboot --mode=bootloader  # Reboot to bootloader/fastboot
reboot --mode=safe        # Reboot in safe mode
sleep                     # Put device to sleep
lock                      # Lock screen
```

---

## 🌐 NETWORK MANAGEMENT

```bash
# WiFi
wifi --state=on           # Enable WiFi
wifi --state=off          # Disable WiFi

# Mobile Data
data --state=on           # Enable mobile data
data --state=off          # Disable mobile data

# Airplane Mode
airplane --state=on       # Enable airplane mode
airplane --state=off      # Disable airplane mode

# Bluetooth
bluetooth --state=on      # Enable Bluetooth
bt --state=off            # Disable Bluetooth (short form)
```

---

## 💡 DISPLAY & BRIGHTNESS

```bash
brightness 128            # Set brightness (0-255)
brightness 255            # Max brightness
brightness 0              # Min brightness

autobrightness --state=on # Enable auto-brightness
autobrightness --state=off # Disable auto-brightness

timeout 30                # Screen timeout 30 seconds
timeout 300               # Screen timeout 5 minutes
```

---

## 🔊 VOLUME & AUDIO

```bash
volume --type=music 10    # Set music volume
volume --type=ring 15     # Set ring volume
volume --type=notification 8  # Set notification volume
volume --type=alarm 12    # Set alarm volume

mute --state=on           # Mute all audio
mute --state=off          # Unmute all audio
```

---

## 📱 APP MANAGEMENT (Advanced)

```bash
freeze com.example.app    # Disable/freeze app
unfreeze com.example.app  # Enable/unfreeze app
hide com.example.app      # Hide app from launcher
unhide com.example.app    # Unhide app

install /path/to/app.apk  # Install APK
uninstall com.example.app # Uninstall app
cleardata com.example.app # Clear app data
```

---

## ⏰ TIME & DATE

```bash
settime 1704844800000     # Set system time (Unix timestamp)
settimezone America/New_York  # Set timezone
autotime --state=on       # Enable automatic time
autotime --state=off      # Disable automatic time
```

---

## ⚙️ SYSTEM SETTINGS

```bash
# Developer Options
developermode --state=on  # Enable developer mode
adb --state=on            # Enable USB debugging
stayawake --state=on      # Stay awake when charging

# Animations
animations 0.5            # Set animation scale to 0.5x
animations 1.0            # Normal animations
animations 0              # Disable animations

# Location
location --state=on       # Enable location services
location --state=off      # Disable location services
```

---

## 🚀 PERFORMANCE & BATTERY

```bash
performance high          # High performance mode
performance balanced      # Balanced mode
performance powersave     # Power saving mode

batterysaver --state=on   # Enable battery saver
batterysaver --state=off  # Disable battery saver

clearram                  # Clear RAM (kill background)
clearcache                # Trim all app caches
```

---

## 🔔 NOTIFICATIONS

```bash
dnd --state=on            # Enable Do Not Disturb
dnd --state=off           # Disable Do Not Disturb

notify "Hello World" --title="Test"  # Send system notification
```

---

## 📊 SYSTEM INFORMATION

```bash
sysinfo                   # Comprehensive system info
show battery              # Battery status
show storage              # Storage information
show network              # Network information
show memory               # Memory usage
show device               # Device information
```

---

## 💾 STORAGE MANAGEMENT

```bash
ls /sdcard                # List files
cat /sdcard/file.txt      # Read file
rm /sdcard/file.txt       # Delete file
cp /src /dest             # Copy file
mv /src /dest             # Move file
```

---

## 🛠️ ADVANCED OPERATIONS

```bash
# Direct system commands
exec "your custom command"  # Execute any system command

# Package management
pm list packages          # List all packages
pm list packages -3       # List 3rd party packages
pm list packages -s       # List system packages

# Service management
dumpsys battery           # Battery dump
dumpsys meminfo           # Memory info
dumpsys cpuinfo           # CPU info
```

---

## 🎨 ZSH-LIKE FEATURES

### Auto-completion
- Press TAB for command completion
- Press TAB twice for suggestions

### Command History
```bash
history                   # View command history
history 20                # View last 20 commands
!!                        # Repeat last command
!10                       # Repeat command #10
```

### Aliases
```bash
alias ll="ls -la"         # Create alias
alias shutdown="shutdown" # Create shortcut
alias                     # List all aliases
```

### Environment Variables
```bash
export THEME=dark         # Set environment variable
env                       # Show all environment variables
echo $THEME               # Display variable
```

---

## ⚠️ CRITICAL OPERATIONS

**These operations require confirmation:**

```bash
shutdown                  # Requires confirmation
reboot                    # Requires confirmation
rm -rf /                  # Dangerous - requires confirmation
```

---

## 🎯 USAGE EXAMPLES

### Power user workflow
```bash
# Morning routine
brightness 200
volume --type=music 10
wifi --state=on
data --state=off

# Performance tuning
performance high
animations 0.5
clearram

# Night mode
brightness 50
dnd --state=on
batterysaver --state=on
```

### System maintenance
```bash
# Clean up
clearram
clearcache
pm trim-caches

# Check status
sysinfo
show battery
show storage
show memory
```

### Network management
```bash
# Switch to WiFi only
wifi --state=on
data --state=off
airplane --state=off

# Airplane mode (all off)
airplane --state=on
```

---

## 💡 TIPS & TRICKS

1. **Chain commands**: `wifi --state=on; data --state=off`
2. **Use aliases**: Create shortcuts for frequent commands
3. **Tab completion**: Type partial command + TAB
4. **History search**: Use `history | grep wifi`
5. **Clear screen**: Type `clear` or press Ctrl+L

---

## 🔒 SECURITY NOTES

- All privileged commands require Shizuku
- Critical operations require confirmation
- Command history is saved locally
- No commands are sent to external servers

---

## 📚 HELP COMMANDS

```bash
help                      # General help
syshelp                   # This system commands guide
help network              # Network commands help
help power                # Power commands help
man <command>             # Command manual (if available)
```

---

**Mentra Shell v2.0 - System Administrator Edition**  
**Type 'syshelp' anytime to see this guide**  
**Type 'help' for general commands**

🔥 You now have FULL SYSTEM CONTROL! 🔥

