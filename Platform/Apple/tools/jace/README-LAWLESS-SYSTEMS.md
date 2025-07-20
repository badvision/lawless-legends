# Lawless Legends Systems Documentation

This document explains the custom systems implemented specifically for Lawless Legends gameplay optimization and enhancement.

## Speed Control System

The Lawless Legends emulator implements a sophisticated speed control system that balances performance optimization with gameplay authenticity.

### Speed Control Flow

```mermaid
graph TD
    A[User Sets UI Speed<br/>e.g., 8MHz] --> B[JaceUIController.setSpeed]
    B --> C[Motherboard.setSpeedInPercentage]
    C --> D[TimedDevice.setSpeedInHz]
    
    E[Game Activity Detection] --> F{Activity Type?}
    F -->|Text Rendering| G[LawlessHacks.fastText]
    F -->|Character Animation| H[LawlessHacks.adjustAnimationSpeed]
    F -->|Memory Access| I[CardRamFactor speedBoost]
    
    G --> J[Motherboard.requestSpeed]
    J --> K[TimedDevice.enableTempMaxSpeed]
    K --> L[Run at Max Speed]
    
    H --> M{Animation Context?}
    M -->|Stack=0x09b + PC=0xD5FE| N[LawlessHacks.beginSlowdown]
    M -->|Other| O[Continue Current Speed]
    
    N --> P[Force 1MHz for Animation]
    P --> Q{Key Pressed?}
    Q -->|Yes| R[LawlessHacks.endSlowdown]
    Q -->|No| P
    R --> S[Restore User Speed]
    
    I --> T[Request Temp Speed Boost]
    
    style A fill:#e1f5fe
    style L fill:#ffcdd2
    style P fill:#fff3e0
    style S fill:#e8f5e8
```

### Speed Control Components

#### 1. UI Speed Setting
- **Location**: `JaceUIController.setSpeed()`
- **Purpose**: Sets the baseline speed that the user desires
- **Range**: 0.5x (half speed) to ∞ (max speed)
- **Implementation**: Converts UI slider value to percentage and calls `setSpeedInPercentage()`

#### 2. Text Rendering Speed Boost
- **Location**: `LawlessHacks.fastText()`
- **Trigger**: Execution in memory range `0x0ee00` to `0x0f0c0`
- **Purpose**: Accelerates text rendering for better user experience
- **Mechanism**: Uses temporary speed requests to motherboard
- **Duration**: Active only during text rendering operations

#### 3. Animation Speed Control
- **Location**: `LawlessHacks.adjustAnimationSpeed()`
- **Detection Method**: Heuristic-based using:
  - Program Counter at `0x0D5FE`
  - Memory access at `0x0c000`
  - Stack analysis (specific byte pattern `0x09b`)
- **Purpose**: Slows character animations to 1MHz to prevent comical speed
- **Challenge**: Animation detection is imperfect, may cause false positives during idle

#### 4. Memory Access Speed Boost
- **Location**: `CardRamFactor.speedBoost`
- **Purpose**: Accelerates when accessing RAM expansion cards
- **Configuration**: Optional, disabled by default

### Speed Priority System

```mermaid
graph LR
    A[Max Speed Flag] -->|Highest| B[Final Speed]
    C[Temp Speed Requests] -->|High| B
    D[Animation Slowdown] -->|Medium| B
    E[User UI Setting] -->|Baseline| B
    
    style A fill:#ffcdd2
    style C fill:#ffe0b2
    style D fill:#fff3e0
    style E fill:#e1f5fe
```

1. **Max Speed Flag**: Overrides everything (∞ speed)
2. **Temporary Speed Requests**: Text rendering, memory access boosts
3. **Animation Slowdown**: Forces 1MHz during character animations
4. **User UI Setting**: Baseline speed when no overrides are active

### Timing Drift Protection

The system includes protection against timing drift during max speed periods:

- **Problem**: During max speed, `nextSync` could drift far into the future
- **Solution**: Cap maximum drift to 50ms using `MAX_TIMER_DELAY_MS`
- **Location**: `TimedDevice.calculateResyncDelay()`
- **Effect**: Prevents multi-second pauses when transitioning from max speed to normal speed

## Video Rendering Enhancement System

The Lawless Legends video system enhances text readability through selective black & white rendering.

### Video Enhancement Flow

```mermaid
sequenceDiagram
    participant Game as Game Code
    participant LH as LawlessHacks
    participant LV as LawlessVideo
    participant Screen as Screen Buffer
    
    Game->>LH: Write to video memory (0x2000-0x3fff)
    LH->>LH: Check program counter
    LH->>LH: Determine if text rendering
    
    alt Text Rendering Context
        LH->>LV: Set activeMask[y][x] = false
        Note over LV: B&W mode for text
    else Graphics Rendering
        LH->>LV: Set activeMask[y][x] = true
        Note over LV: Color mode for graphics
    end
    
    LV->>LV: Apply mask during scanline
    LV->>Screen: Render enhanced output
```

### Video Enhancement Components

#### 1. Text Detection
- **Location**: `LawlessHacks.enhanceText()`
- **Trigger**: Memory writes to video RAM (`0x2000-0x3fff`)
- **Detection Logic**:
  ```java
  boolean drawingText = (pc >= 0x0ee00 && pc <= 0x0f0c0 && pc != 0x0f005) || pc > 0x0f100;
  ```

