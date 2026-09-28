package ru.azaryaev.jnetwalk;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.nio.file.Path;

public class Main {

    static Arena arena = new Arena();

    static JFrame frame;
    static JPanel panel;
    static JLabel sbMoves;
    static JLabel sbTime;

    static int lastMoveCount   = -1;
    static int lastSecondCount = -1;

    static void setVideo(int w, int h) {
        if (Arena.screen == null || Arena.screen.getWidth() != w
                || Arena.screen.getHeight() != h) {
            Arena.screen = new BufferedImage(Math.max(w, 1), Math.max(h, 1),
                    BufferedImage.TYPE_INT_ARGB);
            Arena.screenG = Arena.screen.createGraphics();
            Arena.screenG.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            Arena.screenG.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        }
        if (panel != null) panel.setPreferredSize(new Dimension(w, h));
        if (frame != null) frame.pack();
    }

    static void resize() {
        State.game.sourceX = State.game.boardW / 2 - 1;
        State.game.sourceYTop = State.game.boardH / 2;
        State.game.sourceYBottom = State.game.sourceYTop + 1;

        int w = State.cellW * State.game.boardW
                + (State.game.boardW + 1) * State.BORDER + 2 * State.PADDING;
        int h = State.cellH * State.game.boardH
                + (State.game.boardH + 1) * State.BORDER + 2 * State.PADDING;

        arena.x = 0;
        arena.y = 0;
        arena.w = w;
        arena.h = h;
        setVideo(w, h);
        arena.invalidateCache();
    }

    static void newGame() {
        State.cellW = State.NORMAL_CELL_W;
        State.cellH = State.NORMAL_CELL_H;
        switch (State.level) {
            case State.LEVEL_EASY:
                State.game.boardW = 5;
                State.game.boardH = 5;
                State.game.wrapFlag = 0;
                State.game.noFourway = 0;
                break;
            case State.LEVEL_MEDIUM:
                State.game.boardW = 10;
                State.game.boardH = 9;
                State.game.wrapFlag = 0;
                State.game.noFourway = 0;
                break;
            case State.LEVEL_HARD:
                State.game.boardW = 10;
                State.game.boardH = 9;
                State.game.wrapFlag = 1;
                State.game.noFourway = 1;
                break;
            case State.LEVEL_VERYHARD:
                State.game.boardW = 20;
                State.game.boardH = 18;
                State.game.wrapFlag = 1;
                State.game.noFourway = 1;
                break;
            case State.LEVEL_GIANT:
                State.game.boardW = 30;
                State.game.boardH = 30;
                State.game.wrapFlag = 0;
                State.game.noFourway = 1;
                State.cellW = State.BIG_BOARD_CELL_W;
                State.cellH = State.BIG_BOARD_CELL_H;
                break;
            case State.LEVEL_ABSURD:
                State.game.boardW = 50;
                State.game.boardH = 30;
                State.game.wrapFlag = 1;
                State.game.noFourway = 1;
                State.cellW = State.BIG_BOARD_CELL_W;
                State.cellH = State.BIG_BOARD_CELL_H;
                break;
        }
        resize();
        State.game.generateMaze();
        State.game.clearFlags();
        State.game.scramble();
        State.game.checkLive();
        State.resetMoveCount();
        arena.clearRotations();
        arena.clearWinAnimation();
        State.resetTime();
        State.msCount = 0;
        State.tick = (int) System.currentTimeMillis();
        State.secondCount = 0;
        arena.invalidateCache();
    }

    static void handleClick(int button, int x, int y) {
        arena.handleClick(button, x, y);
        if (State.game.isWin()) checkHs();
    }

    static void quit() {
        State.state = State.STATE_QUIT;
    }

    static void handleKey(int key, int mod) {
        switch (key) {
            case KeyEvent.VK_D:      enterNameOpen(); break;
            case KeyEvent.VK_ESCAPE:
            case KeyEvent.VK_Q:      quit(); break;
            case KeyEvent.VK_F2:     newGame(); break;
        }
    }

    static void updateScreen() {
        Arena.fill(0, 0, Arena.screen.getWidth(), Arena.screen.getHeight(),
                Color.C_INVTEXT);
        arena.update();
    }

    static void aboutOpen() {
        JOptionPane.showMessageDialog(
                frame,
                "JNetWalk 0.0.1\nZaryaev Alexey",
                "About",
                JOptionPane.INFORMATION_MESSAGE);
    }

    static void hsOpen() {
        JPanel p = new JPanel(new GridLayout(State.LEVEL_MAX + 1, 3, 8, 4));

        p.add(new JLabel("Level"));
        p.add(new JLabel("Time"));
        p.add(new JLabel("Name"));

        for (int i = 0; i < State.LEVEL_MAX; i++) {
            p.add(new JLabel(State.levelName[i]));
            p.add(new JLabel(State.highScores.getDisplayTime(i)));
            p.add(new JLabel(State.highScores.getDisplayName(i)));
        }

        JOptionPane.showMessageDialog(
                frame, p, "High Scores", JOptionPane.PLAIN_MESSAGE);
    }

