import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Server {

    public static void main(String[] args) {

        final int PORT = 12345;

        // This will store output streams of all connected clients
        List<PrintWriter> clientOutputStreams = new ArrayList<>();
        // This will store the latest status of all players
        Map<String, PlayerStatus> playersStatus = new HashMap<>();

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server is listening on port " + PORT);

            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("New client connected");

                // Add client output stream to list
                PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
                synchronized (clientOutputStreams) {
                    clientOutputStreams.add(writer);
                }

                // Send the latest player status to the new client
                synchronized (playersStatus) {
                    for (PlayerStatus playerStatus : playersStatus.values()) {
                        writer.println(playerStatus);
                    }
                }

                new Thread(() -> {
                    try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                        String inputLine;
                        while ((inputLine = in.readLine()) != null) {
                            System.out.println("Received from client: " + inputLine);

                            // Parsing the input line
                            String[] parts = inputLine.split(", ");
                            String playerName = parts[0].split(": ")[1];
                            int applesEaten = Integer.parseInt(parts[1].split(": ")[1]);
                            boolean started = Boolean.parseBoolean(parts[2].split(": ")[1]);

                            // Set the status based on the 'started' field
                            String status = started ? "alive" : "dead";

                            // Create the player status object
                            PlayerStatus playerStatus = new PlayerStatus(playerName, status, applesEaten);

                            // Update the player status in the map
                            synchronized (playersStatus) {
                                playersStatus.put(playerName, playerStatus);
                            }

                            // Broadcast the updated player status to all clients
                            synchronized (clientOutputStreams) {
                                for (PrintWriter writerTmp : clientOutputStreams) {
                                    writerTmp.println(playerStatus);
                                }
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        synchronized (clientOutputStreams) {
                            clientOutputStreams.remove(writer);
                        }
                        try {
                            socket.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    

    private static class PlayerStatus {
        private String playerName;
        private String status;
        private int applesEaten;

        public PlayerStatus(String playerName, String status, int applesEaten) {
            this.playerName = playerName;
            this.status = status;
            this.applesEaten = applesEaten;
        }

        @Override
        public String toString() {
            return playerName + ":" + status + ":" + applesEaten;
        }
    }
}
