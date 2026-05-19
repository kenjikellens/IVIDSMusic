import os
import subprocess
import sys

def check_node_installed():
    """
    Checks if Node.js (node) is installed and available in the system PATH.
    Returns True if installed, False otherwise.
    """
    try:
        subprocess.run(['node', '--version'], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, check=True)
        return True
    except (subprocess.CalledProcessError, FileNotFoundError):
        return False

def main():
    """
    Main execution method for the PC/Electron startup wrapper.
    Ensures Node.js is present, installs package.json dependencies, and launches the Electron application.
    """
    # Change current working directory to the directory of this script to ensure relative paths resolve correctly
    script_dir = os.path.dirname(os.path.abspath(__file__))
    os.chdir(script_dir)

    print("=== IVIDS Music PC Version (Electron) Launcher ===")

    # Verify Node.js is installed
    if not check_node_installed():
        print("\n[Error] Node.js is not installed or not in your system PATH.")
        print("Please download and install Node.js from https://nodejs.org/")
        input("\nPress Enter to exit...")
        sys.exit(1)

    # Check and install npm packages if node_modules is missing
    node_modules_dir = os.path.join(script_dir, 'node_modules')
    if not os.path.exists(node_modules_dir):
        print("\nnode_modules not found. Installing Electron dependencies via npm...")
        try:
            subprocess.run(['cmd', '/c', 'npm install'], check=True)
            print("\x1b[32m✔ Dependencies successfully installed.\x1b[0m")
        except subprocess.CalledProcessError as e:
            print(f"\n[Error] npm install failed: {e}")
            input("\nPress Enter to exit...")
            sys.exit(1)

    # Run the Electron desktop shell
    print("\nLaunching IVIDS Music Desktop Application...")
    try:
        subprocess.run(['cmd', '/c', 'npm start'], check=True)
        print("\nDesktop application closed.")
    except subprocess.CalledProcessError as e:
        print(f"\n[Error] Failed to start Electron: {e}")
        input("\nPress Enter to exit...")
        sys.exit(1)

if __name__ == "__main__":
    main()
