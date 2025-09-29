import flask
from flask import request, jsonify
import qrcode
import socket
import io
import os
import subprocess
import uuid
import threading
import sys
import platform
import tempfile
import argparse
from contextlib import redirect_stdout

# Add HashKitty to the Python path
# This assumes the server is run from the hashkitty-gui directory
sys.path.append(os.path.abspath(os.path.join(os.path.dirname(__file__), '..', 'HashKitty')))
from HashKitty import main as hashkitty_main

app = flask.Flask(__name__)

# In-memory job store
jobs = {}

# Run prereq setup on startup to ensure hashcat and wordlists are ready
hashkitty_main.prereq_setup()

def get_ip_address():
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    try:
        # doesn't even have to be reachable
        s.connect(('10.255.255.255', 1))
        IP = s.getsockname()[0]
    except Exception:
        IP = '127.0.0.1'
    finally:
        s.close()
    return IP

@app.route('/')
def index():
    return "HashKitty Server is running!"

@app.route('/upload', methods=['POST'])
def upload_file_route():
    if 'file' not in request.files:
        return jsonify({"error": "No file part"}), 400
    file = request.files['file']
    if file.filename == '':
        return jsonify({"error": "No selected file"}), 400
    if file:
        # Use a relative path for the uploads folder
        upload_folder = os.path.join(os.path.dirname(__file__), 'uploads')
        os.makedirs(upload_folder, exist_ok=True)
        filepath = os.path.join(upload_folder, file.filename)
        file.save(filepath)
        return jsonify({"message": "File uploaded successfully", "filepath": filepath}), 200

@app.route('/identify', methods=['POST'])
def identify_hash_route():
    data = request.get_json()
    if not data or 'hash' not in data:
        return jsonify({"error": "Missing hash in request"}), 400

    hash_string = data['hash']

    # Create a temporary file to store the hash
    with tempfile.NamedTemporaryFile(mode='w', delete=False, suffix=".txt") as tmp:
        tmp.write(hash_string)
        tmp_path = tmp.name

    try:
        # Ensure the empty file exists for hashcat to run
        _, hashcat_results_folder = hashkitty_main.prereq_setup()
        empty_file_path = os.path.join(hashcat_results_folder, "empty.txt")
        if not os.path.exists(empty_file_path):
             with open(empty_file_path, "w") as f:
                pass

        # Call the non-interactive detect_mode function
        modes = hashkitty_main.detect_mode(tmp_path, users_true=False, empty_file=empty_file_path, interactive=False)

        # Format the response into the structure the app expects
        formatted_modes = [{"mode": mode[0].strip(), "name": " | ".join(mode[1:]).strip()} for mode in modes]

        return jsonify({"modes": formatted_modes})

    finally:
        # Clean up the temporary file
        os.remove(tmp_path)

def run_attack_in_background(job_id, attack_args):
    """This function runs the hashcat attack in a background thread."""
    try:
        jobs[job_id]['status'] = 'running'

        # Create a namespace object to hold the arguments for do_the_thing
        args = argparse.Namespace(
            file=attack_args.get('file'),
            mode=attack_args.get('mode'),
            wordlist=attack_args.get('wordlist'),
            rules=attack_args.get('rules'),
            user=attack_args.get('user', False),
            quiet=attack_args.get('quiet', False),
            disable=attack_args.get('disable', False),
            analysis=attack_args.get('analysis', False),
            show=False  # We handle showing results separately
        )

        # Capture stdout to return to the user
        output_io = io.StringIO()
        with redirect_stdout(output_io):
            hashkitty_main.do_the_thing(args)

        jobs[job_id]['status'] = 'completed'

        # The results are in the potfile. A more robust solution would be for
        # main.py to output results to a structured file for easier parsing.
        hashcat_folder, hashcat_results_folder = hashkitty_main.prereq_setup()
        potfile_path = os.path.join(hashcat_folder, "hashcat.potfile")
        if platform.system() != "Windows":
             potfile_path = os.path.join(hashcat_results_folder, "hashcat.potfile")

        cracked = []
        if os.path.exists(potfile_path):
            with open(potfile_path, 'r') as f:
                cracked = [line.strip() for line in f.readlines()]
        jobs[job_id]['result'] = cracked
        jobs[job_id]['output'] = output_io.getvalue()

    except Exception as e:
        jobs[job_id]['status'] = 'failed'
        jobs[job_id]['error'] = str(e)


@app.route('/attack', methods=['POST'])
def start_attack_route():
    data = request.get_json()
    if not data or 'file' not in data or 'mode' not in data:
        return jsonify({"error": "Missing 'file' or 'mode' in request"}), 400

    job_id = str(uuid.uuid4())
    jobs[job_id] = {'status': 'queued'}

    thread = threading.Thread(target=run_attack_in_background, args=(job_id, data))
    thread.daemon = True
    thread.start()

    return jsonify({"jobId": job_id, "status": "started"}), 202

@app.route('/attack/<jobId>', methods=['GET'])
def get_attack_status_route(jobId):
    job = jobs.get(jobId)
    if not job:
        return jsonify({"error": "Job not found"}), 404

    response = {"jobId": jobId, "status": job.get('status')}

    if job.get('status') == 'completed':
        response['cracked'] = job.get('result', [])
        response['output'] = job.get('output')
    elif job.get('status') == 'failed':
        response['error'] = job.get('error')

    return jsonify(response)


if __name__ == '__main__':
    ip_address = get_ip_address()

    qr = qrcode.QRCode(version=1, error_correction=qrcode.constants.ERROR_CORRECT_L, box_size=10, border=4)
    qr.add_data(f'http://{ip_address}:5000')
    qr.make(fit=True)

    f = io.StringIO()
    qr.print_ascii(out=f)
    f.seek(0)
    print(f.read())

    print(f"[*] Server running on http://{ip_address}:5000")
    print("[*] Scan the QR code with the Android app to connect.")

    app.run(host='0.0.0.0', port=5000)