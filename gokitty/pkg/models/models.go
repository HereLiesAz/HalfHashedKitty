package models

import "github.com/gorilla/websocket"

// Message defines the structure for WebSocket messages.
type Message struct {
	Type    string      `json:"type"`
	Payload interface{} `json:"payload"`
	RoomID  string      `json:"room_id"`
	Sender  *websocket.Conn `json:"-"`
}

// AttackParams defines the parameters for a Hashcat attack.
type AttackParams struct {
	JobID      string `json:"jobId"`
	File       string `json:"file"`
	Mode       string `json:"mode"`
	AttackMode string `json:"attackMode"`
	Wordlist   string `json:"wordlist"`
	Rules      string `json:"rules,omitempty"`
}