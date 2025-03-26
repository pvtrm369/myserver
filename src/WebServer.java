import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.concurrent.*;

public class WebServer {

    private static final int PORT = 8080;
    private static final int CORE_POOL_SIZE = 10;  // Minimum number of threads
    private static final int KEEP_ALIVE_TIME = 60; // Seconds
    private static final int CACHE_SIZE = 100; // Max cache entries

    // **Concurrent LRU Cache with Async Handling**
    private static final ConcurrentHashMap<String, CompletableFuture<String>> cache = new ConcurrentHashMap<>();
    private static final LinkedBlockingQueue<String> evictionQueue = new LinkedBlockingQueue<>(CACHE_SIZE);

    public static void main(String[] args) {
        // **Dynamic thread pool for handling requests**
        ThreadPoolExecutor threadPool = new ThreadPoolExecutor(
                CORE_POOL_SIZE,
                Integer.MAX_VALUE,
                KEEP_ALIVE_TIME,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );

        try (Selector selector = Selector.open();
             ServerSocketChannel serverSocket = ServerSocketChannel.open()) {

            serverSocket.bind(new InetSocketAddress(PORT));
            serverSocket.configureBlocking(false);
            serverSocket.register(selector, SelectionKey.OP_ACCEPT);
            System.out.println("🚀 Server started on port " + PORT);

            while (true) {
                selector.select();
                Iterator<SelectionKey> keys = selector.selectedKeys().iterator();

                while (keys.hasNext()) {
                    SelectionKey key = keys.next();
                    keys.remove();

                    if (key.isAcceptable()) {
                        acceptConnection(serverSocket, selector);
                    } else if (key.isReadable()) {
                        key.interestOps(0);  // Prevent duplicate reads until handled
                        threadPool.execute(new RequestHandler((SocketChannel) key.channel(), key));
                    }
                }

                logThreadPoolStatus(threadPool);
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            threadPool.shutdown();
        }
    }

    private static void acceptConnection(ServerSocketChannel serverSocket, Selector selector) throws IOException {
        SocketChannel client = serverSocket.accept();
        client.configureBlocking(false);
        client.register(selector, SelectionKey.OP_READ);
        System.out.println("✅ Accepted new connection from " + client.getRemoteAddress());
    }

    private static void logThreadPoolStatus(ThreadPoolExecutor threadPool) {
        System.out.println("Active Threads: " + threadPool.getActiveCount() +
                ", Total Tasks: " + threadPool.getTaskCount() +
                ", Completed Tasks: " + threadPool.getCompletedTaskCount());
    }

    // **Optimized Request Handler**
    static class RequestHandler implements Runnable {
        private final SocketChannel client;
        private final SelectionKey key;

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
                String path = extractPath(request);

                // **Async Cache Lookup**
                CompletableFuture<String> cachedResponse = cache.computeIfAbsent(path, k -> CompletableFuture.supplyAsync(() -> {
                    String response = generateResponse(path);
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

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private String extractPath(String request) {
            String path = "/";
            String[] lines = request.split("\r\n");
            if (lines.length > 0 && lines[0].startsWith("GET")) {
                String[] parts = lines[0].split(" ");
                if (parts.length > 1) path = parts[1];
            }
            return path;
        }

        private void sendResponse(String response) throws IOException {
            ByteBuffer responseBuffer = ByteBuffer.wrap(response.getBytes());
            client.write(responseBuffer);
            client.close();
            key.interestOps(SelectionKey.OP_READ);
            key.selector().wakeup();
        }

        private String generateResponse(String path) {
            String body = "<html><body><h1>Welcome to Ultra-Fast Web Server</h1><p>Path: " + path + "</p></body></html>";
            return "HTTP/1.1 200 OK\r\n" +
                    "Content-Type: text/html\r\n" +
                    "Content-Length: " + body.length() + "\r\n" +
                    "Connection: close\r\n\r\n" + body;
        }

        private static void evictIfNeeded(String key) {
            evictionQueue.offer(key);
            if (evictionQueue.size() > CACHE_SIZE) {
                String evictKey = evictionQueue.poll();
                cache.remove(evictKey);
            }
        }
    }
}
