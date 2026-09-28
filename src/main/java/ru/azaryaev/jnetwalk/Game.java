package ru.azaryaev.jnetwalk;

import java.util.Random;

public class Game {

    public static final int BOARD_MAX_W = 50;
    public static final int BOARD_MAX_H = 50;

    public int boardW = 10, boardH = 9;
    public int[][] board = new int[BOARD_MAX_W][BOARD_MAX_H];
    public int[][] neighbourCount = new int[BOARD_MAX_W][BOARD_MAX_H];
    public int[][] flags = new int[BOARD_MAX_W][BOARD_MAX_H];
    public int sourceX, sourceYTop, sourceYBottom;

    public int wrapFlag = 0;
    public int noFourway = 0;
    private boolean gameWin = false;

    public static final int[][] dir = {
            {0, -1},
            {1, 0},
            {0, 1},
            {-1, 0}
    };

    private final Random random;

    public Game() {
        this(new Random());
    }

    public Game(Random r) {
        this.random = r;
    }

    public int[] addDir(int x1, int y1, int d) {
        int[] out = new int[2];
        out[0] = x1 + dir[d][0];
        out[1] = y1 + dir[d][1];
        if (wrapFlag != 0) {
            if (out[0] < 0) out[0] = boardW - 1;
            if (out[0] >= boardW) out[0] = 0;
            if (out[1] < 0) out[1] = boardH - 1;
            if (out[1] >= boardH) out[1] = 0;
        }
        return out;
    }

    public void clearFlags() {
        for (int i = 0; i < boardW; i++)
            for (int j = 0; j < boardH; j++)
                flags[i][j] = 0;
    }

    public void generateMaze() {
        int[] openX = new int[BOARD_MAX_W * BOARD_MAX_H];
        int[] openY = new int[BOARD_MAX_W * BOARD_MAX_H];
        int n = 2;
        openX[0] = sourceX;
        openX[1] = sourceX;
        openY[0] = sourceYTop;
        openY[1] = sourceYBottom;

        for (int i = 0; i < boardW; i++)
            for (int j = 0; j < boardH; j++) {
                board[i][j] = 0;
                neighbourCount[i][j] = 0;
            }
        board[sourceX][sourceYTop] = 32;
        board[sourceX][sourceYBottom] = 32;

        while (n > 0) {
            int flag;
            int i = random.nextInt(n);
            int x = openX[i];
            int y = openY[i];
            flag = 1;

            if (x == sourceX && y == sourceYTop) {
                if (board[x][y - 1] == 0) flag = 0;
            } else {
                for (int j = 0; j < 4; j++) {
                    int[] xy = addDir(x, y, j);
                    int x1 = xy[0], y1 = xy[1];
                    if (x1 < 0 || x1 >= boardW || y1 < 0 || y1 >= boardH) continue;
                    if (board[x1][y1] == 0) {
                        flag = 0;
                        break;
                    }
                }
            }

            if (flag != 0) {
                n--;
                System.arraycopy(openX, i + 1, openX, i, n - i);
                System.arraycopy(openY, i + 1, openY, i, n - i);
                continue;
            }

            int j = random.nextInt(4);

            if (x == sourceX && y == sourceYTop) {
                if (j % 2 != 0) continue;
            }

            int[] xy = addDir(x, y, j);
            int x1 = xy[0], y1 = xy[1];

            if (x1 < 0 || x1 >= boardW || y1 < 0 || y1 >= boardH) continue;
            if (board[x1][y1] != 0) continue;
            if (noFourway != 0 && neighbourCount[x][y] >= 3) continue;
            neighbourCount[x][y]++;
            neighbourCount[x1][y1]++;

            board[x][y] |= (1 << j);
            board[x1][y1] |= (1 << ((j + 2) % 4));

            openX[n] = x1;
            openY[n] = y1;
            n++;
        }
    }

    public int rotate(int d, int n) {
        for (int i = 0; i < n; i++) {
            d = (d << 1);
            if (d >= 16) d -= 15;
        }
        return d;
    }

    public void scramble() {
        gameWin = false;
        board[sourceX][sourceYBottom] |= board[sourceX][sourceYTop] & 1;

        for (int i = 0; i < boardW; i++) {
            for (int j = 0; j < boardH; j++) {
                if (i == sourceX && j == sourceYTop) continue;
                if (board[i][j] != 0) {
                    int d = board[i][j] & 15;
                    int d1;
                    switch (random.nextInt(4)) {
                        case 1:
                            d1 = rotate(d, 1);
                            break;
                        case 2:
                            d1 = rotate(d, 2);
                            break;
                        case 3:
                            d1 = rotate(d, 3);
                            break;
                        default:
                            d1 = d;
                            break;
                    }
                    board[i][j] &= ~15;
                    board[i][j] += d1;
                }
            }
        }

        board[sourceX][sourceYTop] &= ~1;
        board[sourceX][sourceYTop] |= board[sourceX][sourceYBottom] & 1;
        board[sourceX][sourceYBottom] &= ~1;
    }

    public void checkLive() {
        int[] openX = new int[BOARD_MAX_W * BOARD_MAX_H];
        int[] openY = new int[BOARD_MAX_W * BOARD_MAX_H];
        int tilecount = 0;
        int livecount;
        int n = 2;

        openX[0] = sourceX;
        openX[1] = sourceX;
        openY[0] = sourceYTop;
        openY[1] = sourceYBottom;

        for (int i = 0; i < boardW; i++)
            for (int j = 0; j < boardH; j++) {
                if (board[i][j] != 0) tilecount++;
                board[i][j] &= ~16;
            }
        board[sourceX][sourceYTop] |= 16;
        board[sourceX][sourceYBottom] |= 16;
        livecount = 2;

        while (n > 0) {
            n--;
            int x = openX[n];
            int y = openY[n];

            for (int j = 0; j < 4; j++) {
                if ((board[x][y] & (1 << j)) != 0) {
                    int[] xy = addDir(x, y, j);
                    int x1 = xy[0], y1 = xy[1];
                    if (x1 < 0 || x1 >= boardW || y1 < 0 || y1 >= boardH) continue;

                    int i = board[x1][y1];
                    if ((i & (1 << ((j + 2) % 4))) != 0) {
                        if ((i & 16) == 0) {
                            board[x1][y1] |= 16;
                            livecount++;
                            openX[n] = x1;
                            openY[n] = y1;
                            n++;
                        }
                    }
                }
            }
        }
        if (livecount == tilecount) gameWin = true;
    }

    public boolean isWin(){
        return gameWin;
    }
}