import asyncio
import json
import logging
import websockets
import shortuuid

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

class WebSocketServer:
    def __init__(self, host="0.0.0.0", port=5001):
        self.host = host
        self.port = port
        self.hub = Hub()
        self.attack_callback = None

    def set_attack_callback(self, callback):
        """Sets a callback function to be called when an attack is requested."""
        self.attack_callback = callback

    async def handler(self, websocket, path):
        client = Client(websocket, self.hub)
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
                            # The payload of an attack message is a stringified JSON
                            attack_params = json.loads(msg.get("payload", "{}"))
                            self.attack_callback(attack_params, client)

                    # Broadcast the original raw message to other clients in the room
                    await self.hub.broadcast(raw_message, client)

                except json.JSONDecodeError:
                    logging.error(f"Failed to decode JSON message: {raw_message}")
                except Exception as e:
                    logging.error(f"Error processing message: {e}")
        finally:
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