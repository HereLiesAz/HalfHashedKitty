package main

import (
	"encoding/json"
	"fmt"
	"log"
	"net"
	"net/http"
	"os"
	"os/exec"
	"runtime"
	"sync"

	qrcode "github.com/skip2/go-qrcode"
	"github.com/teris-io/shortid"
)

// --- Job Management ---

type Job struct {
	ID      string `json:"id"`
	Status  string `json:"status"`
	Output  string `json:"output,omitempty"`
	Error   string `json:"error,omitempty"`
}

var (
	jobs  = make(map[string]*Job)
	mutex = &sync.Mutex{}
)

// --- HTTP Handlers ---

func handleAttack(w http.ResponseWriter, r *http.Request) {
	if r.Method != "POST" {
		http.Error(w, "Only POST method is allowed", http.StatusMethodNotAllowed)
		return
	}

	var req map[string]string
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		http.Error(w, err.Error(), http.StatusBadRequest)
		return
	}

	jobID, _ := shortid.Generate()
	job := &Job{ID: jobID, Status: "queued"}
	mutex.Lock()
	jobs[jobID] = job
	mutex.Unlock()

	go runAttack(job, req)

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(job)
}

func handleStatus(w http.ResponseWriter, r *http.Request) {
	jobID := r.URL.Path[len("/attack/"):]
	mutex.Lock()
	job, ok := jobs[jobID]
	mutex.Unlock()

	if !ok {
		http.NotFound(w, r)
		return
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(job)
}

// --- Helper Functions ---

func runAttack(job *Job, params map[string]string) {
	job.Status = "running"
	log.Printf("Starting job %s...", job.ID)

	executable := "hashcat"
	if runtime.GOOS == "windows" {
		executable = "hashcat.exe"
	}

	args := []string{
		"-m", params["mode"],
		"-a", "0",
		params["file"],
		params["wordlist"],
	}
	if rules, ok := params["rules"]; ok && rules != "" {
		args = append(args, "-r", rules)
	}

	cmd := exec.Command(executable, args...)
	output, err := cmd.CombinedOutput()

	mutex.Lock()
	if err != nil {
		job.Status = "failed"
		job.Error = err.Error()
		job.Output = string(output)
		log.Printf("Job %s failed: %v", job.ID, err)
	} else {
		job.Status = "completed"
		job.Output = string(output)
		log.Printf("Job %s completed.", job.ID)
	}
	mutex.Unlock()
}

func getLocalIP() string {
	addrs, err := net.InterfaceAddrs()
	if err != nil {
		return "127.0.0.1"
	}
	for _, address := range addrs {
		if ipnet, ok := address.(*net.IPNet); ok && !ipnet.IP.IsLoopback() {
			if ipnet.IP.To4() != nil {
				return ipnet.IP.String()
			}
		}
	}
	return "127.0.0.1"
}

// --- Main Application ---

func main() {
	// This is a placeholder for the prerequisite setup.
	// In a real application, this would be ported to Go.
	log.Println("[*] Ensuring prerequisites are met (Hashcat, wordlists)...")
	cmd := exec.Command("python", "-c", "from HashKitty import main; main.prereq_setup()")
	if err := cmd.Run(); err != nil {
		log.Printf("Warning: Failed to run Python prerequisite setup. Please ensure Hashcat is installed manually. Error: %v", err)
	}

	ip := getLocalIP()
	port := "8080"
	serverURL := fmt.Sprintf("http://%s:%s", ip, port)

	qr, _ := qrcode.New(serverURL, qrcode.Medium)
	fmt.Println(qr.ToString(true))
	log.Printf("[*] Server starting on %s", serverURL)
	log.Println("[*] Scan the QR code with the mobile app to connect.")

	http.HandleFunc("/attack", handleAttack)
	http.HandleFunc("/attack/", handleStatus)

	if err := http.ListenAndServe(":"+port, nil); err != nil {
		log.Fatalf("Failed to start server: %v", err)
	}
}