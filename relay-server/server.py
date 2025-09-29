from flask import Flask, request
from flask_socketio import SocketIO, emit, join_room, leave_room
import uuid

app = Flask(__name__)
socketio = SocketIO(app, cors_allowed_origins="*")

# In-memory store for clients and their rooms
clients = {}

def generate_unique_id():
    """Generates a simple, unique ID for clients."""
    return uuid.uuid4().hex[:6]

@socketio.on('connect')
def handle_connect():
    print(f"Client connected: {request.sid}")

@socketio.on('disconnect')
def handle_disconnect():
    print(f"Client disconnected: {request.sid}")
    # Clean up the client's room if they were a host
    for room_id, host_sid in list(clients.items()):
        if host_sid == request.sid:
            del clients[room_id]
            print(f"Desktop host for room {room_id} disconnected. Room closed.")
            break

@socketio.on('register_desktop')
def handle_register_desktop():
    """
    When a desktop client connects, it registers itself and gets a unique room ID.
    """
    room_id = generate_unique_id()
    clients[room_id] = request.sid
    join_room(room_id)
    print(f"Desktop client {request.sid} registered and created room: {room_id}")
    emit('desktop_registered', {'room_id': room_id})

@socketio.on('join_room')
def handle_join_room(data):
    """
    When a mobile client connects, it joins the room of its desktop pair.
    """
    room_id = data.get('room_id')
    if room_id in clients:
        join_room(room_id)
        print(f"Mobile client {request.sid} joined room: {room_id}")
        # Notify the desktop that the mobile client has connected
        desktop_sid = clients[room_id]
        emit('mobile_joined', {'sid': request.sid}, room=desktop_sid)
    else:
        emit('error', {'message': 'Room not found'})

@socketio.on('message_from_mobile')
def handle_message_from_mobile(data):
    """
    Relay a message from the mobile client to the desktop client in the same room.
    """
    room_id = data.get('room_id')
    if room_id in clients:
        desktop_sid = clients[room_id]
        emit('message_to_desktop', data['payload'], room=desktop_sid)
        print(f"Relayed message from mobile to desktop in room {room_id}")
    else:
        emit('error', {'message': 'Not connected to a valid room'})

@socketio.on('message_from_desktop')
def handle_message_from_desktop(data):
    """
    Relay a message from the desktop client to the mobile client in the same room.
    """
    room_id = data.get('room_id')
    # The desktop is already in the room, so we just broadcast to all other members (the phone)
    emit('message_to_mobile', data['payload'], room=room_id, include_self=False)
    print(f"Relayed message from desktop to mobile in room {room_id}")


if __name__ == '__main__':
    print("[*] Starting Relay Server...")
    socketio.run(app, host='0.0.0.0', port=5001)