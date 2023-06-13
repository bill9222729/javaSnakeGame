import java.io.*;
import java.net.*;

public class ClientHandler extends Thread {

    private Server server;
    private Socket socket;
    
    public ClientHandler(Server server, Socket socket) {
        this.server = server;
        this.socket = socket;
    }
    
    public void run() {
        try {
            ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
            while (true) {
                Object obj = ois.readObject();
                // handle incoming data and update game state
                // broadcast the new state with server.broadcast(newState);
            }
        } catch (Exception e) {
            // Handle exception
        }
    }
}
