package main

import (
	"log"
	"os"
	"gokitty-relay/relay"
	"github.com/kardianos/service"
)

// program struct holds the state for our service.
type program struct{
	stop chan struct{}
}

// Start is called when the service is started.
func (p *program) Start(s service.Service) error {
	p.stop = make(chan struct{})
	log.Println("Starting GoKittyRelay service...")
	// Start the server in a separate goroutine.
	go relay.RunRelayServer(p.stop)
	return nil
}

// Stop is called when the service is stopped.
func (p *program) Stop(s service.Service) error {
	log.Println("Stopping GoKittyRelay service...")
	// Signal the server to stop by closing the channel.
	if p.stop != nil {
		close(p.stop)
	}
	return nil
}

func main() {
	svcConfig := &service.Config{
		Name:        "GoKittyRelay",
		DisplayName: "GoKitty Relay Server",
		Description: "A WebSocket relay server for the Half-Hashed Kitty project.",
	}

	prg := &program{}
	s, err := service.New(prg, svcConfig)
	if err != nil {
		log.Fatal(err)
	}

	// This part allows for command-line management of the service
	// e.g., `gokitty-relay install`, `gokitty-relay start`, etc.
	if len(os.Args) > 1 {
		err = service.Control(s, os.Args[1])
		if err != nil {
			log.Printf("Failed to control service: %s", err)
		}
		return
	}

	err = s.Run()
	if err != nil {
		log.Fatal(err)
	}
}