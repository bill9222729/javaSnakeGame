import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.Timer;

@SuppressWarnings("serial")
public class GamePanel extends JPanel implements ActionListener, MouseListener {

	static final int EDGE_MARGIN = 20;
	static final int SCREEN_WIDTH = 1200;
	static final int SCREEN_HEIGHT = 800;
	static final int GAME_AREA_SIZE = 750 - (2 * EDGE_MARGIN);
	static final int UNIT_SIZE = 25;
	static final int GAME_UNITS = (SCREEN_WIDTH * SCREEN_HEIGHT) / UNIT_SIZE;

	static final int DELAY = 55;

	private boolean paused = false; // 新增暂停标志变量

	final int x[] = new int[GAME_UNITS];
	final int y[] = new int[GAME_UNITS];
	int bodyParts = 3;
	int applesEaten;
	int appleX, appleY;

	char direction = 'R';
	boolean running = false;
	boolean started = false;
	boolean noClick = true;
	Timer timer;
	Random random;
	static boolean gameOn = false;

	private Map<String, String> otherPlayersStatus = new ConcurrentHashMap<>();

	private ScheduledExecutorService scheduledExecutorService;
	private String playerName;
	private Socket socket;
	private PrintWriter out;

	public void sendStatusToServer() {
		if (out != null) {
			out.println("playerName: " + playerName + ", applesEaten: " + applesEaten + ", started: " + started);
		}
	}

