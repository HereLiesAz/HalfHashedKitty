import customtkinter as ctk
from tkinter import filedialog
import subprocess
import threading
import asyncio
import json
from PIL import Image
import qrcode

# Since this file is now in a subdirectory, we need to adjust the import path
import hashcat_manager as hm
from server import WebSocketServer, generate_room_id

class HashKittyDesktopApp(ctk.CTk):
    def __init__(self):
        super().__init__()

        self.title("HashKitty Desktop")
        self.geometry("900x700")

        # --- Initialize Backend ---
        hm.download_and_extract_hashcat()
        self.server_loop = None
        self.room_id = generate_room_id()
        self.server = WebSocketServer()
        self.server.set_attack_callback(self.handle_remote_attack)

        # Start the server in a separate thread
        self.server_thread = threading.Thread(target=self.run_server_thread, daemon=True)
        self.server_thread.start()

        # --- Build UI ---
        self.grid_rowconfigure(0, weight=1)
        self.grid_columnconfigure(0, weight=1)
        self.tab_view = ctk.CTkTabview(self, anchor="w")
        self.tab_view.grid(row=0, column=0, padx=10, pady=10, sticky="nsew")

        self.attack_tab = self.tab_view.add("Attack")
        self.terminal_tab = self.tab_view.add("Terminal")
        self.server_tab = self.tab_view.add("Server")

        self.configure_attack_tab()
        self.configure_terminal_tab()
        self.configure_server_tab()

        self.tab_view.set("Server")

        self.protocol("WM_DELETE_WINDOW", self.on_closing)

    def on_closing(self):
        """Handle window closing."""
        if self.server_loop and self.server_loop.is_running():
            self.server_loop.call_soon_threadsafe(self.server_loop.stop)
        self.destroy()

    def run_server_thread(self):
        """Runs the asyncio event loop for the server in a dedicated thread."""
        self.server_loop = asyncio.new_event_loop()
        asyncio.set_event_loop(self.server_loop)
        self.server_loop.run_until_complete(self.server.start())

    def configure_attack_tab(self):
        attack_frame = ctk.CTkFrame(self.attack_tab)
        attack_frame.pack(padx=20, pady=20, fill="both", expand=True)
        attack_frame.grid_columnconfigure(1, weight=1)

        # UI Elements for local attack...
        self.hash_file_label = ctk.CTkLabel(attack_frame, text="Hash File:")
        self.hash_file_label.grid(row=0, column=0, padx=10, pady=10, sticky="w")
        self.hash_file_entry = ctk.CTkEntry(attack_frame)
        self.hash_file_entry.grid(row=0, column=1, padx=10, pady=10, sticky="ew")
        self.hash_file_browse_button = ctk.CTkButton(attack_frame, text="Browse...", command=lambda: self.browse_file(self.hash_file_entry))
        self.hash_file_browse_button.grid(row=0, column=2, padx=10, pady=10)

        self.hash_mode_label = ctk.CTkLabel(attack_frame, text="Hash Mode:")
        self.hash_mode_label.grid(row=1, column=0, padx=10, pady=10, sticky="w")
        self.hash_mode_entry = ctk.CTkEntry(attack_frame)
        self.hash_mode_entry.grid(row=1, column=1, padx=10, pady=10, sticky="ew")

        self.attack_mode_label = ctk.CTkLabel(attack_frame, text="Attack Mode:")
        self.attack_mode_label.grid(row=2, column=0, padx=10, pady=10, sticky="w")
        self.attack_mode_entry = ctk.CTkEntry(attack_frame)
        self.attack_mode_entry.grid(row=2, column=1, padx=10, pady=10, sticky="ew")

        self.wordlist_label = ctk.CTkLabel(attack_frame, text="Wordlist (Optional):")
        self.wordlist_label.grid(row=3, column=0, padx=10, pady=10, sticky="w")
        self.wordlist_entry = ctk.CTkEntry(attack_frame)
        self.wordlist_entry.grid(row=3, column=1, padx=10, pady=10, sticky="ew")
        self.wordlist_browse_button = ctk.CTkButton(attack_frame, text="Browse...", command=lambda: self.browse_file(self.wordlist_entry))
        self.wordlist_browse_button.grid(row=3, column=2, padx=10, pady=10)

        self.rules_label = ctk.CTkLabel(attack_frame, text="Rules File (Optional):")
        self.rules_label.grid(row=4, column=0, padx=10, pady=10, sticky="w")
        self.rules_entry = ctk.CTkEntry(attack_frame)
        self.rules_entry.grid(row=4, column=1, padx=10, pady=10, sticky="ew")
        self.rules_browse_button = ctk.CTkButton(attack_frame, text="Browse...", command=lambda: self.browse_file(self.rules_entry))
        self.rules_browse_button.grid(row=4, column=2, padx=10, pady=10)

        self.start_attack_button = ctk.CTkButton(attack_frame, text="Start Local Attack", command=self.start_local_attack)
        self.start_attack_button.grid(row=5, column=0, columnspan=3, pady=20)

    def configure_terminal_tab(self):
        self.terminal_output = ctk.CTkTextbox(self.terminal_tab, state="disabled", font=("monospace", 12))
        self.terminal_output.pack(padx=10, pady=10, fill="both", expand=True)

    def configure_server_tab(self):
        server_frame = ctk.CTkFrame(self.server_tab)
        server_frame.pack(padx=20, pady=20, fill="both", expand=True)

        info_label = ctk.CTkLabel(server_frame, text="Scan this QR code with the mobile app to connect.", font=("", 16))
        info_label.pack(pady=10)

        # --- QR Code ---
        qr_img = qrcode.make(self.room_id)
        qr_ctk_img = ctk.CTkImage(light_image=qr_img, dark_image=qr_img, size=(250, 250))
        self.qr_label = ctk.CTkLabel(server_frame, image=qr_ctk_img, text="")
        self.qr_label.pack(pady=10)

        room_id_label = ctk.CTkLabel(server_frame, text=f"Your Room ID: {self.room_id}", font=("", 14))
        room_id_label.pack(pady=10)

    def browse_file(self, entry_widget):
        filepath = filedialog.askopenfilename()
        if filepath:
            entry_widget.delete(0, "end")
            entry_widget.insert(0, filepath)

    def start_local_attack(self):
        params = {
            "jobId": "local_attack",
            "file": self.hash_file_entry.get(),
            "mode": self.hash_mode_entry.get(),
            "attackMode": self.attack_mode_entry.get(),
            "wordlist": self.wordlist_entry.get(),
            "rules": self.rules_entry.get(),
        }
        self.start_attack_button.configure(state="disabled", text="Attack in Progress...")
        self.clear_terminal()
        self.tab_view.set("Terminal")
        attack_thread = threading.Thread(target=self._execute_hashcat, args=(params, None), daemon=True)
        attack_thread.start()

    def handle_remote_attack(self, params, client):
        """Callback for the server to trigger an attack."""
        self.log_to_terminal(f"\n--- Received remote attack request from {client.conn.remote_address} ---\n")
        self.after(0, lambda: self.tab_view.set("Terminal"))
        attack_thread = threading.Thread(target=self._execute_hashcat, args=(params, client), daemon=True)
        attack_thread.start()

    def _execute_hashcat(self, params, remote_client=None):
        try:
            hashcat_exe = hm.get_hashcat_executable_path()
            if not hm.os.path.exists(hashcat_exe):
                self.log_to_terminal("Hashcat executable not found.")
                if remote_client: self.send_remote_update(remote_client, params, "failed", "Hashcat executable not found on host.", True)
                return

            command = [hashcat_exe, "-m", params["mode"], "-a", params["attackMode"], params["file"]]
            if params.get("wordlist"): command.append(params["wordlist"])
            if params.get("rules"): command.extend(["-r", params["rules"]])

            self.log_to_terminal(f"Executing command: {' '.join(command)}\n\n")
            if remote_client: self.send_remote_update(remote_client, params, "running", f"Executing command: {' '.join(command)}")

            process = subprocess.Popen(command, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True, bufsize=1, universal_newlines=True)
            for line in iter(process.stdout.readline, ''):
                self.log_to_terminal(line)
                if remote_client: self.send_remote_update(remote_client, params, "running", line)
            process.wait()
            self.log_to_terminal("\n--- Attack Finished ---")
            if remote_client: self.send_remote_update(remote_client, params, "completed", "Hashcat process finished.")

        except Exception as e:
            error_msg = f"\nAn error occurred: {e}"
            self.log_to_terminal(error_msg)
            if remote_client: self.send_remote_update(remote_client, params, "failed", error_msg, True)
        finally:
            if not remote_client: self.start_attack_button.configure(state="normal", text="Start Local Attack")

    def send_remote_update(self, client, params, status, data, is_error=False):
        """Sends a status update message to a remote client."""
        res = {"jobId": params.get("jobId"), "status": status}
        if is_error: res["error"] = data
        else: res["output"] = data

        payload_str = json.dumps(res)
        message_to_send = {"type": "status_update", "room_id": client.room, "payload": payload_str}
        final_msg_str = json.dumps(message_to_send)

        if self.server_loop and self.server_loop.is_running():
            asyncio.run_coroutine_threadsafe(client.conn.send(final_msg_str), self.server_loop)

    def log_to_terminal(self, message):
        self.after(0, lambda: self._update_textbox(message))

    def _update_textbox(self, message):
        self.terminal_output.configure(state="normal")
        self.terminal_output.insert("end", message)
        self.terminal_output.see("end")
        self.terminal_output.configure(state="disabled")

    def clear_terminal(self):
        self.after(0, self.terminal_output.delete, "1.0", "end")

if __name__ == "__main__":
    app = HashKittyDesktopApp()
    app.mainloop()