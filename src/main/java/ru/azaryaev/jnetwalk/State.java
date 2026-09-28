package ru.azaryaev.jnetwalk;

public final class State {

    public static final int STATE_GAME = 0;
    public static final int STATE_QUIT = 2;

    public static final int LEVEL_EASY = 0;
    public static final int LEVEL_MEDIUM = 1;
    public static final int LEVEL_HARD = 2;
    public static final int LEVEL_VERYHARD = 3;
    public static final int LEVEL_GIANT = 4;
    public static final int LEVEL_ABSURD = 5;
    public static final int LEVEL_MAX = 6;

    public static final int NORMAL_CELL_W = 64;
    public static final int NORMAL_CELL_H = 64;
    public static final int BIG_BOARD_CELL_W = 32;
    public static final int BIG_BOARD_CELL_H = 32;

    public static int cellW = NORMAL_CELL_W;
    public static int cellH = NORMAL_CELL_H;
    public static final int BORDER = 1;
    public static final int PADDING = 10;

    public static HighScores highScores;
    public static Game game = new Game();

    public static final String[] levelName = {
            "Newbie",
            "Normal",
            "Nerd",
            "Nutcase",
            "Nonsense",
            "No Sleep"
    };
    public static int level = LEVEL_MEDIUM;

    public static int state;

    public static int lastMouseX, lastMouseY;
    public static int tick, tickOld;
    public static int moveCount, msCount, secondCount;

    public static void resetMoveCount() {
        moveCount = 0;
    }

    public static void incrementMoveCount() {
        moveCount++;
    }

    public static void resetTime() {
        msCount = 0;
        secondCount = 0;
    }

    public static void updateTime() {
        msCount += tick - tickOld;
        while (msCount >= 1000) {
            msCount -= 1000;
            secondCount++;
        }
    }

    public static Runnable onHsCheck;

}