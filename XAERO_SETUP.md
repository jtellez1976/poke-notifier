# Xaero's Integration Setup Guide

## 🚀 Automatic Integration

Poke Notifier integrates seamlessly with Xaero's mods using **runtime detection** - no setup required!

### ✅ How It Works
1. Install Poke Notifier
2. Install Xaero's Minimap and/or Worldmap (any version)
3. Launch the game - waypoint buttons appear automatically!

### 🎯 What You Get
- **Clickable waypoint buttons** in chat instead of plain coordinates
- **Automatic fallback** to coordinates if Xaero's isn't installed
- **No configuration needed** - works out of the box
- **Compatible with all versions** of Xaero's mods

## 🔧 Technical Implementation

Our integration uses reflection-based runtime detection:

```java
// Runtime detection - no compile dependencies needed
private static boolean isXaerosAvailable() {
    try {
        Class.forName("xaero.common.XaeroMinimapSession");
        return true;
    } catch (ClassNotFoundException e) {
        return false;
    }
}
```

### Key Features:
- **Zero dependencies** - no JARs needed in libs/
- **Graceful degradation** - works with or without Xaero's
- **Version agnostic** - compatible across Xaero's updates

## 🎯 Testing the Integration

### With Xaero's Installed
1. Spawn a rare Pokemon: `/pnc gui` → Admin Tools → Testing → Spawn Pokemon
2. Check chat for `[Add Waypoint]` button instead of coordinates
3. Click the button - waypoint appears instantly on Xaero's map
4. Waypoint uses rarity-based colors (red for legendary, etc.)

### Without Xaero's Installed
1. Same spawn test as above
2. Should see normal coordinates: `X: 100, Y: 64, Z: 200`
3. No errors or crashes should occur

## 🐛 Troubleshooting

### No Waypoint Button Appears
- **Check Xaero's installation**: Ensure both Minimap and Worldmap are loaded
- **Verify mod loading**: Look for "Xaero's mods detected" in logs
- **Test with known working version**: Try Xaero's Minimap 25.2.x + Worldmap 1.39.x

### Button Doesn't Work
- **Check Xaero's settings**: Ensure waypoint creation is enabled
- **Verify permissions**: Some servers restrict waypoint commands
- **Check distance**: You must be within notification range of the Pokemon

### Debug Information
Enable debug mode in Poke Notifier to see integration status:
```
[Poke Notifier] Xaero's mods detected - waypoint integration enabled
[Poke Notifier] Created waypoint button for Charizard at 100,64,200
```

## 📋 Compatibility Matrix

| Minecraft | Fabric | Xaero's Minimap | Xaero's Worldmap | Status |
|-----------|--------|-----------------|------------------|---------|
| 1.21.1    | 0.16.x | 25.2.x         | 1.39.x          | ✅ Tested |
| 1.21.1    | 0.15.x | 25.1.x         | 1.38.x          | ⚠️ Should work |
| 1.21.0    | Any    | Any            | Any             | ❌ Not supported |

## 🔍 Debug Information

Enable debug logging in your launcher to see integration status:
```
[Poke Notifier] Xaero's mods detected (Minimap: true, Worldmap: true) - waypoint integration enabled
[Poke Notifier] Created Xaero waypoint button for Charizard at 100,64,200
```

If you see coordinate fallback messages, Xaero's integration is not active.