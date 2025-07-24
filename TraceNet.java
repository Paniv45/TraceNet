import java.io.*;
import java.net.*;
import java.util.*;
import java.util.regex.*;

class Hop {
    int hop;
    String ip;
    double latency;
    String location;

    Hop(int hop, String ip, double latency, String location) {
        this.hop = hop;
        this.ip = ip;
        this.latency = latency;
        this.location = location;
    }

    @Override
    public String toString() {
        return String.format("Hop %d: %s - %.2f ms [%s]", hop, ip, latency, location);
    }
}

public class TraceNet {

    public static void main(String[] args) throws Exception {
        Scanner sc = new Scanner(System.in);
        System.out.print("Enter domain or IP: ");
        String target = sc.nextLine().trim();

        String os = System.getProperty("os.name").toLowerCase();
        String command = os.contains("win") ? "tracert" : "traceroute";

        ProcessBuilder pb = new ProcessBuilder(command, target);
        Process process = pb.start();

        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;

        Pattern latencyPattern = Pattern.compile("(\\d+(\\.\\d+)?)\\s*ms");
        Pattern ipPattern = Pattern.compile("\\b(\\d{1,3}(\\.\\d{1,3}){3})\\b");  // IPv4
        Pattern ipv6Pattern = Pattern.compile("([a-fA-F0-9:]+:+)+[a-fA-F0-9]+");   // IPv6

        ArrayList<Hop> hops = new ArrayList<>();
        Hop bottleneck = new Hop(0, "", 0, "N/A");

        while ((line = reader.readLine()) != null) {
            // Optional Debug Line
            System.out.println("[DEBUG] " + line);

            if (line.contains("Request timed out.") || line.contains("* * *")) {
                continue; // or mark as unreachable hop
            }

            String[] tokens = line.trim().split("\\s+");
            if (tokens.length > 1) {
                try {
                    int hopNumber = Integer.parseInt(tokens[0]);

                    // Extract IP
                    String ip = "No response";
                    int start = line.indexOf('[');
                    int end = line.indexOf(']');
                    if (start != -1 && end != -1 && end > start) {
                        ip = line.substring(start + 1, end);
                    } else {
                        Matcher ipMatch = ipPattern.matcher(line);
                        Matcher ipv6Match = ipv6Pattern.matcher(line);
                        if (ipMatch.find()) {
                            ip = ipMatch.group();
                        } else if (ipv6Match.find()) {
                            ip = ipv6Match.group();
                        }
                    }

                    // Extract latency
                    Matcher timeMatch = latencyPattern.matcher(line);
                    double latency = timeMatch.find() ? Double.parseDouble(timeMatch.group(1)) : 0;

                    // Get location only if valid IP (skip "No response")
                    String location = (!ip.equals("No response")) ? getIPLocation(ip) : "N/A";

                    Hop h = new Hop(hopNumber, ip, latency, location);
                    hops.add(h);

                    if (latency > bottleneck.latency) {
                        bottleneck = h;
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        }

        System.out.println("\n🛰️ Trace Completed:\n");
        for (Hop h : hops) {
            System.out.println(h);
        }

        System.out.printf("\n⚠️ Bottleneck: Hop %d (%s) - %.2f ms [%s]\n",
                bottleneck.hop, bottleneck.ip, bottleneck.latency, bottleneck.location);

        // Save report
        try (PrintWriter out = new PrintWriter("trace_report.txt")) {
            out.println("Trace Report for: " + target + "\n");
            for (Hop h : hops) {
                out.println(h);
            }
            out.printf("\nBottleneck: Hop %d (%s) - %.2f ms [%s]\n",
                    bottleneck.hop, bottleneck.ip, bottleneck.latency, bottleneck.location);
        }

        System.out.println("\n📄 Report saved as 'trace_report.txt'");
    }

    // Get IP Location without external JSON library
    public static String getIPLocation(String ip) {
        try {
            URL url = new URL("http://ip-api.com/json/" + ip);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(4000);
            conn.setReadTimeout(4000);

            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder json = new StringBuilder();
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                json.append(inputLine);
            }
            in.close();

            String data = json.toString();

            String city = extract(data, "\"city\":\"", "\"");
            String country = extract(data, "\"country\":\"", "\"");
            String org = extract(data, "\"org\":\"", "\"");

            return city + ", " + country + " (" + org + ")";
        } catch (Exception e) {
            return "Geo API failed";
        }
    }

    public static String extract(String json, String start, String end) {
        int s = json.indexOf(start);
        if (s == -1)
            return "N/A";
        s += start.length();
        int e = json.indexOf(end, s);
        return (e == -1) ? "N/A" : json.substring(s, e);
    }
}
