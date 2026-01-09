# System Architecture

## Overview

Mentra follows a clean, modular architecture with clear separation of concerns. The system is designed to be offline-first, privacy-focused, and highly extensible.

## High-Level System Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           USER INTERFACE LAYER                              │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐   │
│  │   Launcher   │  │  AI Shell    │  │   Overlay    │  │   Widgets    │   │
│  │     UI       │  │   Clients    │  │   Prompt     │  │              │   │
│  └──────────────┘  └──────────────┘  └──────────────┘  └──────────────┘   │
└─────────────────────────────────────────────────────────────────────────────┘
                                    ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                        PRESENTATION LAYER (UI)                              │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐   │
│  │   Health     │  │  Navigation  │  │    Media     │  │  Messaging   │   │
│  │   Screens    │  │   Screens    │  │   Screens    │  │   Screens    │   │
│  └──────────────┘  └──────────────┘  └──────────────┘  └──────────────┘   │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐   │
│  │   Camera     │  │   Settings   │  │   Plugins    │  │   Utilities  │   │
│  │   Screens    │  │   Screens    │  │   Screens    │  │   Screens    │   │
│  └──────────────┘  └──────────────┘  └──────────────┘  └──────────────┘   │
└─────────────────────────────────────────────────────────────────────────────┘
                                    ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                          AI INTERPRETATION LAYER                            │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                      Offline AI Engine                              │   │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐             │   │
│  │  │   Language   │  │  Intent NLP  │  │  Translation │             │   │
│  │  │  Detection   │  │    Engine    │  │    Engine    │             │   │
│  │  └──────────────┘  └──────────────┘  └──────────────┘             │   │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐             │   │
│  │  │  Entity      │  │   Context    │  │   Confidence │             │   │
│  │  │  Resolver    │  │   Manager    │  │   Scorer     │             │   │
│  │  └──────────────┘  └──────────────┘  └──────────────┘             │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────────┘
                                    ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                         DOMAIN/BUSINESS LOGIC LAYER                         │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐   │
│  │   Activity   │  │  Navigation  │  │    Media     │  │    Shell     │   │
│  │   Engine     │  │   Engine     │  │   Engine     │  │   Engine     │   │
│  └──────────────┘  └──────────────┘  └──────────────┘  └──────────────┘   │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐   │
│  │   Messaging  │  │   Camera     │  │  Automation  │  │   Plugin     │   │
│  │   Engine     │  │   Engine     │  │   Engine     │  │   Manager    │   │
│  └──────────────┘  └──────────────┘  └──────────────┘  └──────────────┘   │
│                                                                             │
│  ┌───────────────────────────────────────────────────────────────────┐     │
│  │                       Action Router                               │     │
│  │  Routes commands to appropriate engines and handlers              │     │
│  └───────────────────────────────────────────────────────────────────┘     │
└─────────────────────────────────────────────────────────────────────────────┘
                                    ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                              DATA LAYER                                     │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐   │
│  │  Repository  │  │  Repository  │  │  Repository  │  │  Repository  │   │
│  │   (Health)   │  │    (Maps)    │  │   (Media)    │  │  (Settings)  │   │
│  └──────────────┘  └──────────────┘  └──────────────┘  └──────────────┘   │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐   │
│  │  Repository  │  │  Repository  │  │  Repository  │  │  Repository  │   │
│  │ (Messaging)  │  │   (Shell)    │  │  (Plugins)   │  │   (User)     │   │
│  └──────────────┘  └──────────────┘  └──────────────┘  └──────────────┘   │
└─────────────────────────────────────────────────────────────────────────────┘
                                    ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                        INFRASTRUCTURE LAYER                                 │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                      Shizuku Bridge Layer                           │   │
