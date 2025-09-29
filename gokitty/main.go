package main

import (
	"bufio"
	"encoding/json"
	"flag"
	"fmt"
	"io"
	"log"
	"net"
	"net/http"
	"os"
	"os/exec"
	"path/filepath"
	"regexp"
	"runtime"
	"sync"

	"github.com/bodgit/sevenzip"
	"github.com/gorilla/websocket"
	qrcode "github.com/skip2/go-qrcode"
	"github.com/teris-io/shortid"
)

// --- Shared Data Structures ---

type Message struct {
	Type    string      `json:"type"`
	Payload interface{} `json:"payload"`
	RoomID  string      `json:"room_id"`
	Sender  *Client     `json:"-"`
}

type AttackParams struct {
	JobID    string `json:"jobId"`
	File     string `json:"file"`
	Mode     string `json:"mode"`
	Wordlist string `json:"wordlist"`
	Rules    string `json:"rules,omitempty"`
}

// --- Relay Server Logic ---

var upgrader = websocket.Upgrader{
	CheckOrigin: func(r *http.Request) bool { return true },
}

type Client struct {
	hub  *Hub
	conn *websocket.Conn
	send chan []byte
	room string
}

type Hub struct {
	rooms      map[string]map[*Client]bool
	broadcast  chan Message
	register   chan *Client
	unregister chan *Client
	mutex      sync.Mutex
}

func newHub() *Hub {
	return &Hub{
		broadcast:  make(chan Message),
		register:   make(chan *Client),
		unregister: make(chan *Client),
		rooms:      make(map[string]map[*Client]bool),
	}
}

func (h *Hub) run() {
	for {
		select {
		case client := <-h.register:
			h.mutex.Lock()
			if h.rooms[client.room] == nil {
				h.rooms[client.room] = make(map[*Client]bool)
			}
			h.rooms[client.room][client] = true
			h.mutex.Unlock()
		case client := <-h.unregister:
			h.mutex.Lock()
			if clients, ok := h.rooms[client.room]; ok {
				delete(clients, client)
				if len(clients) == 0 {
					delete(h.rooms, client.room)
				}
			}
			h.mutex.Unlock()
			close(client.send)
		case message := <-h.broadcast:
			h.mutex.Lock()
			if clients, ok := h.rooms[message.RoomID]; ok {
				for client := range clients {
					if client != message.Sender {
						select {
						case client.send <- message.Payload.([]byte):
						default:
							close(client.send)
							delete(clients, client)
						}
					}
				}
			}
			h.mutex.Unlock()
		}
	}
}

func (c *Client) readPump() {
	defer func() { c.hub.unregister <- c; c.conn.Close() }()
	for {
		_, rawMessage, err := c.conn.ReadMessage()
		if err != nil {
			break
		}
		var msg Message
		if err := json.Unmarshal(rawMessage, &msg); err != nil {
			continue
		}
		msg.Sender = c
		c.room = msg.RoomID
		if msg.Type == "join" {
			c.hub.register <- c
		}
		msg.Payload = rawMessage
		c.hub.broadcast <- msg
	}
}

func (c *Client) writePump() {
	defer c.conn.Close()
	for message := range c.send {
		c.conn.WriteMessage(websocket.TextMessage, message)
	}
}

func serveWs(hub *Hub, w http.ResponseWriter, r *http.Request) {
	conn, err := upgrader.Upgrade(w, r, nil)
	if err != nil {
		log.Println(err)
		return
	}
	client := &Client{hub: hub, conn: conn, send: make(chan []byte, 256)}
	go client.writePump()
	go client.readPump()
}

func runRelayServer() {
	hub := newHub()
	go hub.run()
	http.HandleFunc("/ws", func(w http.ResponseWriter, r *http.Request) { serveWs(hub, w, r) })
	log.Println("[*] Relay server starting on :5001")
	if err := http.ListenAndServe(":5001", nil); err != nil {
		log.Fatal("ListenAndServe: ", err)
	}
}

// --- Desktop Client Logic ---

func prereqSetup() (string, error) {
	homeDir, err := os.UserHomeDir()
	if err != nil {
		return "", err
	}
	gokittyDir := filepath.Join(homeDir, ".gokitty")
	if err := os.MkdirAll(gokittyDir, 0755); err != nil {
		return "", err
	}

	hashcatDir := filepath.Join(gokittyDir, "hashcat-7.1.2")
	if _, err := os.Stat(hashcatDir); err == nil {
		log.Println("[*] Hashcat directory already exists.")
		return hashcatDir, nil
	}

	log.Println("[*] Downloading Hashcat v7.1.2...")
	url := "https://hashcat.net/files/hashcat-7.1.2.7z"
	resp, err := http.Get(url)
	if err != nil {
		return "", fmt.Errorf("failed to download hashcat: %w", err)
	}
	defer resp.Body.Close()

	archivePath := filepath.Join(gokittyDir, "hashcat.7z")
	out, err := os.Create(archivePath)
	if err != nil {
		return "", err
	}
	defer out.Close()
	io.Copy(out, resp.Body)

	log.Println("[*] Extracting Hashcat...")
	r, err := sevenzip.OpenReader(archivePath)
	if err != nil {
		return "", fmt.Errorf("failed to open archive: %w", err)
	}
	defer r.Close()

	for _, f := range r.File {
		dstPath := filepath.Join(gokittyDir, f.Name)
		if f.FileInfo().IsDir() {
			os.MkdirAll(dstPath, f.Mode())
			continue
		}
		if err := os.MkdirAll(filepath.Dir(dstPath), 0755); err != nil {
			return "", err
		}
		dst, err := os.Create(dstPath)
		if err != nil {
			return "", err
		}
		src, err := f.Open()
		if err != nil {
			dst.Close()
			return "", err
		}
		if _, err := io.Copy(dst, src); err != nil {
			src.Close()
			dst.Close()
			return "", err
		}
		src.Close()
		dst.Close()
	}

	log.Println("[*] Hashcat setup complete.")
	os.Remove(archivePath)
	return hashcatDir, nil
}

