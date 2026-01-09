# Phase 6: Offline Multilingual AI Shell - Implementation Plan

## 🎯 Overview

Building the AI Shell FIRST as recommended by guide.txt:
> "Build the shell first; everything else plugs into it"

This will become the **central nervous system** of Mentra, controlling all features through natural language.

---

## 📋 Implementation Order

### Stage 1: Core Shell Engine (Week 1-2)
**Priority: CRITICAL**

1. **Shell Command Parser**
   - Lexer (tokenization)
   - Parser (AST generation)
   - Command validation
   
2. **Shell Executor**
   - Command execution engine
   - Result handling
   - Error management
   
3. **Context Manager**
   - Session state
   - Environment variables
   - Command history

4. **Action System**
   - Action data models
   - Result wrappers
   - Capability system

### Stage 2: Action Handlers (Week 2-3)
**Priority: HIGH**

1. **System Actions**
   - Open app
   - Launch activity
   - System settings
   
2. **Communication Actions**
   - Make call
   - Send SMS
   - Open contacts
   
3. **Media Actions**
   - Play music
   - Control playback
   - Manage playlists
   
4. **Query Actions**
   - Device info
   - Battery status
   - Storage info
   - App usage stats

### Stage 3: Shizuku Bridge (Week 3-4)
**Priority: HIGH**

1. **Shizuku Integration**
   - Permission checking
   - Service binding
   - API wrappers
   
2. **Privileged Actions**
   - Install/uninstall apps
   - Grant/revoke permissions
   - System settings modification
   - File system access

### Stage 4: AI Interpretation Layer (Week 4-6)
**Priority: MEDIUM**

1. **Intent Recognition**
   - Rule-based matcher
   - Keyword extraction
   - Confidence scoring
   
2. **Entity Extraction**
   - Contact names
   - App names
   - Numbers/quantities
   - Locations
   
3. **Alias System**
   - Contact aliases (wife, mom, etc.)
   - App aliases (browser, music, etc.)
   - Custom user aliases

### Stage 5: UI Clients (Week 6-7)
**Priority: HIGH**

1. **Overlay Client**
   - Global overlay window
   - Edge swipe activation
   - Quick command input
   
2. **Fullscreen CLI**
   - Terminal-like interface
   - Command history navigation
   - Auto-completion
   
3. **Quick Panel**
   - Recent commands
   - Favorites
   - Suggestions

### Stage 6: Scripting & Automation (Week 7-8)
**Priority: MEDIUM**

1. **Script Engine**
   - Script parser
   - Script executor
   - Variable substitution
   
2. **Automation Triggers**
   - On boot
   - Headphone plug/unplug
   - App open/close
   - Time-based
   - Location-based
   
3. **Virtual Filesystem**
   - /config
   - /scripts
   - /plugins
   - /history
   - /env

### Stage 7: Multilingual Support (Week 8-10)
**Priority: LOW (Start with English)**

1. **Language Detection**
   - Offline language ID
   - User language preference
   
2. **Translation Pipeline**
   - User Lang → English → Shell
   - Shell → English → User Lang
   
3. **Offline Models**
   - TensorFlow Lite models
   - FastText language detection
   - Small translation models

### Stage 8: Voice Interface (Week 10-11)
**Priority: LOW (Optional)**

1. **Speech Recognition**
   - Offline STT
   - Wake word detection
   
2. **Text-to-Speech**
   - Offline TTS
   - Response reading

### Stage 9: Plugin System (Week 11-12)
**Priority: MEDIUM**

1. **Plugin SDK**
   - Command registration
   - Handler interface
   - Permission requests
   
2. **Plugin Manager**
   - Install/uninstall
   - Enable/disable
   - Update mechanism

---

## 🚀 Let's Start: Stage 1 - Core Shell Engine

### What We'll Build First:

1. ✅ **Data Models** (Already done in database)
2. 🔨 **Command Parser**
3. 🔨 **Shell Executor**
4. 🔨 **Context Manager**
5. 🔨 **Action Router**

---

## 📁 Module Structure

