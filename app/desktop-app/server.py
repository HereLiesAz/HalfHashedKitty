import asyncio
import json
import logging
import websockets
import shortuuid
import threading
import time

try:
    import paramiko
    logging.info("Paramiko library loaded successfully.")
except ImportError:
    logging.error("Paramiko library not found. Sniffing functionality will be disabled. Please install it: pip install paramiko")
    paramiko = None

logging.basicConfig(level=logging.INFO)

class Hub:
    def __init__(self):
        self.rooms = {}
        self.mutex = asyncio.Lock()

    async def register(self, client):
        async with self.mutex:
            if client.room not in self.rooms:
                self.rooms[client.room] = set()
            self.rooms[client.room].add(client)
            logging.info(f"Client registered to room {client.room}. Total clients: {len(self.rooms[client.room])}")

    async def unregister(self, client):
        async with self.mutex:
            if client.room in self.rooms:
                self.rooms[client.room].remove(client)
                if not self.rooms[client.room]:
                    del self.rooms[client.room]
                logging.info(f"Client unregistered from room {client.room}.")

    async def broadcast(self, message, sender):
        async with self.mutex:
            if sender.room in self.rooms:
                # The message is already a JSON string, no need to re-encode
                payload = message
                receivers = [client for client in self.rooms[sender.room] if client != sender]
                if receivers:
                    await asyncio.wait([client.conn.send(payload) for client in receivers])

class Client:
    def __init__(self, conn, hub):
        self.conn = conn
        self.hub = hub
        self.room = None

class Sniffer:
    def __init__(self, websocket, loop):
        self.websocket = websocket
        self.loop = loop
        self.ssh = None
        self.channel = None
        self._stop_event = threading.Event()

    def start(self, host, port, username, password):
        try:
            if not paramiko:
                self._send_output("Error: Paramiko library not installed on the server.")
                return

            self.ssh = paramiko.SSHClient()
            self.ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
            logging.info(f"Connecting to {username}@{host}:{port} for sniffing...")
            self.ssh.connect(hostname=host, port=port, username=username, password=password, timeout=10)
            logging.info("SSH connection established for sniffing.")

            # Command to capture packets. -l and -U are for unbuffered output.
            # 'not port 22' to avoid capturing our own SSH traffic.
            command = "sudo tcpdump -l -U -i any 'not port 22'"
            stdin, stdout, stderr = self.ssh.exec_command(command, get_pty=True)
            self.channel = stdout.channel

            # Handle sudo password prompt if necessary
            # This is a bit tricky. A better solution would use key-based auth with passwordless sudo.
            # For now, we'll just check stderr.
            err = stderr.read().decode('utf-8', errors='ignore')
            if err:
                logging.error(f"Error starting sniff on remote: {err}")
                self._send_output(f"Error starting sniff: {err}")
                # Don't return here, as some systems send initial messages to stderr

            while not self._stop_event.is_set() and not self.channel.exit_status_ready():
                if self.channel.recv_ready():
                    output = self.channel.recv(1024).decode('utf-8', errors='ignore')
                    self._send_output(output)
                else:
                    time.sleep(0.1) # Avoid busy-waiting

        except Exception as e:
            error_message = f"Sniffing failed: {str(e)}"
            logging.error(error_message)
            self._send_output(error_message)
        finally:
            logging.info("Sniffing process ended.")
            self._send_stopped()
            self.stop() # Ensure resources are cleaned up

    def stop(self):
        if self._stop_event.is_set():
            return # Already stopping/stopped
        logging.info("Stopping sniffer...")
        self._stop_event.set()
        if self.ssh:
            try:
                # Kill the remote tcpdump process. pkill is convenient.
                self.ssh.exec_command("sudo pkill -f 'tcpdump -l -U -i any'")
                logging.info("Sent pkill to remote tcpdump.")
            except Exception as e:
                logging.warning(f"Could not send pkill command: {e}")
            finally:
                self.ssh.close()
                self.ssh = None
                logging.info("SSH connection for sniffer closed.")

    def _send_output(self, output):
        # To be consistent with other messages, the payload should be a JSON string.
        payload_dict = {"output": output}
        payload_json = json.dumps(payload_dict)
        message = json.dumps({"type": "sniff_output", "payload": payload_json})
        future = asyncio.run_coroutine_threadsafe(self.websocket.send(message), self.loop)
        try:
            future.result(timeout=1)
        except Exception as e:
            logging.warning(f"Failed to send sniff output to client: {e}")
            self.stop() # Stop sniffing if we can't communicate with the client

    def _send_stopped(self):
        message = json.dumps({"type": "sniff_stopped"})
        future = asyncio.run_coroutine_threadsafe(self.websocket.send(message), self.loop)
        try:
            future.result(timeout=1)
        except Exception as e:
            logging.warning(f"Failed to send sniff_stopped to client: {e}")

