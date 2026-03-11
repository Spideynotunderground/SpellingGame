# Quick Emulator Fix - Try These NOW

## Fastest Solutions (2 minutes each):

### 1. Create AVD (Most Common Issue)
- Android Studio → **Tools** → **Device Manager** → **Create Device**
- Choose **Pixel 5** → **Next**
- Download **API 33** (or latest) → **Next** → **Finish**
- Click ▶️ Play button

### 2. Enable Virtualization (Windows)
- Press **Windows + R** → type `optionalfeatures` → Enter
- Check ✅ **Windows Hypervisor Platform**
- Check ✅ **Virtual Machine Platform**
- Restart computer

### 3. Cold Boot Existing AVD
- **Tools** → **Device Manager**
- Right-click your AVD → **Cold Boot Now**

### 4. Use Physical Phone (Fastest Alternative)
- Enable **Developer Options** (Settings → About Phone → Tap Build Number 7x)
- Enable **USB Debugging**
- Connect phone → Select in Android Studio

### 5. Check If Emulator Exists
- Open PowerShell in: `%LOCALAPPDATA%\Android\Sdk\emulator`
- Run: `.\emulator -list-avds`
- If empty, create AVD (Step 1)

**Most likely fix: Step 1 or Step 2**
