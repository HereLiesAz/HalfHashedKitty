import socketio
import qrcode
import io
import os
import threading
import sys
import argparse
from contextlib import redirect_stdout
import time

# Add HashKitty to the Python path
sys.path.append(os.path.abspath(os.path.join(os.path.dirname(__file__), '..', 'HashKitty')))
from HashKitty import main as hashkitty_main

# --- Configuration ---
RELAY_SERVER_URL = os.environ.get('RELAY_URL', 'http://localhost:5001')

# --- Globals ---
sio = socketio.Client()
room_id = None

@sio.event
def connect():
    print("[*] Connected to relay server.")
    print("[*] Registering as a desktop client...")
    sio.emit('register_desktop')

@sio.event
def connect_error(data):
    print(f"[!] Connection to relay failed: {data}")

@sio.event
def disconnect():
    print("[*] Disconnected from relay server.")

@sio.on('desktop_registered')
def on_desktop_registered(data):
    global room_id
    room_id = data['room_id']
    print("\n" + "="*40)
    print(f"      DESKTOP CLIENT REGISTERED      ")
    print(f"      Your unique ID is: {room_id}")
    print("="*40 + "\n")

    # Generate and print the QR code for the mobile app
    qr = qrcode.QRCode(version=1, error_correction=qrcode.constants.ERROR_CORRECT_L, box_size=10, border=4)
    qr.add_data(room_id)
    qr.make(fit=True)

    f = io.StringIO()
    qr.print_ascii(out=f)
    f.seek(0)
    print(f.read())
    print(f"[*] Scan this QR code with the mobile app to connect.")
    print("[*] Waiting for commands from the mobile app...\n")

@sio.on('message_to_desktop')
def on_message_to_desktop(data):
    """Handles commands received from the mobile app via the relay."""
    print(f"[*] Received command from mobile: {data}")

    # Assuming the payload is for an attack
    job_id = data.get('jobId', 'unknown_job')
    attack_args = data.get('payload')

    if not attack_args:
        print("[!] Invalid command payload received.")
        return

    thread = threading.Thread(target=run_attack_in_background, args=(job_id, attack_args))
    thread.daemon = True
    thread.start()

def run_attack_in_background(job_id, attack_args):
    """Runs the hashcat attack and sends status updates back to the mobile app."""
    try:
        sio.emit('message_from_desktop', {'room_id': room_id, 'payload': {'jobId': job_id, 'status': 'running'}})

        args = argparse.Namespace(
            file=attack_args.get('file'),
            mode=attack_args.get('mode'),
            wordlist=attack_args.get('wordlist'),
            rules=attack_args.get('rules'),
            user=attack_args.get('user', False),
            quiet=attack_args.get('quiet', True), # For non-interactive use
            disable=attack_args.get('disable', False),
            analysis=attack_args.get('analysis', True), # Enable analysis by default
            show=False
        )

        output_io = io.StringIO()
        with redirect_stdout(output_io):
            hashkitty_main.do_the_thing(args)

        # Retrieve results from the potfile
        hashcat_folder, hashcat_results_folder = hashkitty_main.prereq_setup()
        potfile_path = os.path.join(hashcat_folder, "hashcat.potfile")
        if sys.platform != "win32":
             potfile_path = os.path.join(hashcat_results_folder, "hashcat.potfile")

        cracked = []
        if os.path.exists(potfile_path):
            with open(potfile_path, 'r') as f:
                cracked = [line.strip() for line in f.readlines()]

        result_payload = {
            'jobId': job_id,
            'status': 'completed',
            'cracked': cracked,
            'output': output_io.getvalue()
        }
        sio.emit('message_from_desktop', {'room_id': room_id, 'payload': result_payload})
        print(f"[*] Attack for job {job_id} completed. Results sent.")

    except Exception as e:
        print(f"[!] Error during attack for job {job_id}: {e}")
        error_payload = {
            'jobId': job_id,
            'status': 'failed',
            'error': str(e)
        }
        sio.emit('message_from_desktop', {'room_id': room_id, 'payload': error_payload})

if __name__ == '__main__':
    print("[*] Starting HashKitty Desktop Client...")

    # Run prerequisite setup on startup
    try:
        hashkitty_main.prereq_setup()
    except Exception as e:
        print(f"[!] Prerequisite setup failed: {e}")
        sys.exit(1)

    # Connect to the relay server
    while not sio.connected:
        try:
            sio.connect(RELAY_SERVER_URL)
        except socketio.exceptions.ConnectionError as e:
            print(f"[!] Could not connect to relay server at {RELAY_SERVER_URL}. Retrying in 10 seconds...")
            time.sleep(10)

    sio.wait()