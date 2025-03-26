import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.LinkedHashMap;
import java.util.Map;

public class RequestHandler implements Runnable {
    private final SocketChannel client;
    private final SelectionKey key;

    // Thread-safe LRU cache for storing responses
    private static final int CACHE_SIZE = 100;
    private static final Map<String, String> cache = new LinkedHashMap<>(CACHE_SIZE, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
            return size() > CACHE_SIZE;
        }
    };
    private static final Object cacheLock = new Object(); // Synchronization lock for cache

    public RequestHandler(SocketChannel client, SelectionKey key) {
        this.client = client;
        this.key = key;
    }

    @Override
    public void run() {
        try {
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            int bytesRead = client.read(buffer);

            if (bytesRead == -1) {
                client.close();
                return;
            }

            buffer.flip();
            String request = new String(buffer.array(), 0, bytesRead);
            System.out.println("Received request:\n" + request);

            // Extract method and path
            String[] requestLines = request.split("\r\n");
            String[] requestParts = requestLines[0].split(" ");

            if (requestParts.length < 2) {
                sendResponse(400, "Bad Request");
                return;
            }

            String method = requestParts[0];
            String path = requestParts[1];

            switch (method) {
                case "GET":
                    handleGet(path);
                    break;
                case "POST":
                    handlePost(path, request);
                    break;
                case "PUT":
                    handlePut(path, request);
                    break;
                case "DELETE":
                    handleDelete(path);
                    break;
                default:
                    sendResponse(405, "Method Not Allowed");
            }

            client.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** Handle GET Requests */
    private void handleGet(String path) throws IOException {
        synchronized (cacheLock) {
            if (cache.containsKey(path)) {
                sendResponse(200, cache.get(path));
                System.out.println("⚡ Served from cache: " + path);
            } else {
                String response = "<html><body><h1>Welcome</h1><p>Path: " + path + "</p></body></html>";
                cache.put(path, response);
                sendResponse(200, response);
            }
        }
    }

    /** Handle POST Requests */
    private void handlePost(String path, String request) throws IOException {
        String body = extractBody(request);
        synchronized (cacheLock) {
            cache.put(path, body);
        }
        sendResponse(201, "Resource Created at " + path);
    }

    /** Handle PUT Requests */
    private void handlePut(String path, String request) throws IOException {
        String body = extractBody(request);
        synchronized (cacheLock) {
            if (cache.containsKey(path)) {
                cache.put(path, body);
                sendResponse(200, "Resource Updated at " + path);
            } else {
                sendResponse(404, "Resource Not Found");
            }
        }
    }

    /** Handle DELETE Requests */
    private void handleDelete(String path) throws IOException {
        synchronized (cacheLock) {
            if (cache.containsKey(path)) {
                cache.remove(path);
                sendResponse(200, "Resource Deleted at " + path);
            } else {
                sendResponse(404, "Resource Not Found");
            }
        }
    }

    /** Extract Request Body */
    private String extractBody(String request) {
        int index = request.indexOf("\r\n\r\n");
        return (index != -1) ? request.substring(index + 4) : "";
    }

    /** Send HTTP Response */
    private void sendResponse(int statusCode, String body) throws IOException {
        String statusMessage = switch (statusCode) {
            case 200 -> "OK";
            case 201 -> "Created";
            case 400 -> "Bad Request";
            case 404 -> "Not Found";
            case 405 -> "Method Not Allowed";
            default -> "Internal Server Error";
        };

        String response = "HTTP/1.1 " + statusCode + " " + statusMessage + "\r\n" +
                "Content-Type: text/html\r\n" +
                "Content-Length: " + body.length() + "\r\n" +
                "Connection: close\r\n\r\n" + body;

        ByteBuffer responseBuffer = ByteBuffer.wrap(response.getBytes());
        client.write(responseBuffer);
    }
}