│  │              (Elevated Permissions & System Access)                 │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐   │
│  │   Sensor     │  │   Location   │  │   Storage    │  │   Network    │   │
│  │  Abstraction │  │   Services   │  │   Manager    │  │   Manager    │   │
│  └──────────────┘  └──────────────┘  └──────────────┘  └──────────────┘   │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐   │
│  │   Database   │  │   File       │  │   Media      │  │  Permission  │   │
│  │   (Room)     │  │   System     │  │   Scanner    │  │   Manager    │   │
│  └──────────────┘  └──────────────┘  └──────────────┘  └──────────────┘   │
└─────────────────────────────────────────────────────────────────────────────┘
                                    ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                         ANDROID SYSTEM LAYER                                │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐   │
│  │   Sensors    │  │   Location   │  │   Storage    │  │   Network    │   │
│  │   Framework  │  │     APIs     │  │     APIs     │  │     APIs     │   │
│  └──────────────┘  └──────────────┘  └──────────────┘  └──────────────┘   │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐   │
│  │    Media     │  │   Telephony  │  │   Camera     │  │   Package    │   │
│  │   Framework  │  │     APIs     │  │     APIs     │  │   Manager    │   │
│  └──────────────┘  └──────────────┘  └──────────────┘  └──────────────┘   │
└─────────────────────────────────────────────────────────────────────────────┘
```

## Module Architecture

### Health & Activity Tracking Module

```
┌────────────────────────────────────────────────────────────────┐
│                    Health & Activity Module                    │
├────────────────────────────────────────────────────────────────┤
│  UI Layer                                                      │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐         │
│  │  Dashboard   │  │   Activity   │  │    Stats     │         │
│  │   Screen     │  │    Graph     │  │   History    │         │
│  └──────────────┘  └──────────────┘  └──────────────┘         │
├────────────────────────────────────────────────────────────────┤
│  ViewModel Layer                                               │
│  ┌──────────────┐  ┌──────────────┐                           │
│  │  Dashboard   │  │   Activity   │                           │
│  │  ViewModel   │  │  ViewModel   │                           │
│  └──────────────┘  └──────────────┘                           │
├────────────────────────────────────────────────────────────────┤
│  Domain Layer                                                  │
│  ┌──────────────────────────────────────────────────────┐     │
│  │           Activity Computation Engine                │     │
│  │  - Step Counter                                      │     │
│  │  - Distance Calculator (using sensor fusion)         │     │
│  │  - Calorie Estimator                                 │     │
│  │  - Sleep Detector                                    │     │
│  │  - Activity Classifier (walk/run/bike/still)         │     │
│  │  - Heart Rate Monitor (if available)                 │     │
│  └──────────────────────────────────────────────────────┘     │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐         │
│  │   Use Cases  │  │   Use Cases  │  │   Use Cases  │         │
│  │  (Get Stats) │  │ (Track Run)  │  │(Get History) │         │
│  └──────────────┘  └──────────────┘  └──────────────┘         │
├────────────────────────────────────────────────────────────────┤
│  Data Layer                                                    │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐         │
│  │  Repository  │  │  Local DB    │  │   Sensor     │         │
│  │              │  │   (Room)     │  │   Provider   │         │
│  └──────────────┘  └──────────────┘  └──────────────┘         │
└────────────────────────────────────────────────────────────────┘
          ▼                    ▼                    ▼
┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│ Accelerometer│    │  Gyroscope   │    │ Step Counter │
└──────────────┘    └──────────────┘    └──────────────┘
```

### Navigation & Maps Module

```
┌────────────────────────────────────────────────────────────────┐
│                   Navigation & Maps Module                     │
├────────────────────────────────────────────────────────────────┤
│  UI Layer                                                      │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐         │
│  │   Map View   │  │    Route     │  │     POI      │         │
│  │   Screen     │  │    Panel     │  │    Search    │         │
│  └──────────────┘  └──────────────┘  └──────────────┘         │
├────────────────────────────────────────────────────────────────┤
│  ViewModel Layer                                               │
│  ┌──────────────┐  ┌──────────────┐                           │
│  │     Map      │  │  Navigation  │                           │
│  │  ViewModel   │  │  ViewModel   │                           │
│  └──────────────┘  └──────────────┘                           │
├────────────────────────────────────────────────────────────────┤
│  Domain Layer                                                  │
│  ┌──────────────────────────────────────────────────────┐     │
│  │         Navigation & Routing Engine                  │     │
│  │  - GPS Tracker                                       │     │
│  │  - Route Calculator                                  │     │
│  │  - Distance & Bearing Computer                       │     │
│  │  - ETA Estimator (walk/drive/bike/transit)           │     │
│  │  - Turn-by-Turn Generator                            │     │
│  │  - Offline Map Manager                               │     │
│  │  - POI Database                                      │     │
│  └──────────────────────────────────────────────────────┘     │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐         │
│  │   Use Cases  │  │   Use Cases  │  │   Use Cases  │         │
│  │(Get Route)   │  │(Track Nav)   │  │(Search POI)  │         │
│  └──────────────┘  └──────────────┘  └──────────────┘         │
├────────────────────────────────────────────────────────────────┤
│  Data Layer                                                    │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐         │
│  │  Repository  │  │  Map Cache   │  │   Location   │         │
│  │              │  │   Storage    │  │   Provider   │         │
│  └──────────────┘  └──────────────┘  └──────────────┘         │
└────────────────────────────────────────────────────────────────┘
          ▼                    ▼                    ▼
┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│  GPS Sensor  │    │   Network    │    │  Magnetometer│
│              │    │   Location   │    │              │
└──────────────┘    └──────────────┘    └──────────────┘
```

### Media Player Module

```
┌────────────────────────────────────────────────────────────────┐
│                     Media Player Module                        │
├────────────────────────────────────────────────────────────────┤
│  UI Layer                                                      │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐         │
│  │   Player     │  │   Library    │  │   Playlist   │         │
│  │   Screen     │  │   Browser    │  │   Manager    │         │
│  └──────────────┘  └──────────────┘  └──────────────┘         │
├────────────────────────────────────────────────────────────────┤
│  ViewModel Layer                                               │
│  ┌──────────────┐  ┌──────────────┐                           │
│  │   Playback   │  │   Library    │                           │
│  │  ViewModel   │  │  ViewModel   │                           │
│  └──────────────┘  └──────────────┘                           │
├────────────────────────────────────────────────────────────────┤
│  Domain Layer                                                  │
│  ┌──────────────────────────────────────────────────────┐     │
│  │              Media Playback Engine                   │     │
│  │  - Audio Player (ExoPlayer)                          │     │
│  │  - Video Player                                      │     │
│  │  - Playlist Manager                                  │     │
│  │  - Queue Manager                                     │     │
│  │  - Equalizer & Effects                               │     │
│  │  - Streaming Engine                                  │     │
│  │  - Cache Manager                                     │     │
│  │  - Metadata Parser                                   │     │
│  └──────────────────────────────────────────────────────┘     │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐         │
│  │   Use Cases  │  │   Use Cases  │  │   Use Cases  │         │
│  │  (Play Song) │  │(Scan Media)  │  │(Manage Queue)│         │
│  └──────────────┘  └──────────────┘  └──────────────┘         │
├────────────────────────────────────────────────────────────────┤
│  Data Layer                                                    │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐         │
│  │  Repository  │  │  Media DB    │  │    Media     │         │
│  │              │  │   (Room)     │  │   Scanner    │         │
│  └──────────────┘  └──────────────┘  └──────────────┘         │
└────────────────────────────────────────────────────────────────┘
          ▼                    ▼                    ▼
┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│   Storage    │    │  MediaStore  │    │  ExoPlayer   │
│   (Files)    │    │     API      │    │   Library    │
└──────────────┘    └──────────────┘    └──────────────┘
```

### AI Shell Module

```
┌────────────────────────────────────────────────────────────────┐
│              Offline Multilingual AI Shell Module              │
├────────────────────────────────────────────────────────────────┤
│  UI Layer                                                      │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐         │
│  │  CLI Client  │  │   Overlay    │  │    Voice     │         │
│  │  Fullscreen  │  │   Prompt     │  │   Interface  │         │
│  └──────────────┘  └──────────────┘  └──────────────┘         │
├────────────────────────────────────────────────────────────────┤
│  Shell Client Layer                                            │
│  ┌──────────────────────────────────────────────────────┐     │
│  │             Shell Client Interface                   │     │
│  │  - Input Handler (text/voice)                        │     │
│  │  - Output Formatter                                  │     │
│  │  - Session Manager                                   │     │
│  └──────────────────────────────────────────────────────┘     │
├────────────────────────────────────────────────────────────────┤
│  AI Interpretation Layer                                       │
│  ┌──────────────────────────────────────────────────────┐     │
│  │           Natural Language Processor                 │     │
│  │  ┌──────────────┐  ┌──────────────┐                 │     │
│  │  │   Language   │  │ Translation  │                 │     │
│  │  │  Detection   │  │   (Offline)  │                 │     │
│  │  └──────────────┘  └──────────────┘                 │     │
│  │  ┌──────────────┐  ┌──────────────┐                 │     │
│  │  │   Intent     │  │   Entity     │                 │     │
│  │  │ Recognition  │  │  Resolution  │                 │     │
│  │  └──────────────┘  └──────────────┘                 │     │
│  │  ┌──────────────┐  ┌──────────────┐                 │     │
│  │  │  Confidence  │  │   Context    │                 │     │
│  │  │   Scoring    │  │   Manager    │                 │     │
│  │  └──────────────┘  └──────────────┘                 │     │
│  └──────────────────────────────────────────────────────┘     │
├────────────────────────────────────────────────────────────────┤
│  Shell Engine Layer                                            │
│  ┌──────────────────────────────────────────────────────┐     │
│  │               Core Shell Engine                      │     │
│  │  ┌──────────────┐  ┌──────────────┐                 │     │
│  │  │    Lexer     │  │    Parser    │                 │     │
│  │  │  (Tokenize)  │  │  (AST Build) │                 │     │
│  │  └──────────────┘  └──────────────┘                 │     │
│  │  ┌──────────────┐  ┌──────────────┐                 │     │
│  │  │   Executor   │  │  Permission  │                 │     │
│  │  │              │  │   Validator  │                 │     │
│  │  └──────────────┘  └──────────────┘                 │     │
│  │  ┌──────────────┐  ┌──────────────┐                 │     │
│  │  │   History    │  │  Environment │                 │     │
│  │  │   Manager    │  │   Variables  │                 │     │
│  │  └──────────────┘  └──────────────┘                 │     │
│  └──────────────────────────────────────────────────────┘     │
├────────────────────────────────────────────────────────────────┤
│  Action Router Layer                                           │
│  ┌──────────────────────────────────────────────────────┐     │
│  │              Action Router & Dispatcher              │     │
│  │  Routes canonical commands to appropriate handlers   │     │
│  └──────────────────────────────────────────────────────┘     │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐         │
│  │    Call      │  │   Message    │  │    Media     │         │
│  │   Action     │  │   Action     │  │   Action     │         │
│  └──────────────┘  └──────────────┘  └──────────────┘         │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐         │
│  │     App      │  │   System     │  │   Plugin     │         │
│  │   Action     │  │   Action     │  │   Action     │         │
│  └──────────────┘  └──────────────┘  └──────────────┘         │
├────────────────────────────────────────────────────────────────┤
│  Automation Layer                                              │
│  ┌──────────────────────────────────────────────────────┐     │
│  │            Automation & Workflow Engine              │     │
│  │  - Script Executor                                   │     │
│  │  - Trigger Manager (on boot, events, etc.)           │     │
│  │  - Workflow Builder                                  │     │
│  │  - Job Scheduler                                     │     │
│  └──────────────────────────────────────────────────────┘     │
├────────────────────────────────────────────────────────────────┤
│  Data Layer                                                    │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐         │
│  │  Repository  │  │   Virtual    │  │    Alias     │         │
│  │              │  │  Filesystem  │  │   Storage    │         │
│  └──────────────┘  └──────────────┘  └──────────────┘         │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐         │
│  │   Scripts    │  │   Plugins    │  │   History    │         │
│  │   Storage    │  │   Storage    │  │   Storage    │         │
│  └──────────────┘  └──────────────┘  └──────────────┘         │
└────────────────────────────────────────────────────────────────┘
          ▼                    ▼                    ▼
┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│   Shizuku    │    │  TensorFlow  │    │   FastText   │
│    Bridge    │    │     Lite     │    │   (Lang ID)  │
└──────────────┘    └──────────────┘    └──────────────┘
```

## Data Flow Architecture

### Command Processing Flow (AI Shell)

```
User Input (Voice/Text in any language)
         │
         ▼
┌─────────────────────┐
│ Language Detection  │ (FastText)
└──────────┬──────────┘
           ▼
┌─────────────────────┐
│ Translate to English│ (Offline Translation)
└──────────┬──────────┘
           ▼
┌─────────────────────┐
│   Intent NLP        │ (Extract action, entity, params)
│   - Rule-based      │
│   - ML fallback     │
└──────────┬──────────┘
           ▼
┌─────────────────────┐
│ Confidence Check    │
└──────────┬──────────┘
           │
    Low?   ├─Yes─► Request Clarification
           │
        No │
           ▼
┌─────────────────────┐
│  Entity Resolution  │ (Resolve aliases, contacts, etc.)
└──────────┬──────────┘
           ▼
┌─────────────────────┐
│  Shell Parser       │ (Build canonical command)
└──────────┬──────────┘
           ▼
┌─────────────────────┐
│ Permission Check    │
└──────────┬──────────┘
           │
   Denied? ├─Yes─► Request Permission/PIN
           │
   Allowed │
           ▼
┌─────────────────────┐
│  Action Router      │ (Route to handler)
└──────────┬──────────┘
           ▼
┌─────────────────────┐
│  Action Executor    │ (Execute via Shizuku/APIs)
└──────────┬──────────┘
           ▼
┌─────────────────────┐
│  Result Formatting  │
└──────────┬──────────┘
           ▼
┌─────────────────────┐
│ Translate to User   │
│     Language        │
└──────────┬──────────┘
           ▼
┌─────────────────────┐
│ Display/Speak Result│
└─────────────────────┘
```

### Activity Tracking Data Flow

```
Physical Movement
         │
         ▼
┌─────────────────────┐
│ Sensor Framework    │
│ - Accelerometer     │
│ - Gyroscope         │
│ - Step Counter      │
└──────────┬──────────┘
           ▼
┌─────────────────────┐
│  Sensor Manager     │ (Infrastructure)
│  - Fusion           │
│  - Filtering        │
└──────────┬──────────┘
           ▼
┌─────────────────────┐
│ Activity Engine     │ (Domain)
│ - Step Detection    │
│ - Distance Calc     │
│ - Calorie Estimate  │
│ - Activity Classify │
└──────────┬──────────┘
           ▼
┌─────────────────────┐
│   Repository        │
│ - Save to DB        │
│ - Cache stats       │
└──────────┬──────────┘
           ▼
┌─────────────────────┐
│   Room Database     │
└──────────┬──────────┘
           ▼
┌─────────────────────┐
│   ViewModel         │ (Update UI state)
└──────────┬──────────┘
           ▼
┌─────────────────────┐
│   UI Composables    │ (Display graphs, stats)
└─────────────────────┘
```

### Navigation & Routing Data Flow

```
User Request: "Navigate to Machakos"
         │
         ▼
┌─────────────────────┐
│  AI Shell / UI      │
└──────────┬──────────┘
           ▼
┌─────────────────────┐
│ Navigation Engine   │
│ - Get current GPS   │
└──────────┬──────────┘
           ▼
