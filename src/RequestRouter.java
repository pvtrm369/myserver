import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;

public class RequestRouter implements Runnable {
    private final SocketChannel client;
    private final SelectionKey key;
    private final ExecutorService dbExecutor;

    public RequestRouter(SocketChannel client, SelectionKey key, ExecutorService dbExecutor) {
        this.client = client;
        this.key = key;
        this.dbExecutor = dbExecutor;
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

            if (path.startsWith("/query?name=")) {
                String name = path.split("=")[1];
                new DatabaseRequestHandler(client, key, name, dbExecutor).handle();
            } else {
                new StaticFileHandler(client, key, path).handle();
            }

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
        return path.equals("/") ? "/index.html" : path;
    }
}
