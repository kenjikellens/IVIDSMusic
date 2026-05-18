import subprocess
import webbrowser
import time
import os
import socket

def is_port_in_use(port):
    """
    Checks if a given TCP port is currently in use on localhost.
    Returns True if the port is occupied, False otherwise.
    """
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
        return s.connect_ex(('localhost', port)) == 0

def kill_process_on_port(port):
    """
    Attempts to find and terminate any process listening on the specified port.
    Returns True if a process was found and successfully killed, False otherwise.
    """
    try:
        # Run netstat to find the process ID on the port
        output = subprocess.check_output(f'netstat -ano | findstr :{port}', shell=True, text=True)
        pids = set()
        for line in output.strip().split('\n'):
            parts = line.split()
            if len(parts) >= 5:
                pid = parts[-1]
                if pid.isdigit() and pid != '0':
                    pids.add(int(pid))
        
        killed_any = False
        for pid in pids:
            print(f"Found existing process with PID {pid} on port {port}. Terminating it...")
            subprocess.run(f'taskkill /F /PID {pid}', shell=True, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
            killed_any = True
        return killed_any
    except subprocess.CalledProcessError:
        # findstr returns exit code 1 if no matches are found, which is normal
        return False
    except Exception as e:
        print(f"Warning: Could not kill process on port {port}: {e}")
        return False

def main():
    """
    Starts the local development server (temp_server.js) in the background,
    verifies it starts successfully, and opens the GUI in the default browser.
    """
    server_script = os.path.join('app', 'src', 'main', 'assets', 'gui', 'temp_server.js')
    port = 3000
    
    if not os.path.exists(server_script):
        print(f"Error: Could not find server script at {server_script}")
        print("Make sure you are running this script from the project root.")
        input("\nPress Enter to exit...")
        return

    # Check if port is in use and auto-terminate the old process
    if is_port_in_use(port):
        print(f"Port {port} is in use. Attempting to clean up the old server instance...")
        if kill_process_on_port(port):
            time.sleep(1.0)  # Give the port a moment to release
        else:
            print(f"Error: Port {port} is in use and could not be freed automatically.")
            input("\nPress Enter to exit...")
            return

    print("Starting local development server (temp_server.js)...")
    
    try:
        # Start the node server in the background
        process = subprocess.Popen(['node', server_script])
        
        # Give the server a moment to start up
        time.sleep(1.5)
        
        # Verify the server is still running
        if process.poll() is not None:
            print(f"\nError: Server process exited unexpectedly with code {process.poll()}.")
            print("Please check if Node.js is installed correctly and has no syntax errors.")
            input("\nPress Enter to exit...")
            return
            
        # Open the default web browser to the correct GUI entry point
        url = f"http://localhost:{port}/gui/index.html"
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
        input("\nPress Enter to exit...")

if __name__ == "__main__":
    main()