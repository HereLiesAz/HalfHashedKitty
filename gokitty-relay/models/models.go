package models

import "github.com/gorilla/websocket"

// Message defines the structure for websocket messages.
type Message struct {
	Type    string      `json:"type"`
	RoomID  string      `json:"roomId"`
	Payload interface{} `json:"payload"`
	Sender  *websocket.Conn `json:"-"` // Ignored by JSON
}