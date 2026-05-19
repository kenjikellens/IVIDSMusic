import os
import subprocess
import sys

def check_adb_devices():
    """
    Checks if there are any active, authorized Android devices or emulators connected.
    Returns True if at least one device is online, False otherwise.
    """
    try:
        # Run adb devices command
        output = subprocess.check_output(['adb', 'devices'], text=True)
        lines = output.strip().split('\n')[1:] # Skip header
        active_devices = 0
        for line in lines:
            if line.strip() and not line.startswith('*') and 'device' in line:
                active_devices += 1
        return active_devices > 0
    except (subprocess.CalledProcessError, FileNotFoundError):
        # adb might not be in PATH
        return None

def main():
    """
    Main execution method for the Phone/Mobile flavor compilation and installation.
    Verifies adb device availability and executes the gradlew.bat installMobileDebug target task.
    """
    # Change current working directory to the directory of this script to ensure relative paths resolve correctly
    script_dir = os.path.dirname(os.path.abspath(__file__))
    os.chdir(script_dir)

    print("=== IVIDS Music Phone Version (Native Java) Launcher ===")

    # Verify device connection
    devices_status = check_adb_devices()
    if devices_status is None:
        print("\n[Warning] 'adb' command-line utility was not found in your system PATH.")
        print("Gradle will still attempt to build, but won't be able to install automatically without adb.")
    elif not devices_status:
        print("\n[Warning] No connected Android devices or emulators were detected.")
        print("Please connect an Android phone with USB debugging enabled, or start an emulator.")
        choice = input("Would you like to build the APK anyway? (y/n): ").strip().lower()
        if choice != 'y':
            sys.exit(0)

    gradlew_path = os.path.join(script_dir, 'gradlew.bat')
    if not os.path.exists(gradlew_path):
        print(f"\n[Error] Could not find Gradle wrapper at {gradlew_path}")
        input("\nPress Enter to exit...")
        sys.exit(1)

    # Determine gradle task based on device presence
    if devices_status:
        task = 'installMobileDebug'
        action_name = "Building and installing mobile build variant (:app:installMobileDebug)..."
        success_msg = "\n\x1b[32m✔ Native Phone App successfully installed on your device!\x1b[0m"
    else:
        task = 'assembleMobileDebug'
        action_name = "Building Native Phone APK (:app:assembleMobileDebug)..."
        success_msg = "\n\x1b[32m✔ Native Phone APK successfully compiled!\nOutput location: app/build/outputs/apk/mobile/debug/app-mobile-debug.apk\x1b[0m"

    print(f"\n{action_name}")
    try:
        # Run gradle task
        subprocess.run([gradlew_path, task], check=True)
        print(success_msg)
        input("\nPress Enter to close...")
    except subprocess.CalledProcessError as e:
        print(f"\n[Error] Gradle build failed: {e}")
        input("\nPress Enter to exit...")
        sys.exit(1)

if __name__ == "__main__":
    main()
