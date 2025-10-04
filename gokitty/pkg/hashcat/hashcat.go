package hashcat

import (
	"bufio"
	"encoding/json"
	"fmt"
	"io"
	"log"
	"os"
	"os/exec"
	"path/filepath"
	"regexp"
	"runtime"
	"strings"
	"net/http"

	"github.com/bodgit/sevenzip"
	"github.com/gorilla/websocket"
	"gokitty/pkg/models"
)

// PrereqSetup ensures Hashcat is downloaded and ready to use.
func PrereqSetup() (string, error) {
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

// validatePath cleans and validates a file path.
func validatePath(path, fileType string) (string, error) {
	if path == "" {
		return "", fmt.Errorf("%s path is empty", fileType)
	}
	cleanedPath := filepath.Clean(path)
	if strings.Contains(cleanedPath, "..") {
		return "", fmt.Errorf("invalid %s path: path traversal is not allowed", fileType)
	}
	if _, err := os.Stat(cleanedPath); os.IsNotExist(err) {
		return "", fmt.Errorf("%s not found: %s", fileType, cleanedPath)
	}
	return cleanedPath, nil
}

// RunAttack executes a Hashcat attack with the given parameters.
func RunAttack(params models.AttackParams, conn *websocket.Conn, hashcatPath string, roomID string) {
	log.Printf("Starting attack for job %s...", params.JobID)

	sendUpdate := func(status, data string, isError bool) {
		res := map[string]interface{}{"jobId": params.JobID, "status": status}
		if isError {
			res["error"] = data
		} else {
			res["output"] = data
		}
		payloadBytes, _ := json.Marshal(res)
		msg, _ := json.Marshal(models.Message{Type: "status_update", RoomID: roomID, Payload: string(payloadBytes)})
		conn.WriteMessage(websocket.TextMessage, msg)
	}

	// Input Validation
	if matched, _ := regexp.MatchString(`^[0-9]+$`, params.Mode); !matched {
		sendUpdate("failed", "Invalid hash mode specified.", true)
		return
	}
	if matched, _ := regexp.MatchString(`^[0-9]+$`, params.AttackMode); !matched {
		sendUpdate("failed", "Invalid attack mode specified.", true)
		return
	}

	cleanFile, err := validatePath(params.File, "Hash file")
	if err != nil {
		sendUpdate("failed", err.Error(), true)
		return
	}

	executable := filepath.Join(hashcatPath, "hashcat")
	if runtime.GOOS == "windows" {
		executable += ".exe"
	}

	args := []string{"-m", params.Mode, "-a", params.AttackMode, cleanFile}
	if params.Wordlist != "" {
		cleanWordlist, err := validatePath(params.Wordlist, "Wordlist")
		if err != nil {
			sendUpdate("failed", err.Error(), true)
			return
		}
		args = append(args, cleanWordlist)
	}
	if params.Rules != "" {
		cleanRules, err := validatePath(params.Rules, "Rules file")
		if err != nil {
			sendUpdate("failed", err.Error(), true)
			return
		}
		args = append(args, "-r", cleanRules)
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