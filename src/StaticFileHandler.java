import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.*;

public class StaticFileHandler {
    private final SocketChannel client;
    private final SelectionKey key;
    private final String path;

    private static final String PUBLIC_DIR = "public";
    private static final int CACHE_SIZE = 100;

    private static final ConcurrentHashMap<String, CompletableFuture<String>> cache = new ConcurrentHashMap<>();
    private static final LinkedBlockingQueue<String> evictionQueue = new LinkedBlockingQueue<>(CACHE_SIZE);

    public StaticFileHandler(SocketChannel client, SelectionKey key, String path) {
        this.client = client;
        this.key = key;
        this.path = path;
    }

    public void handle() {
        CompletableFuture<String> cachedResponse = cache.computeIfAbsent(path, k -> CompletableFuture.supplyAsync(() -> {
            String response = serveStaticFile();
            evictIfNeeded(k);
            return response;
        }));

        cachedResponse.thenAccept(response -> {
            try {
                sendResponse(response);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private String serveStaticFile() {
        Path filePath = Paths.get(PUBLIC_DIR, path);
        if (Files.exists(filePath)) {
            try {
                byte[] fileBytes = Files.readAllBytes(filePath);
                String contentType = getContentType(path);
                return "HTTP/1.1 200 OK\r\n" +
                        "Content-Type: " + contentType + "\r\n" +
                        "Content-Length: " + fileBytes.length + "\r\n\r\n" +
                        new String(fileBytes);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return "HTTP/1.1 404 Not Found\r\nContent-Length: 0\r\n\r\n";
    }

    private void sendResponse(String response) throws IOException {
        ByteBuffer responseBuffer = ByteBuffer.wrap(response.getBytes());
        client.write(responseBuffer);
        client.close();
        key.selector().wakeup();
    }

    private static String getContentType(String fileName) {
        if (fileName.endsWith(".html")) return "text/html";
        if (fileName.endsWith(".css")) return "text/css";
        if (fileName.endsWith(".js")) return "application/javascript";
        return "text/plain";
    }

    private static void evictIfNeeded(String key) {
        evictionQueue.offer(key);
        if (evictionQueue.size() > CACHE_SIZE) {
            String evictKey = evictionQueue.poll();
            cache.remove(evictKey);
        }
    }
}