class WebSocketServer:
    def __init__(self, host="0.0.0.0", port=5001):
        self.host = host
        self.port = port
        self.hub = Hub()
        self.attack_callback = None
        self.sniffers = {} # Maps client object to Sniffer instance

    def set_attack_callback(self, callback):
        """Sets a callback function to be called when an attack is requested."""
        self.attack_callback = callback

    async def handler(self, websocket, path):
        client = Client(websocket, self.hub)
        loop = asyncio.get_running_loop()
        try:
            async for raw_message in websocket:
                try:
                    msg = json.loads(raw_message)
                    msg_type = msg.get("type")
                    room_id = msg.get("room_id")

                    if msg_type == "join" and room_id:
                        client.room = room_id
                        await self.hub.register(client)
                    elif msg_type == "attack":
                        logging.info(f"Received attack command: {msg}")
                        if self.attack_callback:
                            attack_params = json.loads(msg.get("payload", "{}"))
                            self.attack_callback(attack_params, client)

                    elif msg_type == "start_sniff":
                        if not paramiko:
                            logging.error("Cannot start sniff, paramiko is not installed.")
                            await websocket.send(json.dumps({"type": "sniff_output", "payload": "Error: SSH library (paramiko) not installed on server."}))
                            continue

                        logging.info(f"Received start_sniff command.")
                        payload = json.loads(msg.get("payload", "{}"))

                        # Stop any existing sniffer for this client first
                        if client in self.sniffers:
                            logging.info("Stopping previous sniffer for client.")
                            self.sniffers[client].stop()

                        sniffer = Sniffer(websocket, loop)
                        self.sniffers[client] = sniffer

                        thread = threading.Thread(target=sniffer.start, args=(
                            payload.get("host"),
                            payload.get("port", 22),
                            payload.get("username"),
                            payload.get("password")
                        ))
                        thread.daemon = True
                        thread.start()

                    elif msg_type == "stop_sniff":
                        logging.info("Received stop_sniff command.")
                        sniffer = self.sniffers.pop(client, None)
                        if sniffer:
                            sniffer.stop()

                    # Broadcast the original raw message to other clients in the room
                    await self.hub.broadcast(raw_message, client)

                except json.JSONDecodeError:
                    logging.error(f"Failed to decode JSON message: {raw_message}")
                except Exception as e:
                    logging.error(f"Error processing message: {e}", exc_info=True)
        finally:
            # Clean up sniffer if client disconnects
            sniffer = self.sniffers.pop(client, None)
            if sniffer:
                logging.info(f"Client disconnected, stopping sniffer.")
                sniffer.stop()
            await self.hub.unregister(client)

    async def start(self):
        logging.info(f"[*] WebSocket Relay Server starting on ws://{self.host}:{self.port}")
        async with websockets.serve(self.handler, self.host, self.port):
            await asyncio.Future()  # Run forever

def generate_room_id():
    """Generates a new unique room ID."""
    return shortuuid.uuid()

# Example usage
if __name__ == "__main__":
    def dummy_attack_handler(params, client):
        print(f"Dummy attack handler received params: {params} from client {client.conn.remote_address}")
        # In a real scenario, this would trigger the hashcat process

    server = WebSocketServer()
    server.set_attack_callback(dummy_attack_handler)

    room_id = generate_room_id()
    print(f"[*] Server is ready. Room ID for clients to join: {room_id}")

    try:
        asyncio.run(server.start())
    except KeyboardInterrupt:
        logging.info("Server shutting down.")