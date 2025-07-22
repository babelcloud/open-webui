# Android GBox Testing Instructions

## Overview
ChatDroid is the Android client app for Open WebUI. This guide covers testing procedures using GBox Android virtualization.

## Testing Architecture

### Key Components
- **ChatDroid App**: Android client in `/ChatDroid` directory
  - Primary APK: `app/build/outputs/apk/debug/app-debug.apk`
  - Main activities in `app/src/main/java/ai/gbox/chatdroid/`

### Testing Workflow

1. **Box Management**:
   ```
   1. If you don’t have an Android box or your previous one has expired, create a new one and get the boxId. Otherwise, reuse your existing boxId. Set expiredIn to 2h.
   2. ALWAYS open live-view immediately after box creation
   3. Use absolute paths for APK installation
   ```

2. **Installation Process**:
   - Install APK using absolute path:
     ```
     /Users/jack/workspace/open-webui/ChatDroid/app/build/outputs/apk/debug/app-debug.apk
     ```
   - Launch app immediately after installation
   - Allow 2-second delay for app initialization

3. **UI Testing Pattern**:
   ```
   1. Perform UI action (click, type, scroll)
   2. Capture screenshot
   3. Verify expected state
   4. Repeat cycle for test sequence
   ```

### Testing Environment and Account

Open WebUI is hosted at `http://34.121.157.227:3000/`. Use the following credentials for testing:
email: zhenwei@gbox.ai
password: 123456

## Testing Best Practices

### UI Interactions
- When keyboard is visible, field is already focused - input directly
- Use descriptive action instructions (e.g., "tap the email input, scroll up")
- Wait for UI elements to be visible before interaction
- Verify each action with screenshots

## Testing Tools

### GBox Commands
- Create box: `mcp_gbox-android_create_android_box`
- Install APK: `mcp_gbox-android_install_apk`
- UI Actions: `mcp_gbox-android_ui_action`
- Screenshots: `mcp_gbox-android_get_screenshot`
- App Control: `mcp_gbox-android_open_app`/`mcp_gbox-android_close_app`

### Debugging Tips
1. Always verify app state with screenshots
2. Monitor logcat for error messages
3. Check network requests in live view
4. Verify against web UI behavior