    static void enterNameOpen() {
        String name = JOptionPane.showInputDialog(
                frame,
                "Enter name:",
                "New High Score",
                JOptionPane.PLAIN_MESSAGE);

        if (name == null) return;
        name = name.trim();
        if (name.isEmpty()) return;

        State.highScores.recordScore(State.level, name, State.secondCount);
    }

    static void checkHs() {
        if (State.highScores.needsNewRecord(State.level, State.secondCount))
            enterNameOpen();
    }

    static void buildMenuBar() {
        JMenuBar mb = new JMenuBar();

        JMenu game = new JMenu("Game");

        JMenuItem newGameItem = new JMenuItem("New game");
        newGameItem.addActionListener(e -> newGame());
        game.add(newGameItem);

        game.addSeparator();

        for (int i = 0; i < State.LEVEL_MAX; i++) {
            JMenuItem lvlItem = new JMenuItem(State.levelName[i]);
            final int lvl = i;
            lvlItem.addActionListener(e -> {
                State.level = lvl;
                newGame();
            });
            game.add(lvlItem);
        }

        game.addSeparator();

        JMenuItem hsItem = new JMenuItem("High Scores");
        hsItem.addActionListener(e -> hsOpen());
        game.add(hsItem);

        game.addSeparator();

        JMenuItem quitItem = new JMenuItem("Quit");
        quitItem.addActionListener(e -> quit());
        game.add(quitItem);

        JMenu help = new JMenu("Help");

        JMenuItem aboutItem = new JMenuItem("About");
        aboutItem.addActionListener(e -> aboutOpen());
        help.add(aboutItem);

        mb.add(game);
        mb.add(help);

        frame.setJMenuBar(mb);
    }

    static Path netwalkDir() {
        String home = System.getenv("HOME");
        if (home == null) home = System.getenv("USERPROFILE");
        if (home == null) home = System.getProperty("user.home");
        if (home == null) throw new IllegalStateException("$HOME is not set");
        return java.nio.file.Paths.get(home, ".jnetwalk");
    }

    static String hsFile() {
        return netwalkDir().resolve("hiscores.txt").toString();
    }

    public static void main(String[] args) {
        Sound.load();
        Color.initCtable();

        State.highScores = new HighScores(State.LEVEL_MAX, hsFile());
        State.highScores.load();

        frame = new JFrame("JNetWalk");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        sbMoves = new JLabel("moves: 0");
        sbTime  = new JLabel("time: 0");

        JPanel sb = new JPanel(new BorderLayout());
        sb.setBorder(BorderFactory.createLoweredBevelBorder());
        sb.add(sbMoves, BorderLayout.WEST);
        sb.add(sbTime,  BorderLayout.EAST);
        frame.add(sb, BorderLayout.SOUTH);

        panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (Arena.screen != null) g.drawImage(Arena.screen, 0, 0, null);
            }
        };
        panel.setBackground(java.awt.Color.BLACK);
        panel.setFocusable(true);
        panel.setPreferredSize(new Dimension(100, 100));
        frame.add(panel, BorderLayout.CENTER);

        panel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                State.lastMouseX = e.getX();
                State.lastMouseY = e.getY();
                handleClick(e.getButton(), e.getX(), e.getY());
            }
        });
        panel.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                State.lastMouseX = e.getX();
                State.lastMouseY = e.getY();
            }
            @Override
            public void mouseDragged(MouseEvent e) {
                State.lastMouseX = e.getX();
                State.lastMouseY = e.getY();
            }
        });
        panel.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                handleKey(e.getKeyCode(), e.getModifiersEx());
            }
        });

        buildMenuBar();

        arena.initTileImgBoth();

        resize();
        newGame();

        frame.setVisible(true);

        State.tickOld = (int) System.currentTimeMillis();
        State.tick    = State.tickOld;

        Timer timer = new Timer(20, e -> {
            if (State.state == State.STATE_QUIT) {
                ((Timer) e.getSource()).stop();
                frame.dispose();
                return;
            }

            State.tickOld = State.tick;
            State.tick = (int) System.currentTimeMillis();
            if (!State.game.isWin()) State.updateTime();

            if (State.moveCount != lastMoveCount) {
                lastMoveCount = State.moveCount;
                sbMoves.setText("moves: " + State.moveCount);
            }

            if (State.secondCount != lastSecondCount) {
                lastSecondCount = State.secondCount;
                sbTime.setText("time: " + State.secondCount);
            }

            updateScreen();
            panel.repaint();
        });
        timer.start();
    }
}