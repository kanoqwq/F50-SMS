package main

import (
	"bufio"
	"bytes"
	"os"
	"os/exec"
	"regexp"
	"strconv"
)

func parse(input string) string {
	hexWordPattern := regexp.MustCompile(`\b[0-9a-fA-F]{8}\b`)
	var result bytes.Buffer

	scanner := bufio.NewScanner(bytes.NewBufferString(input))
	for scanner.Scan() {
		line := scanner.Text()
		if idx := bytes.IndexRune([]byte(line), '\''); idx != -1 {
			line = line[:idx]
		}
		matches := hexWordPattern.FindAllString(line, -1)
		for _, word := range matches {
			if len(word) != 8 {
				continue
			}
			bytesLE := []string{
				word[6:8],
				word[4:6],
				word[2:4],
				word[0:2],
			}
			for i := 0; i < 4; i += 2 {
				b1, _ := strconv.ParseUint(bytesLE[i], 16, 8)
				b2, _ := strconv.ParseUint(bytesLE[i+1], 16, 8)
				r := rune(uint16(b2)<<8 | uint16(b1))
				if r >= 32 && r != 127 {
					result.WriteRune(r)
				}
			}
		}
	}
	return result.String()
}

func main() {
	if len(os.Args) < 3 {
		os.Stderr.Write([]byte("Usage: ./sendat -c <command> -n <0|1>\n"))
		os.Exit(1)
	}

	var cmdArg string
	var numArg int
	for i := 1; i < len(os.Args)-1; i++ {
		switch os.Args[i] {
		case "-c":
			cmdArg = os.Args[i+1]
		case "-n":
			numArg, _ = strconv.Atoi(os.Args[i+1])
		}
	}

	if cmdArg == "" {
		os.Stderr.Write([]byte("Missing AT command (-c)\n"))
		os.Exit(1)
	}

	// 转义整条 sendAt 命令
	quotedCmd := strconv.Quote("sendAt " + strconv.Itoa(numArg) + " " + cmdArg)
	// 构造完整 shell 命令
	cmdString := `/system/bin/service call vendor.sprd.hardware.log.ILogControl/default 1 s16 "miscserver" s16 ` + quotedCmd

	// 执行命令
	cmd := exec.Command("sh", "-c", cmdString)
	out, err := cmd.CombinedOutput()
	if err != nil {
		os.Stderr.Write([]byte("Shell error: " + err.Error() + "\n"))
		os.Stderr.Write(out)
		os.Exit(1)
	}

	parsed := parse(string(out))
	os.Stdout.Write([]byte(parsed + "\n"))
}
