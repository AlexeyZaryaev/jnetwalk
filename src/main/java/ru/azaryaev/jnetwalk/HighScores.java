package ru.azaryaev.jnetwalk;

import java.io.*;

/**
 * Class to load/save table of high scores records: array of records{name, time}.
 */
public class HighScores {

    public record Entry(String name, int time) {
    }

    private final Entry[] table;
    private final String hsFile;

    public HighScores(int levelMax, String hsFile) {
        this.table = new Entry[levelMax];
        this.hsFile = hsFile;
        for (int i = 0; i < levelMax; i++) {
            table[i] = new Entry(null, -1);
        }
    }

    public void load() {
        if (hsFile == null) return;

        try (BufferedReader br = new BufferedReader(new FileReader(hsFile))) {
            String line;
            int i = 0;
            while (i < table.length && (line = br.readLine()) != null) {
                int comma = line.indexOf(',');
                if (comma < 0) continue;
                String name = line.substring(0, comma);
                try {
                    table[i] = new Entry(name, Integer.parseInt(line.substring(comma + 1).trim()));
                } catch (NumberFormatException e) {
                    table[i] = new Entry(name, -1);
                }
                i++;
            }
        } catch (IOException e) {
        }
    }

    public void save() {
        if (hsFile == null) return;

        try (PrintWriter pw = new PrintWriter(new FileWriter(hsFile))) {
            for (Entry e : table) {
                pw.printf("%s,%d%n", e.name, e.time);
            }
        } catch (IOException e) {
            /* ignore */
        }
    }

    public String getDisplayName(int i) {
        Entry e = table[i];
        return (e != null && e.name != null) ? e.name : "None";
    }

    public String getDisplayTime(int i) {
        Entry e = table[i];
        return (e != null && e.time != -1) ? Integer.toString(e.time) : "None";
    }

    public boolean needsNewRecord(int i, int time) {
        Entry e = table[i];
        return e == null || e.time == -1 || time < e.time;
    }

    public void recordScore(int i, String name, int time) {
        table[i] = new Entry(name, time);
        save();
    }
}