import subprocess
import webbrowser
import time
import os

def main():
    server_script = os.path.join('app', 'src', 'main', 'assets', 'gui', 'temp_server.js')
    
    if not os.path.exists(server_script):
        print(f"Error: Could not find server script at {server_script}")
        print("Make sure you are running this script from the project root.")
        return

    print("Starting local development server (temp_server.js)...")
    
    try:
        # Start the node server in the background
        process = subprocess.Popen(['node', server_script])
        
        # Give the server a moment to start up
        time.sleep(1.5)
        
        # Open the default web browser to the correct GUI entry point
        url = "http://localhost:3000/gui/index.html"
        print(f"Opening browser to {url}...")
        webbrowser.open(url)
        
        # Keep python running so the user can see logs or stop it
        print("\nServer is running! Press Ctrl+C to stop the server.")
        process.wait()
        
    except KeyboardInterrupt:
        print("\nStopping server...")
        if 'process' in locals():
            process.terminate()
            process.wait()
        print("Server stopped.")
    except Exception as e:
        print(f"An error occurred: {e}")

if __name__ == "__main__":
    main()