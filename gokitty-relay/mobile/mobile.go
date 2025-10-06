package mobile

import (
	"gokitty-relay/relay"
)

var stop chan struct{}

// StartRelay starts the relay server.
// This function is intended to be called from mobile apps. It is a blocking call.
func StartRelay() {
	// Prevent starting if already running
	if stop != nil {
		// Or log that it's already running
		return
	}
	stop = make(chan struct{})
	// This will block until StopRelay is called
	relay.RunRelayServer(stop)
}

// StopRelay stops the running relay server by closing the stop channel.
func StopRelay() {
	if stop != nil {
		close(stop)
		stop = nil
	}
}