func runAttack(params AttackParams, conn *websocket.Conn, hashcatPath string) {
	log.Printf("Starting attack for job %s...", params.JobID)

	sendUpdate := func(status, data string, isError bool) {
		res := map[string]interface{}{"jobId": params.JobID, "status": status}
		if isError {
			res["error"] = data
		} else {
			res["output"] = data
		}
		payloadBytes, _ := json.Marshal(res)
		msg, _ := json.Marshal(Message{Type: "status_update", RoomID: params.RoomID, Payload: string(payloadBytes)})
		conn.WriteMessage(websocket.TextMessage, msg)
	}

	// Input Validation
	if matched, _ := regexp.MatchString(`^[0-9]+$`, params.Mode); !matched {
		sendUpdate("failed", "Invalid hash mode specified.", true)
		return
	}
	cleanFile := filepath.Clean(params.File)
	if _, err := os.Stat(cleanFile); os.IsNotExist(err) {
		sendUpdate("failed", fmt.Sprintf("Hash file not found: %s", cleanFile), true)
		return
	}

	executable := filepath.Join(hashcatPath, "hashcat")
	if runtime.GOOS == "windows" {
		executable += ".exe"
	}

	args := []string{"-m", params.Mode, "-a", "0", cleanFile}
	if params.Wordlist != "" {
		args = append(args, filepath.Clean(params.Wordlist))
	}

	cmd := exec.Command(executable, args...)
	stdout, _ := cmd.StdoutPipe()
	cmd.Stderr = cmd.Stdout

	if err := cmd.Start(); err != nil {
		sendUpdate("failed", fmt.Sprintf("Failed to start hashcat: %v", err), true)
		return
	}

	scanner := bufio.NewScanner(stdout)
	for scanner.Scan() {
		line := scanner.Text()
		sendUpdate("running", line, false)
	}

	if err := cmd.Wait(); err != nil {
		sendUpdate("failed", fmt.Sprintf("Hashcat process failed: %v", err), true)
	} else {
		sendUpdate("completed", "Hashcat process finished.", false)
	}
}

func runDesktopClient(relayURL string) {
	hashcatPath, err := prereqSetup()
	if err != nil {
		log.Fatalf("Failed prerequisite setup: %v", err)
	}

	conn, _, err := websocket.DefaultDialer.Dial(relayURL, nil)
	if err != nil {
		log.Fatalf("Failed to connect to relay: %v", err)
	}
	defer conn.Close()
	log.Println("[*] Connected to relay server.")

	roomID, err := shortid.Generate()
	if err != nil {
		log.Fatalf("Failed to generate room ID: %v", err)
	}

	joinMsgBytes, err := json.Marshal(Message{Type: "join", RoomID: roomID})
	if err != nil {
		log.Fatalf("Failed to create join message: %v", err)
	}
	conn.WriteMessage(websocket.TextMessage, joinMsgBytes)

	qr, err := qrcode.New(roomID, qrcode.Medium)
	if err != nil {
		log.Fatalf("Failed to generate QR code: %v", err)
	}
	fmt.Println(qr.ToString(true))
	log.Printf("[*] Scan QR code with the mobile app. Room ID: %s", roomID)

	for {
		_, rawMessage, err := conn.ReadMessage()
		if err != nil {
			log.Println("Read error:", err)
			break
		}
		var msg Message
		if err := json.Unmarshal(rawMessage, &msg); err != nil {
			continue
		}
		if msg.Type == "attack" {
			var params AttackParams
			payloadStr, ok := msg.Payload.(string)
			if !ok { continue }
			if err := json.Unmarshal([]byte(payloadStr), &params); err != nil {
				continue
			}
			params.RoomID = roomID
			go runAttack(params, conn, hashcatPath)
		}
	}
}

// --- Main ---

func main() {
	mode := flag.String("mode", "client", "Run in 'relay' or 'client' mode")
	relayURL := flag.String("relay", "ws://localhost:5001/ws", "URL of the relay server")
	flag.Parse()

	if *mode == "relay" {
		runRelayServer()
	} else if *mode == "client" {
		runDesktopClient(*relayURL)
	} else {
		log.Fatalf("Invalid mode: %s. Choose 'relay' or 'client'.", *mode)
	}
}