```
app/src/main/java/com/example/mentra/
├── shell/
│   ├── core/
│   │   ├── ShellEngine.kt          # Main shell engine
│   │   ├── CommandParser.kt        # Parse text to commands
│   │   ├── CommandExecutor.kt      # Execute parsed commands
│   │   ├── ContextManager.kt       # Manage session state
│   │   └── HistoryManager.kt       # Command history
│   │
│   ├── models/
│   │   ├── ShellCommand.kt         # Command data class
│   │   ├── ShellAction.kt          # Action data class
│   │   ├── ShellResult.kt          # Result data class
│   │   ├── ShellContext.kt         # Context data class
│   │   └── ActionCapability.kt     # Capability enum
│   │
│   ├── actions/
│   │   ├── ActionRouter.kt         # Route actions to handlers
│   │   ├── ActionHandler.kt        # Base handler interface
│   │   ├── SystemActionHandler.kt  # System actions
│   │   ├── CallActionHandler.kt    # Phone call actions
│   │   ├── MessageActionHandler.kt # SMS actions
│   │   ├── MediaActionHandler.kt   # Media playback
│   │   ├── AppActionHandler.kt     # App launch
│   │   └── QueryActionHandler.kt   # Info queries
│   │
│   ├── interpreter/
│   │   ├── IntentRecognizer.kt     # Recognize user intent
│   │   ├── EntityExtractor.kt      # Extract entities
│   │   ├── AliasResolver.kt        # Resolve aliases
│   │   └── ConfidenceScorer.kt     # Score confidence
│   │
│   ├── script/
│   │   ├── ScriptEngine.kt         # Execute scripts
│   │   ├── ScriptParser.kt         # Parse script files
│   │   └── VirtualFileSystem.kt    # Virtual FS
│   │
│   ├── automation/
│   │   ├── TriggerManager.kt       # Manage triggers
│   │   ├── TriggerExecutor.kt      # Execute on trigger
│   │   └── TriggerTypes.kt         # Trigger definitions
│   │
│   ├── shizuku/
│   │   ├── ShizukuBridge.kt        # Shizuku integration
│   │   ├── ShizukuService.kt       # Shizuku service
│   │   └── PrivilegedActions.kt    # Privileged operations
│   │
│   └── ui/
│       ├── overlay/
│       │   ├── ShellOverlay.kt     # Overlay window
│       │   └── OverlayService.kt   # Overlay service
│       ├── fullscreen/
│       │   ├── ShellScreen.kt      # Fullscreen CLI
│       │   └── ShellViewModel.kt   # CLI ViewModel
│       └── components/
│           ├── CommandInput.kt     # Input field
│           ├── CommandHistory.kt   # History display
│           └── ResultDisplay.kt    # Result display
```

---

## 🎯 Stage 1 Deliverables (This Week)

### Day 1-2: Data Models & Parser
- ✅ ShellCommand, ShellAction, ShellResult models
- 🔨 CommandParser with tokenization
- 🔨 AST generation for commands

### Day 3-4: Executor & Context
- 🔨 CommandExecutor
- 🔨 ContextManager with session state
- 🔨 HistoryManager

### Day 5-6: Action System
- 🔨 ActionRouter
- 🔨 Base ActionHandler interface
- 🔨 SystemActionHandler (open app, settings)

### Day 7: Integration & Testing
- 🔨 Wire everything together
- 🔨 Unit tests
- 🔨 Manual testing

---

## 💡 Example Usage (After Stage 1)

```kotlin
// Initialize shell
val shell = ShellEngine(context)

// Execute commands
shell.execute("open chrome")
shell.execute("launch settings")
shell.execute("show battery")

// Results
ShellResult(
    status = SUCCESS,
    message = "Opened Chrome",
    data = null
)
```

---

## 📝 Success Criteria

### Stage 1 Complete When:
- ✅ Can parse basic commands (open, launch, show)
- ✅ Can execute system actions (open app, settings)
- ✅ Can query device info (battery, storage)
- ✅ Command history works
- ✅ Session context maintained
- ✅ Error handling in place

### Full Shell Complete When:
- ✅ All 9 stages implemented
- ✅ Natural language understanding works
- ✅ Multilingual support active
- ✅ Shizuku integration functional
- ✅ Automation triggers working
- ✅ Plugin system operational
- ✅ Voice interface (optional)

---

## 🚀 Let's Begin!

**Starting with**: Core Shell Engine - Data Models & Parser

Ready to implement? Say "Let's build it!" and I'll start creating the files!

