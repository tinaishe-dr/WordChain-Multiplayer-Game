import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.*;
import wordchain.client.RoomState;
import wordchain.client.ui.*;
import wordchain.common.*;

public class Snap {
    static void layout(Component c) {
        if (c instanceof Container k) { k.doLayout(); for (Component ch : k.getComponents()) layout(ch); }
    }
    static void shot(JComponent p, int w, int h, String file) throws Exception {
        p.setSize(w, h); layout(p); layout(p);
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics(); p.print(g); g.dispose();
        ImageIO.write(img, "png", new java.io.File(file));
    }
    public static void main(String[] a) throws Exception {
        System.setProperty("java.awt.headless", "true");
        SwingUtilities.invokeAndWait(() -> {
            try {
                Theme.install();
                RoomState room = new RoomState("BAXG", "Ana", "PLAYING", 4, 20, 3, "",
                    List.of(new RoomState.Player("Ana", 3, 124, 1), new RoomState.Player("Ben", 2, 98, 2),
                            new RoomState.Player("Cy", 0, 40, 2)));
                GamePanel gp = new GamePanel(m -> {});
                gp.onRoom(room, "Ana");
                gp.onGameStart("talk");
                gp.onAccepted("Ben", "talk", "walk", 0, 34);
                gp.onTimeout("Cy", 0); gp.onEliminated("Cy");
                gp.onTurn("Ana", 20, "walk", 3);
                gp.onHint(3);
                gp.chat().addChat("Ben", "nice one!", false); gp.chat().addSystem("Cy left the game");
                shot(gp, 1180, 720, "/tmp/game.png");
                LobbyPanel lp = new LobbyPanel(m -> {});
                lp.update(new RoomState("BAXG", "Ana", "LOBBY", 4, 20, 3, "talk", room.players()), "Ana");
                shot(lp, 1180, 720, "/tmp/lobby.png");
                shot(new ConnectPanel((n, h, p, x) -> {}), 1180, 720, "/tmp/connect.png");
                MenuPanel mp = new MenuPanel(new MenuPanel.Handler() { public void create() {} public void join(String c) {} public void disconnect() {} });
                mp.setInfo("Connected to localhost:5050", "Hosting on port 5050 - friends join at 192.168.1.20");
                shot(mp, 1180, 760, "/tmp/menu.png");
            } catch (Exception e) { e.printStackTrace(); }
        });
        System.exit(0);
    }
}
