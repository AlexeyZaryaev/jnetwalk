package ru.azaryaev.jnetwalk;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * GameBoard: draw grid, tube, server, computer, rotate animation, win gif.
 */
public class Arena {

    public static BufferedImage screen;
    public static Graphics2D    screenG;

    public int x, y, w, h;

    static final int ROT_DURATION_MS = 150;
    record Rot(long start, int startAngle) {}
    Rot[][] rot = new Rot[Game.BOARD_MAX_W][Game.BOARD_MAX_H];

    BufferedImage[] tileImg = new BufferedImage[64];
    BufferedImage computerOff;
    BufferedImage computerOn;
    BufferedImage serverTop;
    BufferedImage serverBottom;
    BufferedImage bgUnmarked;
    BufferedImage bgMarked;
    BufferedImage fieldCache;
    BufferedImage rotBuffer;

    List<BufferedImage> winFrames = new ArrayList<>();
    long winStartTime = 0;

    public Arena() {
    }

    public void invalidateCache() {
        fieldCache = null;
    }

    void rebuildFieldCache() {
        fieldCache = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);

        Graphics2D savedG = screenG;
        int savedX = x, savedY = y;

        screenG = fieldCache.createGraphics();
        screenG.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        x = 0;
        y = 0;

        fill(0, 0, w, h, Color.C_INVTEXT);

        int bc = State.game.isWin() ? Color.C_BORDERWON : Color.C_BORDER;

        int gridX = State.PADDING;
        int gridY = State.PADDING;
        int gridW = State.cellW * State.game.boardW + (State.game.boardW + 1) * State.BORDER;
        int gridH = State.BORDER;
        for (int i = 0; i <= State.game.boardH; i++) {
            fill(gridX, gridY, gridW, gridH, bc);
            gridY += State.cellH + State.BORDER;
        }

        gridX = State.PADDING;
        gridY = State.PADDING;
        gridW = State.BORDER;
        gridH = State.cellH * State.game.boardH + (State.game.boardH + 1) * State.BORDER;
        for (int i = 0; i <= State.game.boardW; i++) {
            fill(gridX, gridY, gridW, gridH, bc);
            gridX += State.cellW + State.BORDER;
        }

        for (int i = 0; i < State.game.boardW; i++)
            for (int j = 0; j < State.game.boardH; j++)
                if (State.game.board[i][j] != 0)
                    drawTileStatic(i, j);

