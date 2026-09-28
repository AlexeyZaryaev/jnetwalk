package ru.azaryaev.jnetwalk;

public class Color {

    public static final int C_INVTEXT    = 0;
    public static final int C_BORDER     = 1;
    public static final int C_BORDERWON  = 2;
    public static final int C_HIGHLIGHT  = 3;
    public static final int C_EDGEMATCH  = 4;
    public static final int C_UNMARKEDBG = 5;
    public static final int C_MARKEDBG   = 6;
    public static final int C_MAX        = 7;

    public static java.awt.Color[] cTable = new java.awt.Color[C_MAX];

    public static void initCtable() {
        cTable[C_INVTEXT]    = new java.awt.Color(  0,   0,   0);
        cTable[C_BORDER]     = new java.awt.Color(  0, 127,   0);
        cTable[C_BORDERWON]  = new java.awt.Color(  0, 127, 127);
        cTable[C_HIGHLIGHT]  = new java.awt.Color(180, 255, 180);
        cTable[C_EDGEMATCH]  = new java.awt.Color(127, 127, 127);
        cTable[C_UNMARKEDBG] = new java.awt.Color(  0,   0,   0);
        cTable[C_MARKEDBG]   = new java.awt.Color(  0,   0, 127);
    }
}