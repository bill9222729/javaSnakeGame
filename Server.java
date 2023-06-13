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

import javax.swing.DefaultListModel;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;

import javax.swing.JButton;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

public class Server {

    // 新增一個JFrame變數
    private static JFrame frame;
    // 新增一個JList變數
    private static JList<String> playerList;
    // 新增一個DefaultListModel來管理JList中的內容
    private static DefaultListModel<String> listModel;
    private static List<PrintWriter> clientOutputStreams = new ArrayList<>();

    public static void main(String[] args) {

        final int PORT = 12345;

        // 儲存所有玩家的最新狀態
        Map<String, PlayerStatus> playersStatus = new HashMap<>();

        // 呼叫建立GUI的方法
        createGUI();

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("伺服器正在監聽連接埠 " + PORT);

            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("新的客戶端已連接");

                // 將客戶端的輸出流加入列表
                PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
                synchronized (clientOutputStreams) {
                    clientOutputStreams.add(writer);
                }

                // 將最新的玩家狀態發送給新的客戶端
                synchronized (playersStatus) {
                    for (PlayerStatus playerStatus : playersStatus.values()) {
                        writer.println(playerStatus);
                    }
                }

                new Thread(() -> {
                    try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                        String inputLine;
                        while ((inputLine = in.readLine()) != null) {
                            System.out.println("從客戶端接收到訊息: " + inputLine);

                            // 解析接收到的訊息
                            String[] parts = inputLine.split(", ");
                            String playerName = parts[0].split(": ")[1];
                            int applesEaten = Integer.parseInt(parts[1].split(": ")[1]);
                            boolean started = Boolean.parseBoolean(parts[2].split(": ")[1]);

                            // 根據 'started' 欄位設定狀態
                            String status = started ? "存活" : "死亡";

                            // 建立玩家狀態對象
                            PlayerStatus playerStatus = new PlayerStatus(playerName, status, applesEaten);

                            // 更新玩家狀態對應
                            synchronized (playersStatus) {
                                playersStatus.put(playerName, playerStatus);
                            }

                            // 更新玩家列表
                            updatePlayerList(playersStatus);

                            // 將更新後的玩家狀態廣播給所有客戶端
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

    private static void updatePlayerList(Map<String, PlayerStatus> playersStatus) {
        // 清空列表
        listModel.clear();

        // 新增新的玩家狀態到列表
        for (PlayerStatus status : playersStatus.values()) {
            listModel.addElement(status.toString());
        }
    }

    private static void createGUI() {
        // 建立一個新的JFrame
        frame = new JFrame("伺服器");
        frame.setSize(300, 500);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // 設定佈局
        frame.setLayout(new BorderLayout());

        // 建立一個新的DefaultListModel
        listModel = new DefaultListModel<>();

        // 建立一個新的JList並將其新增到JFrame
        playerList = new JList<>(listModel);
        frame.add(new JScrollPane(playerList), BorderLayout.CENTER);

        // 建立一個新的按鈕並設定其標籤為 "開始遊戲"
        JButton startGameButton = new JButton("開始遊戲");
        startGameButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // 在這裡向所有已連接的客戶端傳送開始遊戲的訊號
                broadcastMessage("START_GAME");
            }
        });

        // 將按鈕新增到JFrame的底部
        frame.add(startGameButton, BorderLayout.SOUTH);

        // 顯示JFrame
        frame.setVisible(true);
    }

    private static void broadcastMessage(String message) {
        synchronized (clientOutputStreams) {
            for (PrintWriter writer : clientOutputStreams) {
                writer.println(message);
            }
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