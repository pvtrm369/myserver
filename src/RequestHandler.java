import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

public class RequestHandler implements Runnable {
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
            System.out.println("Received request:\n" + request);

            // Extract the requested file/path
            String response = ResponseGenerator.generateResponse(request);

            ByteBuffer responseBuffer = ByteBuffer.wrap(response.getBytes());
            client.write(responseBuffer);
            client.close(); // Close after response to keep it simple
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