┌─────────────────────┐
│ Location Provider   │ (Get current position)
└──────────┬──────────┘
           ▼
┌─────────────────────┐
│ Geocoding / Search  │ (Find destination coords)
└──────────┬──────────┘
           ▼
┌─────────────────────┐
│ Route Calculator    │
│ - Compute path      │
│ - Calculate distance│
│ - Estimate ETA      │
│   (walk/drive/bike) │
└──────────┬──────────┘
           ▼
┌─────────────────────┐
│ Repository          │ (Save route, cache)
└──────────┬──────────┘
           ▼
┌─────────────────────┐
│ ViewModel           │ (Update UI state)
└──────────┬──────────┘
           ▼
┌─────────────────────┐
│ Map UI              │
│ - Render map        │
│ - Draw route        │
│ - Show markers      │
│ - Display ETA       │
└──────────┬──────────┘
           │
    During Navigation
           ▼
┌─────────────────────┐
│ GPS Tracking        │ (Continuous updates)
│ - Update position   │
│ - Recalculate ETA   │
│ - Turn-by-turn      │
└─────────────────────┘
```

## Cross-Module Communication

### Event Bus Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     Global Event Bus                        │
│                   (Kotlin Flow / SharedFlow)                │
└─────────────────────────────────────────────────────────────┘
      ▲                  ▲                  ▲                  
      │                  │                  │                  
   Publish            Publish            Publish             
      │                  │                  │                  
┌───────────┐      ┌───────────┐      ┌───────────┐         
│  Health   │      │   Media   │      │Navigation │         
│  Module   │      │  Module   │      │  Module   │         
└───────────┘      └───────────┘      └───────────┘         
      │                  │                  │                  
   Subscribe          Subscribe          Subscribe           
      │                  │                  │                  
      ▼                  ▼                  ▼                  
┌─────────────────────────────────────────────────────────────┐
│                     Global Event Bus                        │
└─────────────────────────────────────────────────────────────┘
```

### Event Types

```kotlin
sealed class SystemEvent {
    // Activity Events
    data class ActivityDetected(val type: ActivityType, val confidence: Float)
    data class StepCountUpdated(val steps: Int, val timestamp: Long)
    
    // Navigation Events
    data class LocationUpdated(val location: Location)
    data class NavigationStarted(val destination: String)
    data class NavigationCompleted()
    
    // Media Events
    data class MediaStarted(val mediaId: String)
    data class MediaStopped()
    data class PlaylistChanged(val playlistId: String)
    
    // Shell Events
    data class CommandExecuted(val command: String, val result: String)
    data class AutomationTriggered(val trigger: String)
    
    // System Events
    data class BatteryLow(val level: Int)
    data class ConnectivityChanged(val isOnline: Boolean)
    data class HeadphonesPlugged(val plugged: Boolean)
}
```

## Storage Architecture

### Database Schema (Room)

