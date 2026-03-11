# Android Emulator Troubleshooting Guide

## Common Issues and Solutions

### 1. **No AVD (Android Virtual Device) Created**
   - **Solution**: Create a new AVD
   - Open Android Studio → Tools → Device Manager
   - Click "Create Device"
   - Select a device (e.g., Pixel 5)
   - Download a system image (API 24 or higher for your app)
   - Finish the setup

### 2. **Hardware Acceleration Not Enabled**
   - **Windows**: Enable Hyper-V or HAXM
   - **Check**: Open Android Studio → Tools → SDK Manager → SDK Tools
   - Ensure "Android Emulator" and "Intel x86 Emulator Accelerator (HAXM)" are installed
   - **For Windows 10/11**: 
     - Enable Windows Hypervisor Platform (WHPX) in Windows Features
     - Or install Intel HAXM if using Intel processor

### 3. **BIOS Virtualization Disabled**
   - Restart computer and enter BIOS/UEFI settings
   - Enable "Virtualization Technology" (VT-x for Intel, AMD-V for AMD)
   - Save and restart

### 4. **Insufficient System Resources**
   - Close other applications
   - Allocate more RAM to emulator in AVD settings
   - Use a lower resolution device profile

### 5. **Emulator Not Starting**
   - Check Android Studio logs: View → Tool Windows → Logcat
   - Try cold boot: Device Manager → Right-click AVD → Cold Boot Now
   - Delete and recreate the AVD

### 6. **Windows-Specific Issues**
   - Run Android Studio as Administrator
   - Check Windows Defender/antivirus isn't blocking emulator
   - Ensure Windows Hypervisor Platform is enabled:
     - Settings → Apps → Optional Features → More Windows Features
     - Check "Windows Hypervisor Platform" and "Virtual Machine Platform"

## Quick Fix Steps (Try in Order)

1. **Verify Emulator Installation**:
   ```
   Android Studio → Tools → SDK Manager → SDK Tools
   ✓ Android Emulator (check if installed)
   ```

2. **Create/Verify AVD**:
   ```
   Tools → Device Manager → Create Device
   ```

3. **Check System Requirements**:
   - Minimum 8GB RAM (16GB recommended)
   - 2GB free disk space for AVD
   - 64-bit Windows

4. **Enable Virtualization**:
   - Check Task Manager → Performance → CPU → "Virtualization: Enabled"
   - If disabled, enable in BIOS

5. **Try Command Line**:
   ```powershell
   cd "C:\Users\YourUsername\AppData\Local\Android\Sdk\emulator"
   .\emulator -list-avds
   .\emulator -avd YourAVDName
   ```

6. **Check Logs**:
   - Android Studio → Help → Show Log in Explorer
   - Look for emulator-related errors

## Alternative: Use Physical Device

If emulator continues to fail, use a physical Android device:
1. Enable Developer Options on phone
2. Enable USB Debugging
3. Connect via USB
4. Select device in Android Studio

## Still Having Issues?

Check these files for errors:
- `%LOCALAPPDATA%\Android\Sdk\emulator\qemu\system-qemu-images\`
- Android Studio logs: `Help → Show Log in Explorer`
