public class ClientGameInfo {
  private String playerName;
  private int applesEaten;
  private boolean started;

  public ClientGameInfo(String playerName, int applesEaten, boolean started) {
      this.playerName = playerName;
      this.applesEaten = applesEaten;
      this.started = started;
  }

  public String getPlayerName() {
      return playerName;
  }

  public int getApplesEaten() {
      return applesEaten;
  }

  public boolean isStarted() {
      return started;
  }

  @Override
  public String toString() {
      return "ClientGameInfo{" +
              "playerName='" + playerName + '\'' +
              ", applesEaten=" + applesEaten +
              ", started=" + started +
              '}';
  }
}
