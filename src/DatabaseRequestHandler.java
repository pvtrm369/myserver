import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.concurrent.*;

@SuppressWarnings("ALL")
public class DatabaseRequestHandler {
    private final SocketChannel client;
    private final SelectionKey key;
    private final String name;
    private final ExecutorService dbExecutor;

    private static final String DB_URL = "postgresql://postgres:mdQkANEfquCNhYcQRbkpebqqbGLjnsCo@postgres.railway.internal:5432/railway";
    private static final String DB_USER = "postgres";
    private static final String DB_PASSWORD = "mdQkANEfquCNhYcQRbkpebqqbGLjnsCo";

    private static final ConcurrentHashMap<String, String> queryCache = new ConcurrentHashMap<>();

    public DatabaseRequestHandler(SocketChannel client, SelectionKey key, String name, ExecutorService dbExecutor) {
        this.client = client;
        this.key = key;
        this.name = name;
        this.dbExecutor = dbExecutor;
    }

    public void handle() {
        CompletableFuture.supplyAsync(this::queryDatabase, dbExecutor)
                .thenAccept(result -> {
                    queryCache.putIfAbsent(name, result);
                    sendResponse(result);
                });
    }

    private String queryDatabase() {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement stmt = conn.prepareStatement("SELECT email FROM users WHERE name = ?")) {

            stmt.setString(1, name);
            ResultSet rs = stmt.executeQuery();
            return rs.next() ? "Email: " + rs.getString("email") : "User not found";

        } catch (Exception e) {
            e.printStackTrace();
            return "Database error";
        }
    }

    private void sendResponse(String body) {
        try {
            ByteBuffer responseBuffer = ByteBuffer.wrap(("HTTP/1.1 200 OK\r\nContent-Length: " + body.length() + "\r\n\r\n" + body).getBytes());
            client.write(responseBuffer);
            client.close();
            key.selector().wakeup();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