```
┌─────────────────────────────────────────────────────────────┐
│                      Room Database                          │
├─────────────────────────────────────────────────────────────┤
│  ┌────────────────┐  ┌────────────────┐  ┌──────────────┐  │
│  │ ActivityRecord │  │  HealthStats   │  │  SleepData   │  │
│  ├────────────────┤  ├────────────────┤  ├──────────────┤  │
│  │ id             │  │ id             │  │ id           │  │
│  │ timestamp      │  │ date           │  │ date         │  │
│  │ activityType   │  │ totalSteps     │  │ sleepStart   │  │
│  │ steps          │  │ totalDistance  │  │ sleepEnd     │  │
│  │ distance       │  │ calories       │  │ quality      │  │
│  │ calories       │  │ activeMinutes  │  │ interruption │  │
│  │ duration       │  │                │  │              │  │
│  └────────────────┘  └────────────────┘  └──────────────┘  │
│                                                             │
│  ┌────────────────┐  ┌────────────────┐  ┌──────────────┐  │
│  │  SavedRoute    │  │  RoutePoint    │  │  POI         │  │
│  ├────────────────┤  ├────────────────┤  ├──────────────┤  │
│  │ id             │  │ id             │  │ id           │  │
│  │ name           │  │ routeId (FK)   │  │ name         │  │
│  │ startLocation  │  │ latitude       │  │ latitude     │  │
│  │ endLocation    │  │ longitude      │  │ longitude    │  │
│  │ distance       │  │ sequence       │  │ category     │  │
│  │ createdAt      │  │                │  │ description  │  │
│  └────────────────┘  └────────────────┘  └──────────────┘  │
│                                                             │
│  ┌────────────────┐  ┌────────────────┐  ┌──────────────┐  │
│  │  MediaItem     │  │  Playlist      │  │PlaylistItem  │  │
│  ├────────────────┤  ├────────────────┤  ├──────────────┤  │
│  │ id             │  │ id             │  │ id           │  │
│  │ title          │  │ name           │  │ playlistId   │  │
│  │ artist         │  │ createdAt      │  │ mediaItemId  │  │
│  │ album          │  │ modifiedAt     │  │ sequence     │  │
│  │ filePath       │  │                │  │              │  │
│  │ duration       │  │                │  │              │  │
│  │ mimeType       │  │                │  │              │  │
│  └────────────────┘  └────────────────┘  └──────────────┘  │
│                                                             │
│  ┌────────────────┐  ┌────────────────┐  ┌──────────────┐  │
│  │ ShellHistory   │  │  ShellAlias    │  │  ShellScript │  │
│  ├────────────────┤  ├────────────────┤  ├──────────────┤  │
│  │ id             │  │ id             │  │ id           │  │
│  │ command        │  │ alias          │  │ name         │  │
│  │ timestamp      │  │ target         │  │ content      │  │
│  │ success        │  │ type           │  │ createdAt    │  │
│  │ result         │  │                │  │ modifiedAt   │  │
│  └────────────────┘  └────────────────┘  └──────────────┘  │
│                                                             │
│  ┌────────────────┐  ┌────────────────┐                    │
│  │  UserProfile   │  │  Preferences   │                    │
│  ├────────────────┤  ├────────────────┤                    │
│  │ id             │  │ key            │                    │
│  │ name           │  │ value          │                    │
│  │ heightCm       │  │ type           │                    │
│  │ weightKg       │  │                │                    │
│  │ birthDate      │  │                │                    │
│  │ gender         │  │                │                    │
│  └────────────────┘  └────────────────┘                    │
└─────────────────────────────────────────────────────────────┘
```

### File System Structure

```
/data/data/com.example.mentra/
├── databases/
│   └── mentra.db                    # Room database
├── files/
│   ├── shell/
│   │   ├── config/                  # Shell configuration
│   │   ├── scripts/                 # User scripts
│   │   ├── plugins/                 # Installed plugins
│   │   ├── history/                 # Command history
│   │   └── env/                     # Environment variables
│   ├── maps/
│   │   ├── tiles/                   # Offline map tiles
│   │   └── cache/                   # Map cache
│   ├── models/
│   │   ├── nlp/                     # NLP models
│   │   ├── translation/             # Translation models
│   │   └── langdetect/              # Language detection
│   └── cache/
│       ├── media/                   # Media thumbnails
│       └── routes/                  # Cached routes
└── shared_prefs/
    └── settings.xml                 # App preferences

/sdcard/Mentra/                      # External storage
├── Media/
│   ├── Music/
│   ├── Videos/
│   └── Playlists/
├── Maps/
│   └── Offline/
├── Health/
│   └── Exports/
└── Backups/
```

## Security Architecture

### Permission Layers

