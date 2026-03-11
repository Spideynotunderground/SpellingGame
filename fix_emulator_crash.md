# Quick Fix: Emulator Process Terminated

## Fast Solutions (try in order):

### 1. Cold Boot (30 seconds)
- **Tools** → **Device Manager**
- Right-click **Medium_Phone_API_36.1** → **Cold Boot Now**

### 2. Wipe Data (1 minute)
- **Tools** → **Device Manager**
- Right-click AVD → **Wipe Data**
- Click **▶️ Play** button

### 3. Increase RAM (if crash continues)
- **Device Manager** → Click **Edit** (pencil icon) on your AVD
- **Show Advanced Settings**
- **Memory and Storage** → **RAM**: Change to **2048 MB** (if currently less)
- **Internal Storage**: Ensure at least **1024 MB**
- Click **Finish**
- Cold boot again

### 4. Recreate AVD (last resort - 2 minutes)
- **Device Manager** → Delete **Medium_Phone_API_36.1**
- **Create Device** → Choose **Pixel 5**
- **System Image**: Download **API 33** or **API 34** (lighter than 36)
- **Finish** → Click **▶️ Play**

## Most Common Fix: Solution #2 (Wipe Data)