#### 2. Mask Application
- **Location**: `LawlessVideo.activeMask[][]`
- **Structure**: 192 rows × 80 columns boolean array
- **Purpose**: Controls color vs. B&W rendering per pixel pair
- **Default**: All pixels set to color mode (`true`)

#### 3. Rendering Process
- **Location**: `LawlessVideo.hblankStart()`
- **Process**:
  1. Copy row's mask to `colorActive[]`
  2. Call parent rendering with mask applied
  3. Parent renderer uses mask to determine color vs. B&W per pixel

### Text Enhancement Benefits

- **Improved Readability**: Text appears in crisp black & white
- **Preserved Graphics**: Game graphics remain in full color
- **Automatic Detection**: No manual mode switching required
- **Performance**: Minimal overhead during rendering

## Audio System

The Lawless Legends audio system provides dynamic music and sound effects with multiple soundtrack options.

### Audio System Architecture

```mermaid
graph TD
    A[Game Writes to 0xC069] --> B[LawlessHacks.playSound]
    B --> C{Command Type?}
    
    C -->|0-31: Music| D[playMusic]
    C -->|128+: SFX| E[playSfx]
    C -->|252: Stop Music| F[stopMusic]
    C -->|253: Stop SFX| G[stopSfx]
    
    D --> H[Media.getAudioTrack]
    H --> I[Check Current Score]
    I --> J[Load File from Score Map]
    J --> K[MediaPlayer.play]
    
    E --> L[Load SFX File<br/>track + 128]
    L --> M[MediaPlayer.play<br/>Single Play]
    
    K --> N[Fade Management]
    N --> O[Volume Control]
    O --> P[Audio Output]
    
    style A fill:#e1f5fe
    style K fill:#e8f5e8
    style M fill:#fff3e0
    style P fill:#f3e5f5
```

### Audio Trigger System

#### 1. Memory-Mapped Audio Control
- **Trigger Address**: `0x0C069` (SFX_TRIGGER)
- **Detection**: Memory write events to this address
- **Handler**: `LawlessHacks.playSound(int value)`

#### 2. Command Interpretation
```java
if (value <= 31) {
    // Music tracks (0-31)
    playMusic(value, false);
} else if (value >= 128) {
    // Sound effects (128+)
    playSfx(value - 128);
} else if (value == 252) {
    stopMusic();
} else if (value == 253) {
    stopSfx();
}
```

### Soundtrack System

#### File Structure
```
/jace/data/sound/
├── scores.txt              # Soundtrack definitions
├── common/                 # Common soundtrack files
│   ├── 01_title.ogg
│   ├── 02_town.ogg
│   └── ...
├── 8-bit-orchestral-samples/  # Orchestral soundtrack
│   ├── 01_title.ogg
│   ├── 02_town.ogg
│   └── ...
└── 8-bit-chipmusic/          # Chiptune soundtrack
    ├── 01_title.ogg
    ├── 02_town.ogg
    └── ...
```

#### Scores Configuration Format
```
# Soundtrack name (lowercase, used as directory)
8-bit orchestral samples

# Track mapping: number filename
1 01_title.ogg
2 02_town.ogg
17* 17_battle.ogg    # * indicates auto-resume track
...

# Another soundtrack
8-bit chipmusic
1 01_title_chip.ogg
2 02_town_chip.ogg
...
```

### Audio Features

#### 1. Multiple Soundtrack Support
- **Selection**: Runtime switching between soundtracks
- **Mapping**: Each soundtrack maps track numbers to different files
- **Fallback**: Graceful handling of missing files

#### 2. Fade Effects
- **Fade In**: New tracks fade in over 1.5 seconds
- **Fade Out**: Smooth transitions between tracks
- **Volume Control**: Separate music and SFX volume controls

#### 3. Auto-Resume System
- **Marked Tracks**: Tracks with `*` in scores.txt resume from last position
- **Time Tracking**: System remembers playback position
- **Context Awareness**: Battle music and story tracks auto-resume

#### 4. Sound Effects
- **File Mapping**: SFX use same numbering + 128 offset
- **Independent Playback**: SFX don't interfere with music
- **Single Play**: SFX play once and stop

### Audio Implementation Details

#### Media Loading
```java
public Media getAudioTrack(int number) {
    String filename = getSongName(number);  // Lookup in current score
    String pathStr = "/jace/data/sound/" + filename;
    return new Media(pathStr);
}
```

#### Volume Control
- **Music Volume**: Controlled via `setMusicVolume(0.0-1.0)`
- **SFX Volume**: Controlled via speaker volume scaling
- **UI Integration**: Sliders in control overlay

#### Performance Considerations
- **Threaded Playback**: Audio operations run in separate threads
- **Resource Management**: Proper cleanup of MediaPlayer objects
- **Error Handling**: Graceful fallback for missing audio files

## System Integration

All these systems work together to provide an enhanced Lawless Legends experience:

1. **Speed Control** ensures smooth gameplay with appropriate speeds for different contexts
2. **Video Enhancement** improves text readability without affecting graphics
3. **Audio System** provides immersive soundscapes with multiple soundtrack options

The systems are designed to be:
- **Non-intrusive**: Enhance without breaking original gameplay
- **Configurable**: Allow user control over enhancements
- **Robust**: Handle edge cases and errors gracefully
- **Performance-conscious**: Minimize impact on emulation speed 