        screenG.dispose();
        screenG = savedG;
        x = savedX;
        y = savedY;
    }

    void drawTileStatic(int i, int j) {
        int marked = State.game.flags[i][j] & 0x1;
        int value  = State.game.board[i][j];
        int index  = value - 1;
        if (index < 0 || index >= 64) return;

        int px = x + State.PADDING + State.BORDER + i * (State.cellW + State.BORDER);
        int py = y + State.PADDING + State.BORDER + j * (State.cellH + State.BORDER);
        int pw = State.cellW;
        int ph = State.cellH;

        BufferedImage bg = (marked != 0) ? bgMarked : bgUnmarked;
        if (bg != null) blit(bg, px, py, pw, ph);
        else fill(px, py, pw, ph, marked != 0 ? Color.C_MARKEDBG : Color.C_UNMARKEDBG);

        BufferedImage img = tileImg[index];
        if (img != null) blit(img, px, py, pw, ph);

        boolean isServer       = (value & 32) != 0;
        boolean isSourceTop    = (i == State.game.sourceX && j == State.game.sourceYTop);
        boolean isSourceBottom = (i == State.game.sourceX && j == State.game.sourceYBottom);

        if (isSourceTop && serverTop != null) blit(serverTop, px, py, pw, ph);
        else if (isSourceBottom && serverBottom != null) blit(serverBottom, px, py, pw, ph);

        if (!isServer) {
            int dirs = value & 15;
            boolean isEnd = dirs == 1 || dirs == 2 || dirs == 4 || dirs == 8;
            if (isEnd) {
                BufferedImage comp = (value & 16) != 0 ? computerOn : computerOff;
                if (comp != null) blit(comp, px, py, pw, ph);
            }
        }
    }

    void drawHighlightCell(int col, int row, int coloridx) {
        int px = x + (State.cellW + State.BORDER) * col + State.PADDING;
        int py = y + (State.cellH + State.BORDER) * row + State.PADDING;
        int pw = State.cellW + 2 * State.BORDER;
        int ph = State.cellH + 2 * State.BORDER;
        int b  = State.BORDER;

        fill(px, py, pw, b,  coloridx);
        fill(px, py + ph - b, pw, b, coloridx);
        fill(px, py, b,  ph, coloridx);
        fill(px + pw - b, py, b, ph, coloridx);
    }

    void drawHighlight() {
        int[] col = new int[1], row = new int[1];
        if (getCellFromMousePosition(State.lastMouseX, State.lastMouseY, col, row) != 0) return;

        drawHighlightCell(col[0], row[0], Color.C_HIGHLIGHT);

        if (State.game.wrapFlag != 0) {
            if (col[0] == 0) drawHighlightCell(State.game.boardW - 1, row[0], Color.C_EDGEMATCH);
            else if (col[0] == State.game.boardW - 1) drawHighlightCell(0, row[0], Color.C_EDGEMATCH);
            if (row[0] == 0) drawHighlightCell(col[0], State.game.boardH - 1, Color.C_EDGEMATCH);
            else if (row[0] == State.game.boardH - 1) drawHighlightCell(col[0], 0, Color.C_EDGEMATCH);
        }
    }

    void drawRotatingTile(int i, int j) {
        int marked = State.game.flags[i][j] & 0x1;
        int value  = State.game.board[i][j];
        int index  = value - 1;
        if (index < 0 || index >= 64) return;

        int px = x + State.PADDING + State.BORDER + i * (State.cellW + State.BORDER);
        int py = y + State.PADDING + State.BORDER + j * (State.cellH + State.BORDER);
        int pw = State.cellW;
        int ph = State.cellH;

        BufferedImage bg = (marked != 0) ? bgMarked : bgUnmarked;
        if (bg != null) blit(bg, px, py, pw, ph);
        else fill(px, py, pw, ph, marked != 0 ? Color.C_MARKEDBG : Color.C_UNMARKEDBG);

        BufferedImage img = tileImg[index];
        if (img != null) {
            Rot r = rot[i][j];
            long elapsed = System.currentTimeMillis() - r.start();

            if (elapsed >= ROT_DURATION_MS) {
                rot[i][j] = null;
                blit(img, px, py, pw, ph);
            } else {
                float frac  = (float) elapsed / ROT_DURATION_MS;
                float ease  = 1f - (1f - frac) * (1f - frac);
                float angle = r.startAngle() * (1f - ease);
                drawRotated(img, px, py, angle);
            }
        }

        boolean isServer       = (value & 32) != 0;
        boolean isSourceTop    = (i == State.game.sourceX && j == State.game.sourceYTop);
        boolean isSourceBottom = (i == State.game.sourceX && j == State.game.sourceYBottom);

        if (isSourceTop && serverTop != null) blit(serverTop, px, py, pw, ph);
        else if (isSourceBottom && serverBottom != null) blit(serverBottom, px, py, pw, ph);

        if (!isServer) {
            int dirs = value & 15;
            boolean isEnd = dirs == 1 || dirs == 2 || dirs == 4 || dirs == 8;
            if (isEnd) {
                BufferedImage comp = (value & 16) != 0 ? computerOn : computerOff;
                if (comp != null) blit(comp, px, py, pw, ph);
            }
        }
    }

    public static void fill(int px, int py, int pw, int ph, int c) {
        if (screenG == null) return;
        screenG.setColor(Color.cTable[c]);
        screenG.fillRect(px, py, pw, ph);
    }

    public static void blit(BufferedImage img, int px, int py) {
        if (img == null || screenG == null) return;
        screenG.drawImage(img, px, py, null);
    }

    public static void blit(BufferedImage img, int px, int py, int pw, int ph) {
        if (img == null || screenG == null) return;
        screenG.drawImage(img,
                px, py, px + pw, py + ph,
                0, 0, img.getWidth(), img.getHeight(), null);
    }

    static BufferedImage loadPng(String path) {
        try (InputStream is = Arena.class.getResourceAsStream(path)) {
            return is == null ? null : ImageIO.read(is);
        } catch (IOException e) {
            return null;
        }
    }

    public void loadWinGif() {
        winFrames.clear();
        try (InputStream is = Arena.class.getResourceAsStream("/tiles/win.gif")) {
            if (is == null) return;

            ImageInputStream iis = ImageIO.createImageInputStream(is);
            Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName("gif");
            if (!readers.hasNext()) return;

            ImageReader reader = readers.next();
            reader.setInput(iis);
            int n = reader.getNumImages(true);
            for (int i = 0; i < n; i++) {
                winFrames.add(reader.read(i));
            }
            reader.dispose();
        } catch (Exception e) {
            System.err.println("Can't load win.gif: " + e.getMessage());
        }
    }

    static BufferedImage resize(BufferedImage src, int w, int h) {
        if (src == null) return null;
        if (src.getWidth() == w && src.getHeight() == h) return src;

        BufferedImage dst = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = dst.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g.drawImage(src, 0, 0, w, h, null);
        g.dispose();
        return dst;
    }

    static BufferedImage rotate90(BufferedImage src, int times) {
        for (int k = 0; k < times; k++) {
            int w = src.getWidth(), h = src.getHeight();
            BufferedImage dst = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = dst.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g.rotate(Math.PI / 2, w / 2.0, h / 2.0);
            g.drawImage(src, 0, 0, null);
            g.dispose();
            src = dst;
        }
        return src;
    }

    public void initTileImgBoth() {
        int cw = State.NORMAL_CELL_W;
        int ch = State.NORMAL_CELL_H;

        BufferedImage pipeEndOff = resize(loadPng("/tiles/pipe_end_off.png"), cw, ch);
        BufferedImage pipeEndOn  = resize(loadPng("/tiles/pipe_end_on.png"),  cw, ch);

        BufferedImage straightOff = resize(loadPng("/tiles/straight_off.png"), cw, ch);
        BufferedImage cornerOff   = resize(loadPng("/tiles/corner_off.png"),   cw, ch);
        BufferedImage teeOff      = resize(loadPng("/tiles/tee_off.png"),      cw, ch);
        BufferedImage crossOff    = resize(loadPng("/tiles/cross_off.png"),    cw, ch);

        BufferedImage straightOn  = resize(loadPng("/tiles/straight_on.png"),  cw, ch);
        BufferedImage cornerOn    = resize(loadPng("/tiles/corner_on.png"),    cw, ch);
        BufferedImage teeOn       = resize(loadPng("/tiles/tee_on.png"),       cw, ch);
        BufferedImage crossOn     = resize(loadPng("/tiles/cross_on.png"),     cw, ch);

        serverTop    = resize(loadPng("/tiles/server_top.png"),    cw, ch);
        serverBottom = resize(loadPng("/tiles/server_bottom.png"), cw, ch);

        bgUnmarked = resize(loadPng("/tiles/bg_unmarked.png"), cw, ch);
        bgMarked   = resize(loadPng("/tiles/bg_marked.png"),   cw, ch);

        computerOff = resize(loadPng("/tiles/computer_off.png"), cw, ch);
        computerOn  = resize(loadPng("/tiles/computer_on.png"),  cw, ch);

        tileImg[ 0] = pipeEndOff;                       // board  1  up
        tileImg[ 1] = rotate90(pipeEndOff, 1);          // board  2  right
        tileImg[ 2] = cornerOff;                        // board  3  up+right
        tileImg[ 3] = rotate90(pipeEndOff, 2);          // board  4  down
        tileImg[ 4] = straightOff;                      // board  5  up+down
        tileImg[ 5] = rotate90(cornerOff, 1);           // board  6  right+down
        tileImg[ 6] = teeOff;                           // board  7  u+r+d
        tileImg[ 7] = rotate90(pipeEndOff, 3);          // board  8  left
        tileImg[ 8] = rotate90(cornerOff, 3);           // board  9  up+left
        tileImg[ 9] = rotate90(straightOff, 1);         // board 10  right+left
        tileImg[10] = rotate90(teeOff, 3);              // board 11  u+r+l
        tileImg[11] = rotate90(cornerOff, 2);           // board 12  down+left
        tileImg[12] = rotate90(teeOff, 2);              // board 13  u+d+l
        tileImg[13] = rotate90(teeOff, 1);              // board 14  r+d+l
        tileImg[14] = crossOff;                         // board 15  all
        tileImg[15] = null;                             // board 16  invalid

        tileImg[16] = pipeEndOn;                        // board 17  up
        tileImg[17] = rotate90(pipeEndOn, 1);           // board 18  right
        tileImg[18] = cornerOn;                         // board 19  up+right
        tileImg[19] = rotate90(pipeEndOn, 2);           // board 20  down
        tileImg[20] = straightOn;                       // board 21  up+down
        tileImg[21] = rotate90(cornerOn, 1);            // board 22  right+down
        tileImg[22] = teeOn;                            // board 23  u+r+d
        tileImg[23] = rotate90(pipeEndOn, 3);           // board 24  left
        tileImg[24] = rotate90(cornerOn, 3);            // board 25  up+left
        tileImg[25] = rotate90(straightOn, 1);          // board 26  right+left
        tileImg[26] = rotate90(teeOn, 3);               // board 27  u+r+l
        tileImg[27] = rotate90(cornerOn, 2);            // board 28  down+left
        tileImg[28] = rotate90(teeOn, 2);               // board 29  u+d+l
        tileImg[29] = rotate90(teeOn, 1);               // board 30  r+d+l
        tileImg[30] = crossOn;                          // board 31  all
        tileImg[31] = null;                             // board 32  invalid

        for (int i = 0; i < 32; i++) tileImg[32 + i] = tileImg[i];

        loadWinGif();
        rotBuffer = null;
    }

    public void clearRotations() {
        for (int i = 0; i < rot.length; i++)
            for (int j = 0; j < rot[i].length; j++)
                rot[i][j] = null;
    }

    public void clearWinAnimation() {
        winStartTime = 0;
    }

    void startRotation(int i, int j, int newDeltaDeg) {
        int currentAngle = 0;
        Rot old = rot[i][j];
        if (old != null) {
            long elapsed = System.currentTimeMillis() - old.start();
            if (elapsed < ROT_DURATION_MS) {
                float frac = (float) elapsed / ROT_DURATION_MS;
                currentAngle = Math.round(old.startAngle() * (1f - frac));
            }
        }
        rot[i][j] = new Rot(System.currentTimeMillis(), currentAngle - newDeltaDeg);
    }

    void drawRotated(BufferedImage img, int px, int py, float angleDeg) {
        if (img == null || screenG == null) return;

        int cw = State.cellW;
        int ch = State.cellH;

        if (rotBuffer == null
                || rotBuffer.getWidth()  != cw
                || rotBuffer.getHeight() != ch) {
            rotBuffer = new BufferedImage(cw, ch, BufferedImage.TYPE_INT_ARGB);
        }

        Graphics2D tg = rotBuffer.createGraphics();
        tg.setComposite(java.awt.AlphaComposite.Clear);
        tg.fillRect(0, 0, cw, ch);
        tg.setComposite(java.awt.AlphaComposite.SrcOver);
        tg.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        tg.rotate(Math.toRadians(angleDeg), cw / 2.0, ch / 2.0);
        tg.drawImage(img, 0, 0, cw, ch, null);
        tg.dispose();

        screenG.drawImage(rotBuffer, px, py, null);
    }

    int getCellFromMousePosition(int mousex, int mousey, int[] colP, int[] rowP) {
        if (mousex < x || mousey < y) return -1;
        int col = (mousex - State.PADDING - State.BORDER - x) / (State.cellW + State.BORDER);
        int row = (mousey - State.PADDING - State.BORDER - y) / (State.cellH + State.BORDER);
        if (col >= State.game.boardW || row >= State.game.boardH) return -1;
        colP[0] = col;
        rowP[0] = row;
        return 0;
    }

    public void update() {
        if (fieldCache == null) rebuildFieldCache();

        blit(fieldCache, x, y);

        drawHighlight();

        for (int i = 0; i < State.game.boardW; i++)
            for (int j = 0; j < State.game.boardH; j++)
                if (rot[i][j] != null)
                    drawRotatingTile(i, j);

        if (State.game.isWin() && !winFrames.isEmpty()) {
            long now = System.currentTimeMillis();
            if (winStartTime == 0) winStartTime = now;

            int frameIdx = (int) ((now / 80) % winFrames.size());
            BufferedImage frame = winFrames.get(frameIdx);
            if (frame != null) {
                int bw = State.cellW * State.game.boardW + (State.game.boardW + 1) * State.BORDER;
                int bh = State.cellH * State.game.boardH + (State.game.boardH + 1) * State.BORDER;
                int px = x + State.PADDING + (bw - frame.getWidth())  / 2;
                int py = y + State.PADDING + (bh - frame.getHeight()) / 2;
                blit(frame, px, py);
            }
        } else {
            winStartTime = 0;
        }
    }

    public void handleClick(int button, int mx, int my) {
        if (State.state != State.STATE_GAME) return;

        int i = (mx - State.PADDING - State.BORDER - x) / (State.cellW + State.BORDER);
        int j = (my - State.PADDING - State.BORDER - y) / (State.cellH + State.BORDER);
        if (i < 0 || j < 0) return;
        if (i >= State.game.boardW || j >= State.game.boardH) return;

        if (State.game.isWin()) return;

        State.game.board[State.game.sourceX][State.game.sourceYBottom] |=
                State.game.board[State.game.sourceX][State.game.sourceYTop] & 1;
        if (i == State.game.sourceX && j == State.game.sourceYTop)
            j = State.game.sourceYBottom;

        int d = State.game.board[i][j] & 15;
        switch (button) {
            case MouseEvent.BUTTON1:
                d = State.game.rotate(d, 3);
                startRotation(i, j, -90);
                State.incrementMoveCount();
                Sound.playClick();
                break;
            case MouseEvent.BUTTON3:
                d = State.game.rotate(d, 1);
                startRotation(i, j, +90);
                State.incrementMoveCount();
                Sound.playClick();
                break;
        }
        State.game.board[i][j] &= ~15;
        State.game.board[i][j] += d;

        State.game.board[State.game.sourceX][State.game.sourceYTop] &= ~1;
        State.game.board[State.game.sourceX][State.game.sourceYTop] |=
                State.game.board[State.game.sourceX][State.game.sourceYBottom] & 1;
        State.game.board[State.game.sourceX][State.game.sourceYBottom] &= ~1;

        if (button == MouseEvent.BUTTON2)
            State.game.flags[i][j] ^= 0x1;

        State.game.checkLive();
        invalidateCache();
    }
}