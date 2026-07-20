import os
import socket
import webbrowser
import threading
import time
from http.server import SimpleHTTPRequestHandler
from socketserver import TCPServer

class Handler(SimpleHTTPRequestHandler):
    """
    Custom HTTP request handler that suppresses default console logs
    to keep the terminal output clean.
    """
    def log_message(self, format, *args):
        # Suppress logging server requests
        pass

def start_server(port, directory):
    """
    Binds a TCPServer to serve static files from the specified directory.

    :param port: The integer port number to bind the server.
    :param directory: The directory path containing the assets to serve.
    """
    # Change the current working directory of the process to the target assets folder
    # so that SimpleHTTPRequestHandler serves files relative to it.
    os.chdir(directory)
    with TCPServer(('localhost', port), Handler) as httpd:
        httpd.serve_forever()

def main():
    """
    Finds a dynamically available free port, starts the background HTTP server
    serving the assets folder, and opens the index.html page in the default web browser.
    """
    # Find a free port dynamically
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
        s.bind(('localhost', 0))
        port = s.getsockname()[1]

    # Resolve the absolute path of the assets directory
    current_dir = os.path.dirname(os.path.abspath(__file__))
    assets_dir = os.path.join(current_dir, "pc")

    if not os.path.exists(assets_dir):
        print(f"Error: Assets folder not found at '{assets_dir}'")
        return

    # Start the HTTP server on a daemon background thread
    server_thread = threading.Thread(
        target=start_server,
        args=(port, assets_dir),
        daemon=True
    )
    server_thread.start()

    # Build local URL and open it in the default system web browser
    url = f"http://localhost:{port}/gui/index.html"
    print(f"Server started on http://localhost:{port}/")
    print(f"Opening browser at: {url}")
    webbrowser.open(url)

    # Keep the main process running to keep the background server thread alive
    try:
        while True:
            time.sleep(1)
    except KeyboardInterrupt:
        print("\nShutting down server...")

if __name__ == '__main__':
    main()
