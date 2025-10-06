package relay

import (
	"context"
	"encoding/json"
	"log"
	"net/http"
	"sync"
	"time"

	"github.com/gorilla/websocket"
	"gokitty-relay/models"
)

// Client represents a WebSocket client.
type Client struct {
	hub  *Hub
	conn *websocket.Conn
	send chan []byte
	room string
}

// Hub maintains the set of active clients and broadcasts messages.
type Hub struct {
	rooms      map[string]map[*Client]bool
	broadcast  chan models.Message
	register   chan *Client
	unregister chan *Client
	mutex      sync.Mutex
}

// NewHub creates a new Hub.
func NewHub() *Hub {
	return &Hub{
		broadcast:  make(chan models.Message),
		register:   make(chan *Client),
		unregister: make(chan *Client),
		rooms:      make(map[string]map[*Client]bool),
	}
}

// Run starts the Hub.
func (h *Hub) Run() {
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
					if client.conn != message.Sender {
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
		var msg models.Message
		if err := json.Unmarshal(rawMessage, &msg); err != nil {
			log.Printf("Failed to unmarshal message: %v", err)
			continue
		}
		msg.Sender = c.conn
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

var upgrader = websocket.Upgrader{
	CheckOrigin: func(r *http.Request) bool { return true },
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

// RunRelayServer starts the WebSocket relay server and blocks until a stop signal is received.
func RunRelayServer(stop chan struct{}) {
	hub := NewHub()
	go hub.Run()

	mux := http.NewServeMux()
	mux.HandleFunc("/ws", func(w http.ResponseWriter, r *http.Request) { serveWs(hub, w, r) })

	server := &http.Server{Addr: ":5001", Handler: mux}

	go func() {
		log.Println("[*] Relay server starting on :5001")
		if err := server.ListenAndServe(); err != nil && err != http.ErrServerClosed {
			log.Fatalf("ListenAndServe error: %v", err)
		}
	}()

	// Wait for stop signal
	<-stop
	log.Println("Shutting down relay server...")

	ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
	defer cancel()

	if err := server.Shutdown(ctx); err != nil {
		log.Fatalf("Server Shutdown Failed: %+v", err)
	}

	log.Println("Server exited properly")
}