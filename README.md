# TraceNet – Network Bottleneck Analyzer

TraceNet is a command-line Java-based tool that analyzes network paths to detect slow or bottleneck hops using traceroute. It parses each hop's IP and latency using regex and maps them to real-world geo-locations (like city, country, organization) using a public API.

---

## 🚀 Features

- Performs real-time traceroute to any domain/IP.
- Extracts hop-wise IP and latency details using regex.
- Uses public API to map IPs to geo-locations (e.g., "Sydney, Australia (Google International, LLC)").
- Identifies the bottleneck hop with the highest latency.
- Saves a full hop-by-hop trace report to a `.txt` file.

---

## 🛠 Tech Stack

- Java (Core Java)
- Regex (for IP and latency extraction)
- OS-level command execution using `ProcessBuilder`
- `ip-api.com` for IP-to-location mapping

---

## 📄 Sample Output

```txt
Hop 1: unknown - 5.00 ms [N/A]
Hop 7: unknown - 240.00 ms [N/A]
Hop 21: 2404:6800:4002:829::200e - 39.00 ms [Sydney, Australia (Google International, LLC)]

⚠️ Bottleneck: Hop 7 (unknown) - 240.00 ms [N/A]