	private void listenForServerMessages() {
		new Thread(() -> {
			try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
				String messageFromServer;
				while ((messageFromServer = in.readLine()) != null) {
					String[] parts = messageFromServer.split(":");
					if (parts.length == 3) {
						String playerName = parts[0];
						String status = parts[1].split("\\(")[0]; // 从状态中去除括号部分
						String score = parts[2];
						otherPlayersStatus.put(playerName, "(" + status + "): " + score); // 调整格式并放入 map 中
						repaint(); // 重新绘制游戏面板以更新其他玩家信息
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}).start();
	}

	public GamePanel() {
		try {
			socket = new Socket("localhost", 12345);
			out = new PrintWriter(socket.getOutputStream(), true);
		} catch (UnknownHostException e) {
			System.err.println("Don't know about host localhost");
		} catch (IOException e) {
			System.err.println("Couldn't get I/O for the connection to localhost");
		}

		playerName = JOptionPane.showInputDialog("請輸入您的名字：");

		random = new Random();
		this.setPreferredSize(new Dimension(SCREEN_WIDTH, SCREEN_HEIGHT));
		this.setBackground(Color.BLACK);
		this.setFocusable(true);
		this.addKeyListener(new MyKeyAdapter());
		this.addMouseListener(this);
		listenForServerMessages();
		startGame();
	}

	public void startGame() {
		sendStatusToServer();
		if (started && noClick) {
			x[0] = (GAME_AREA_SIZE / 2 / UNIT_SIZE) * UNIT_SIZE + EDGE_MARGIN;
			y[0] = (GAME_AREA_SIZE / 2 / UNIT_SIZE) * UNIT_SIZE + EDGE_MARGIN;
			for (int i = 1; i < bodyParts; i++) {
				x[i] = x[0] - i * UNIT_SIZE;
				y[i] = y[0];
			}

			newApple();
			running = true;

			scheduledExecutorService = Executors.newScheduledThreadPool(1);
			scheduledExecutorService.scheduleAtFixedRate(() -> {
				if (running) {
					move();
					checkApple();
					checkCollisions();
				}
				repaint();
			}, 0, DELAY, TimeUnit.MILLISECONDS);
			noClick = false;
		}
	}

	public void newApple() {
		int horizontalUnits = (GAME_AREA_SIZE / UNIT_SIZE);
		int verticalUnits = (GAME_AREA_SIZE / UNIT_SIZE);

		appleX = (random.nextInt(horizontalUnits) * UNIT_SIZE) + EDGE_MARGIN;
		appleY = (random.nextInt(verticalUnits) * UNIT_SIZE) + EDGE_MARGIN;
	}

	@Override
	public void actionPerformed(ActionEvent arg0) {
		if (running) {
			move();
			checkApple();
			checkCollisions();
		}
		repaint();
	}

	public class MyKeyAdapter extends KeyAdapter {
		@Override
		public void keyPressed(KeyEvent e) {
			switch (e.getKeyCode()) {
				case KeyEvent.VK_LEFT:
					if (direction != 'R') {
						direction = 'L';
					}
					break;

				case KeyEvent.VK_RIGHT:
					if (direction != 'L') {
						direction = 'R';
					}
					break;

				case KeyEvent.VK_UP:
					if (direction != 'D') {
						direction = 'U';
					}
					break;

				case KeyEvent.VK_DOWN:
					if (direction != 'U') {
						direction = 'D';
					}
					break;

				case KeyEvent.VK_SPACE:
					if (GamePanel.gameOn) {
						resume();
					} else if (running) {
						pause();
					}
					break;
			}
		}
	}

	public void move() {
		if (paused)
			return;
		for (int i = bodyParts; i > 0; i--) {
			x[i] = x[i - 1];
			y[i] = y[i - 1];
		}
		switch (direction) {
			case 'U':
				y[0] = y[0] - UNIT_SIZE;
				break;
			case 'D':
				y[0] = y[0] + UNIT_SIZE;
				break;
			case 'L':
				x[0] = x[0] - UNIT_SIZE;
				break;
			case 'R':
				x[0] = x[0] + UNIT_SIZE;
				break;
		}
	}

	public void checkApple() {
		if (paused)
			return;
		if ((x[0] == appleX) && (y[0] == appleY)) {
			bodyParts++;
			applesEaten++;
			newApple();
			sendStatusToServer();
		}
	}

	public void checkCollisions() {
		if (paused)
			return;
		for (int i = bodyParts; i > 0; i--) {
			if ((x[0] == x[i]) && (y[0] == y[i])) {
				running = false;
			}
		}
		if (x[0] < EDGE_MARGIN) {
			running = false;
		}
		if (x[0] > GAME_AREA_SIZE + EDGE_MARGIN - UNIT_SIZE) {
			running = false;
		}
		if (y[0] < EDGE_MARGIN) {
			running = false;
		}
		if (y[0] > GAME_AREA_SIZE + EDGE_MARGIN - UNIT_SIZE) {
			running = false;
		}

		if (!running) {
			scheduledExecutorService.shutdown();
		}
	}

	@Override
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		draw(g);
	}

	public void draw(Graphics g) {

		int centerX = EDGE_MARGIN + (GAME_AREA_SIZE / 2);
		int centerY = EDGE_MARGIN + (GAME_AREA_SIZE / 2);

		int scoreAreaX = GAME_AREA_SIZE + 2 * EDGE_MARGIN;
		int scoreAreaY = EDGE_MARGIN;
		int scoreAreaWidth = 350;
		int scoreAreaHeight = 710;
		g.setColor(Color.WHITE);
		g.drawRect(scoreAreaX, scoreAreaY, scoreAreaWidth, scoreAreaHeight);

		g.setColor(Color.RED);
		g.drawRect(EDGE_MARGIN, EDGE_MARGIN, GAME_AREA_SIZE, GAME_AREA_SIZE);

		if (running) {
			g.setColor(Color.RED);
			g.fillOval(appleX, appleY, UNIT_SIZE, UNIT_SIZE);

			for (int i = 0; i < bodyParts; i++) {
				if (i == 0) {
					g.setColor(Color.BLUE);
					g.fillRect(x[i], y[i], UNIT_SIZE, UNIT_SIZE);
				} else {
					g.setColor(Color.GREEN);
					g.fillRect(x[i], y[i], UNIT_SIZE, UNIT_SIZE);
				}
			}

			if (GamePanel.gameOn) {
				g.setFont(new Font("SAN_SERIF", Font.BOLD, 30));
				FontMetrics pauseMetrics = getFontMetrics(g.getFont());
				String pauseText = "遊戲暫停";
				int pauseTextX = (GAME_AREA_SIZE - pauseMetrics.stringWidth(pauseText)) / 2 + EDGE_MARGIN;
				int pauseTextY = (GAME_AREA_SIZE + pauseMetrics.getHeight()) / 2 + EDGE_MARGIN;
				g.drawString(pauseText, pauseTextX, pauseTextY);
			}
		} else {
			if (!started) {
				g.setColor(Color.RED);
				g.setFont(new Font("SAN_SERIF", Font.BOLD, 30));
				FontMetrics metrics = getFontMetrics(g.getFont());
				String startText = "[Click] 開始遊戲";
				g.drawString(startText, centerX - (metrics.stringWidth(startText) / 2), centerY - 20);
				String pauseText = "[Space] 暫停遊戲";
				g.drawString(pauseText, centerX - (metrics.stringWidth(pauseText) / 2), centerY + 30);
			} else {
				gameOver(g, centerX, centerY);
			}
		}
		// 玩家分數
		g.setColor(Color.CYAN);
		g.setFont(new Font("Ink Free", Font.BOLD, 30));
		FontMetrics metrics = getFontMetrics(g.getFont());
		int scoreTextX = scoreAreaX + (scoreAreaWidth - metrics.stringWidth(playerName + " 的分數: " + applesEaten)) / 2;
		int scoreTextY = scoreAreaY + metrics.getHeight() + EDGE_MARGIN;
		g.drawString(playerName + " 的分數: " + applesEaten, scoreTextX, scoreTextY);
		// 連線玩家分數
		g.setColor(Color.WHITE);
		g.setFont(new Font("Ink Free", Font.BOLD, 20));
		int yOffset = 30;
		for (Map.Entry<String, String> entry : otherPlayersStatus.entrySet()) {
			yOffset += 30;
			g.drawString(entry.getKey() + entry.getValue(), GAME_AREA_SIZE + EDGE_MARGIN * 3,
					yOffset + EDGE_MARGIN * 3);
		}
	}

	public void gameOver(Graphics g, int centerX, int centerY) {
		g.setColor(Color.RED);
		g.setFont(new Font("SAN_SERIF", Font.BOLD, 45));
		FontMetrics metrics1 = getFontMetrics(g.getFont());
		String scoreText = "您的分數為: " + applesEaten;
		g.drawString(scoreText, centerX - (metrics1.stringWidth(scoreText) / 2), centerY + 50);

		g.setColor(Color.RED);
		g.setFont(new Font("SAN_SERIF", Font.BOLD, 75));
		FontMetrics metrics = getFontMetrics(g.getFont());
		String gameOverText = "遊戲結束";
		g.drawString(gameOverText, centerX - (metrics.stringWidth(gameOverText) / 2), centerY);
		sendStatusToServer();
	}

	public void pause() {
		paused = true; // 设置暂停标志
		GamePanel.gameOn = true;
		repaint();
		// 停止 ScheduledExecutorService 的执行
		scheduledExecutorService.shutdown();
	}

	public void resume() {
		paused = false; // 清除暂停标志
		GamePanel.gameOn = false;
		scheduledExecutorService = Executors.newScheduledThreadPool(1);
		scheduledExecutorService.scheduleAtFixedRate(() -> {
			if (running) {
				move();
				checkApple();
				checkCollisions();
			}
			repaint();
		}, 0, DELAY, TimeUnit.MILLISECONDS);
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		started = true;
		startGame();
	}

	@Override
	public void mouseEntered(MouseEvent e) {
	}

	@Override
	public void mouseExited(MouseEvent e) {
	}

	@Override
	public void mousePressed(MouseEvent e) {
	}

	@Override
	public void mouseReleased(MouseEvent e) {
	}
}
