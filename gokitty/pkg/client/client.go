package client

import (
	"encoding/json"
	"fmt"
	"log"

	"github.com/gorilla/websocket"
	qrcode "github.com/skip2/go-qrcode"
	"github.com/teris-io/shortid"
	"gokitty/pkg/hashcat"
	"gokitty/pkg/models"
)

// RunDesktopClient starts the desktop client application.
func RunDesktopClient(relayURL string) {
	hashcatPath, err := hashcat.PrereqSetup()
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

	joinMsgBytes, err := json.Marshal(models.Message{Type: "join", RoomID: roomID})
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
		var msg models.Message
		if err := json.Unmarshal(rawMessage, &msg); err != nil {
			log.Printf("Failed to unmarshal message: %v", err)
			continue
		}
		if msg.Type == "attack" {
			var params models.AttackParams
			payloadStr, ok := msg.Payload.(string)
			if !ok {
				log.Printf("Payload is not a string for attack message")
				continue
			}
			if err := json.Unmarshal([]byte(payloadStr), &params); err != nil {
				log.Printf("Failed to unmarshal attack params: %v", err)
				continue
			}
			go hashcat.RunAttack(params, conn, hashcatPath, roomID)
		}
	}
}