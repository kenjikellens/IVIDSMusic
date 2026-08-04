import os
import socket
import webbrowser
import threading
import time
import sys
from functools import partial
from http.server import SimpleHTTPRequestHandler
from socketserver import TCPServer

class QuietHTTPRequestHandler(SimpleHTTPRequestHandler):
    """
    Custom HTTP request handler that suppresses default console request logs
    to keep terminal output clean during static asset serving.
    """
    def log_message(self, format, *args):
        pass

class LocalWebServer:
    """
    Manages the background local HTTP server lifecycle, dynamic port binding,
    and asset serving without process working directory mutations.
    """
    def __init__(self, directory: str, host: str = 'localhost', port: int = 0):
        self.directory = directory
        self.host = host
        self.requested_port = port
        self.port = port
        self.server = None
        self.thread = None
        self.is_running = False

    def find_free_port(self) -> int:
        """
        Finds an available dynamic TCP port on the host network interface.
        :returns: Integer port number.
        """
        with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
            s.bind((self.host, 0))
            return s.getsockname()[1]

    def start(self):
        """
        Starts the background HTTP server on a daemon thread using partial handler directory binding.
        """
        if self.requested_port == 0:
            self.port = self.find_free_port()

        handler_factory = partial(QuietHTTPRequestHandler, directory=self.directory)
        TCPServer.allow_reuse_address = True
        self.server = TCPServer((self.host, self.port), handler_factory)
        self.is_running = True

        self.thread = threading.Thread(target=self.server.serve_forever, daemon=True)
        self.thread.start()

    def stop(self):
        """
        Stops the server gracefully and closes network sockets.
        """
        if self.server and self.is_running:
            self.server.shutdown()
            self.server.server_close()
            self.is_running = False

    def get_url(self, relative_path: str = "") -> str:
        """
        Constructs the local HTTP URL for the target relative asset path.
        :param relative_path: Relative URL path string.
        :returns: Full HTTP URL string.
        """
        path = relative_path.lstrip('/')
        return f"http://{self.host}:{self.port}/{path}"

class IVIDSPCLauncher:
    """
    Main application orchestrator responsible for validating environments,
    launching background web servers, and opening system web browsers.
    """
    def __init__(self, assets_dir: str = None):
        if assets_dir is None:
            base_dir = os.path.dirname(os.path.abspath(__file__))
            assets_dir = os.path.abspath(os.path.join(base_dir, '..', 'app', 'src', 'main', 'assets'))
        self.assets_dir = assets_dir
        self.web_server = None

    def validate_environment(self) -> bool:
        """
        Verifies that required web asset directories exist before launching.
        :returns: True if environment is valid, False otherwise.
        """
        if not os.path.exists(self.assets_dir):
            print(f"Error: Assets folder not found at '{self.assets_dir}'")
            return False
        return True

    def run(self):
        """
        Executes the PC server startup workflow, opens browser, and monitors process lifecycle.
        """
        if not self.validate_environment():
            sys.exit(1)

        self.web_server = LocalWebServer(directory=self.assets_dir)
        self.web_server.start()

        target_url = self.web_server.get_url("gui/index.html")
        print(f"Server started on http://localhost:{self.web_server.port}/")
        print(f"Opening browser at: {target_url}")
        webbrowser.open(target_url)

        try:
            while self.web_server.is_running:
                time.sleep(0.5)
        except KeyboardInterrupt:
            print("\nShutting down server...")
            self.web_server.stop()

if __name__ == '__main__':
    launcher = IVIDSPCLauncher()
    launcher.run()
