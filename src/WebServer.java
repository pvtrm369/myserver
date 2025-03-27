import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.concurrent.*;

public class WebServer {
    private static final int PORT = 8080;
    private static final int CORE_POOL_SIZE = 10;
    private static final int KEEP_ALIVE_TIME = 60;

    // Thread Pools
    private static final ExecutorService requestPool = new ThreadPoolExecutor(
            CORE_POOL_SIZE, Integer.MAX_VALUE, KEEP_ALIVE_TIME, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(), new ThreadPoolExecutor.CallerRunsPolicy()
    );

    private static final ExecutorService dbExecutor = Executors.newFixedThreadPool(5);  // Separate DB query executor

    public static void main(String[] args) {
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
                        key.interestOps(0);  // Temporarily disable until processed
                        requestPool.execute(new RequestRouter((SocketChannel) key.channel(), key, dbExecutor));
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            requestPool.shutdown();
            dbExecutor.shutdown();
        }
    }

    private static void acceptConnection(ServerSocketChannel serverSocket, Selector selector) throws IOException {
        SocketChannel client = serverSocket.accept();
        client.configureBlocking(false);
        client.register(selector, SelectionKey.OP_READ);
        System.out.println("✅ Accepted new connection from " + client.getRemoteAddress());
    }
}