```
┌─────────────────────────────────────────────────────────────┐
│                     User Request                            │
└──────────────────────┬──────────────────────────────────────┘
                       ▼
┌─────────────────────────────────────────────────────────────┐
│              Permission Validator                           │
│  - Check if action requires permission                      │
│  - Map action to capability                                 │
└──────────────────────┬──────────────────────────────────────┘
                       ▼
┌─────────────────────────────────────────────────────────────┐
│           Permission Decision Engine                        │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  Standard Android Permissions                       │   │
│  │  (Already granted via manifest/runtime)             │   │
│  └─────────────────────────────────────────────────────┘   │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  Sensitive Actions                                  │   │
│  │  (Require user confirmation or PIN)                 │   │
│  │  - Call, SMS, System changes                        │   │
│  └─────────────────────────────────────────────────────┘   │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  Shizuku Permissions                                │   │
│  │  (Elevated system access)                           │   │
│  │  - Package install, System settings                 │   │
│  └─────────────────────────────────────────────────────┘   │
└──────────────────────┬──────────────────────────────────────┘
                       ▼
         ┌─────────────┴─────────────┐
         │                           │
    Granted?                    Denied?
         │                           │
         ▼                           ▼
┌─────────────────┐         ┌─────────────────┐
│ Execute Action  │         │ Request User    │
│                 │         │ Approval/PIN    │
└─────────────────┘         └─────────────────┘
```

### Shizuku Integration

```
┌─────────────────────────────────────────────────────────────┐
│                    Mentra Application                       │
├─────────────────────────────────────────────────────────────┤
│  ┌────────────────────────────────────────────────────┐    │
│  │           Shizuku Bridge Layer                     │    │
│  │  - Permission checker                              │    │
│  │  - Binder interface                                │    │
│  │  - Action executor                                 │    │
│  └──────────────────────┬─────────────────────────────┘    │
└─────────────────────────┼──────────────────────────────────┘
                          │ (IPC)
                          ▼
┌─────────────────────────────────────────────────────────────┐
│                   Shizuku Service                           │
│  (Running with ADB permissions)                             │
├─────────────────────────────────────────────────────────────┤
│  - Package Manager                                          │
│  - System Settings                                          │
│  - Activity Manager                                         │
│  - Other system services                                    │
└──────────────────────┬──────────────────────────────────────┘
                       ▼
┌─────────────────────────────────────────────────────────────┐
│                Android System Framework                     │
└─────────────────────────────────────────────────────────────┘
```

## Performance Considerations

### Background Service Strategy

```
┌─────────────────────────────────────────────────────────────┐
│                  Service Architecture                       │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  Foreground Service (Always Running)                        │
│  ┌────────────────────────────────────────────────────┐    │
│  │  Activity Tracking Service                         │    │
│  │  - Step counter monitoring                         │    │
│  │  - Low battery impact (<5%)                        │    │
│  │  - Persistent notification                         │    │
│  └────────────────────────────────────────────────────┘    │
│                                                             │
│  Conditional Foreground (When Active)                       │
│  ┌────────────────────────────────────────────────────┐    │
│  │  Navigation Service                                │    │
│  │  - GPS tracking during navigation                  │    │
│  │  - Turn-by-turn updates                            │    │
│  └────────────────────────────────────────────────────┘    │
│  ┌────────────────────────────────────────────────────┐    │
│  │  Media Playback Service                            │    │
│  │  - Audio/video playback                            │    │
│  │  - Media session control                           │    │
│  └────────────────────────────────────────────────────┘    │
│                                                             │
│  Background Work (WorkManager)                              │
│  ┌────────────────────────────────────────────────────┐    │
│  │  Media Scanner Worker                              │    │
│  │  - Periodic media indexing                         │    │
│  │  - Battery-aware scheduling                        │    │
│  └────────────────────────────────────────────────────┘    │
│  ┌────────────────────────────────────────────────────┐    │
│  │  Backup Worker                                     │    │
│  │  - Scheduled backups                               │    │
│  │  - Network-aware                                   │    │
│  └────────────────────────────────────────────────────┘    │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## Next Steps

1. Review feature specifications in `02-FEATURE-SPECIFICATIONS.md`
2. Follow implementation guide in `03-IMPLEMENTATION-GUIDE.md`
3. Set up development environment using `04-DEVELOPMENT-SETUP.md`

