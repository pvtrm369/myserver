import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ConcurrentHashMap;

public class ResponseGenerator {
    private static final ConcurrentHashMap<String, String> cache = new ConcurrentHashMap<>();
    private static final String PUBLIC_DIR = "public"; // Folder where static files are stored

    public static String generateResponse(String request) {
        String key = extractPath(request);

        // Ignore favicon requests
        if (key.equals("/favicon.ico")) {
            return "HTTP/1.1 404 Not Found\r\nContent-Length: 0\r\n\r\n";
        }

        // Default to index.html if requesting "/"
        if (key.equals("/")) {
            key = "/index.html";
        }

        Path filePath = Paths.get(PUBLIC_DIR, key);
        if (Files.exists(filePath)) {
            try {
                // Check cache
                if (cache.containsKey(key)) {
                    System.out.println("Cache hit for: " + key);
                    return cache.get(key);
                }

                // Read file contents
                byte[] fileBytes = Files.readAllBytes(filePath);
                String contentType = getContentType(key);
                String response = "HTTP/1.1 200 OK\r\n" +
                        "Content-Type: " + contentType + "\r\n" +
                        "Content-Length: " + fileBytes.length + "\r\n\r\n" +
                        new String(fileBytes);

                // Store in cache
                cache.put(key, response);
                return response;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // If file is not found, return 404
        return "HTTP/1.1 404 Not Found\r\nContent-Length: 0\r\n\r\n";
    }

    private static String extractPath(String request) {
        String[] lines = request.split("\r\n");
        if (lines.length > 0) {
            String[] parts = lines[0].split(" ");
            if (parts.length > 1) {
                return parts[1];
            }
        }
        return "/";
    }

    private static String getContentType(String fileName) {
        if (fileName.endsWith(".html")) return "text/html";
        if (fileName.endsWith(".css")) return "text/css";
        if (fileName.endsWith(".js")) return "application/javascript";
        if (fileName.endsWith(".png")) return "image/png";
        if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) return "image/jpeg";
        return "text/plain";
    }
}
