import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

public class Sudoku extends JPanel {
    static final int CELL = 60, GRID = CELL * 9, TOP = 50, SIDE = 200, PAD = 20;
    static final Color BG = new Color(24, 24, 34), BOARD = new Color(250, 248, 240),
        GIVEN = new Color(30, 30, 40), USER = new Color(40, 90, 200), WRONG = new Color(210, 40, 40),
        SEL = new Color(255, 225, 120), PEER = new Color(232, 236, 250), SAME = new Color(200, 220, 255);

    int[][] solution = new int[9][9], puzzle = new int[9][9], board = new int[9][9];
    boolean[][][] notes = new boolean[9][9][10];
    int selR = 0, selC = 0, mistakes = 0, seconds = 0;
    boolean notesMode = false, won = false, paused = false;
    String difficulty = "Medium";
    Random rnd = new Random();
    javax.swing.Timer clock = new javax.swing.Timer(1000, e -> { if (!won && !paused) { seconds++; repaint(); } });

    public Sudoku() {
        setPreferredSize(new Dimension(PAD * 2 + GRID + SIDE, TOP + GRID + PAD * 2));
        setBackground(BG);
        setFocusable(true);
        addKeyListener(new KeyAdapter() { public void keyPressed(KeyEvent e) { key(e); } });
        addMouseListener(new MouseAdapter() { public void mousePressed(MouseEvent e) { click(e.getX(), e.getY()); } });
        newGame("Medium");
        clock.start();
    }

    // ---------- puzzle generation ----------
    void newGame(String diff) {
        difficulty = diff;
        for (int[] r : solution) Arrays.fill(r, 0);
        fill(solution, 0);
        for (int r = 0; r < 9; r++) puzzle[r] = solution[r].clone();
        int remove = diff.equals("Easy") ? 36 : diff.equals("Medium") ? 46 : 54;
        java.util.List<Integer> cells = new ArrayList<>();
        for (int i = 0; i < 81; i++) cells.add(i);
        Collections.shuffle(cells, rnd);
        for (int idx : cells) {
            if (remove == 0) break;
            int r = idx / 9, c = idx % 9, keep = puzzle[r][c];
            puzzle[r][c] = 0;
            if (countSolutions(copy(puzzle), 0, 2) != 1) puzzle[r][c] = keep; else remove--;
        }
        for (int r = 0; r < 9; r++) board[r] = puzzle[r].clone();
        notes = new boolean[9][9][10];
        mistakes = 0; seconds = 0; won = false; paused = false; selR = selC = 0;
        repaint();
    }

    boolean fill(int[][] g, int pos) {
        if (pos == 81) return true;
        int r = pos / 9, c = pos % 9;
        java.util.List<Integer> nums = new ArrayList<>(Arrays.asList(1,2,3,4,5,6,7,8,9));
        Collections.shuffle(nums, rnd);
        for (int n : nums) if (ok(g, r, c, n)) { g[r][c] = n; if (fill(g, pos + 1)) return true; g[r][c] = 0; }
        return false;
    }

    int countSolutions(int[][] g, int pos, int limit) {
        while (pos < 81 && g[pos / 9][pos % 9] != 0) pos++;
        if (pos == 81) return 1;
        int r = pos / 9, c = pos % 9, count = 0;
        for (int n = 1; n <= 9 && count < limit; n++)
            if (ok(g, r, c, n)) { g[r][c] = n; count += countSolutions(g, pos + 1, limit - count); g[r][c] = 0; }
        return count;
    }

    static boolean ok(int[][] g, int r, int c, int n) {
        for (int i = 0; i < 9; i++) if (g[r][i] == n || g[i][c] == n) return false;
        int br = r / 3 * 3, bc = c / 3 * 3;
        for (int i = 0; i < 3; i++) for (int j = 0; j < 3; j++) if (g[br + i][bc + j] == n) return false;
        return true;
    }

    static int[][] copy(int[][] g) { int[][] c = new int[9][]; for (int i = 0; i < 9; i++) c[i] = g[i].clone(); return c; }

    // ---------- input ----------
    void click(int x, int y) {
        requestFocusInWindow();
        int gx = x - PAD, gy = y - TOP;
        if (gx >= 0 && gy >= 0 && gx < GRID && gy < GRID) { selR = gy / CELL; selC = gx / CELL; repaint(); return; }
        int sx = PAD + GRID + 20;
        // side buttons
        String[] btns = {"Easy", "Medium", "Hard", "Hint", "Notes", "Solve"};
        for (int i = 0; i < btns.length; i++) {
            int by = TOP + 125 + i * 42;
            if (x >= sx && x <= sx + 160 && y >= by && y <= by + 36) { button(btns[i]); return; }
        }
        // number pad
        for (int n = 1; n <= 9; n++) {
            int bx = sx + ((n - 1) % 3) * 54, by = TOP + GRID - 160 + ((n - 1) / 3) * 54;
            if (x >= bx && x <= bx + 48 && y >= by && y <= by + 48) { enter(n); return; }
        }
    }

    void button(String b) {
        switch (b) {
            case "Easy": case "Medium": case "Hard": newGame(b); break;
            case "Hint": hint(); break;
            case "Notes": notesMode = !notesMode; break;
            case "Solve": for (int r = 0; r < 9; r++) board[r] = solution[r].clone(); won = true; break;
        }
        repaint();
    }

    void hint() {
        if (won) return;
        if (puzzle[selR][selC] == 0 && board[selR][selC] != solution[selR][selC]) { board[selR][selC] = solution[selR][selC]; }
        else for (int r = 0; r < 9; r++) for (int c = 0; c < 9; c++)
            if (board[r][c] != solution[r][c]) { board[r][c] = solution[r][c]; selR = r; selC = c; checkWin(); return; }
        checkWin();
    }

    void key(KeyEvent e) {
        int k = e.getKeyCode();
        if (k == KeyEvent.VK_P) { paused = !paused; repaint(); return; }
        if (paused) return;
        if (k == KeyEvent.VK_UP) selR = (selR + 8) % 9;
        else if (k == KeyEvent.VK_DOWN) selR = (selR + 1) % 9;
        else if (k == KeyEvent.VK_LEFT) selC = (selC + 8) % 9;
        else if (k == KeyEvent.VK_RIGHT) selC = (selC + 1) % 9;
        else if (k >= KeyEvent.VK_1 && k <= KeyEvent.VK_9) enter(k - KeyEvent.VK_0);
        else if (k >= KeyEvent.VK_NUMPAD1 && k <= KeyEvent.VK_NUMPAD9) enter(k - KeyEvent.VK_NUMPAD0);
        else if (k == KeyEvent.VK_BACK_SPACE || k == KeyEvent.VK_DELETE || k == KeyEvent.VK_0) erase();
        else if (k == KeyEvent.VK_N) notesMode = !notesMode;
        else if (k == KeyEvent.VK_H) hint();
        repaint();
    }

    void enter(int n) {
        if (won || paused || puzzle[selR][selC] != 0) return;
        if (notesMode) { if (board[selR][selC] == 0) notes[selR][selC][n] = !notes[selR][selC][n]; }
        else {
            board[selR][selC] = n;
            notes[selR][selC] = new boolean[10];
            if (n != solution[selR][selC]) mistakes++;
            else clearNotes(selR, selC, n);
            checkWin();
        }
        repaint();
    }

    void clearNotes(int r, int c, int n) {
        for (int i = 0; i < 9; i++) { notes[r][i][n] = false; notes[i][c][n] = false; }
        int br = r / 3 * 3, bc = c / 3 * 3;
        for (int i = 0; i < 3; i++) for (int j = 0; j < 3; j++) notes[br + i][bc + j][n] = false;
    }

    void erase() { if (puzzle[selR][selC] == 0) { board[selR][selC] = 0; notes[selR][selC] = new boolean[10]; } }

    void checkWin() {
        for (int r = 0; r < 9; r++) for (int c = 0; c < 9; c++) if (board[r][c] != solution[r][c]) return;
        won = true;
    }

    // ---------- drawing ----------
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        g.setColor(Color.WHITE); g.setFont(new Font("SansSerif", Font.BOLD, 22));
        g.drawString("SUDOKU", PAD, 33);
        g.setFont(new Font("SansSerif", Font.PLAIN, 15));
        g.setColor(new Color(180, 180, 200));
        g.drawString(difficulty + "   ·   Mistakes: " + mistakes + "   ·   " + String.format("%02d:%02d", seconds / 60, seconds % 60), PAD + 120, 32);

        int selVal = board[selR][selC];
        for (int r = 0; r < 9; r++) for (int c = 0; c < 9; c++) {
            int x = PAD + c * CELL, y = TOP + r * CELL;
            Color bg = BOARD;
            if (r == selR || c == selC || (r / 3 == selR / 3 && c / 3 == selC / 3)) bg = PEER;
            if (selVal != 0 && board[r][c] == selVal) bg = SAME;
            if (r == selR && c == selC) bg = SEL;
            g.setColor(bg); g.fillRect(x, y, CELL, CELL);
            int v = board[r][c];
            if (paused) continue;
            if (v != 0) {
                g.setFont(new Font("SansSerif", puzzle[r][c] != 0 ? Font.BOLD : Font.PLAIN, 30));
                g.setColor(puzzle[r][c] != 0 ? GIVEN : v == solution[r][c] ? USER : WRONG);
                String s = String.valueOf(v);
                FontMetrics fm = g.getFontMetrics();
                g.drawString(s, x + (CELL - fm.stringWidth(s)) / 2, y + (CELL + fm.getAscent()) / 2 - 4);
            } else {
                g.setFont(new Font("SansSerif", Font.PLAIN, 13)); g.setColor(new Color(110, 110, 130));
                for (int n = 1; n <= 9; n++) if (notes[r][c][n])
                    g.drawString(String.valueOf(n), x + 7 + ((n - 1) % 3) * 18, y + 16 + ((n - 1) / 3) * 18);
            }
        }
        // grid lines
        for (int i = 0; i <= 9; i++) {
            g.setColor(i % 3 == 0 ? new Color(40, 40, 50) : new Color(190, 190, 200));
            g.setStroke(new BasicStroke(i % 3 == 0 ? 3f : 1f));
            g.drawLine(PAD + i * CELL, TOP, PAD + i * CELL, TOP + GRID);
            g.drawLine(PAD, TOP + i * CELL, PAD + GRID, TOP + i * CELL);
        }
        g.setStroke(new BasicStroke(1f));

        // side panel
        int sx = PAD + GRID + 20;
        g.setFont(new Font("SansSerif", Font.PLAIN, 13)); g.setColor(new Color(180, 180, 200));
        String[] help = {"Click / arrows: select", "1-9: enter number", "Backspace: erase", "N: notes   H: hint", "P: pause"};
        for (int i = 0; i < help.length; i++) g.drawString(help[i], sx, TOP + 14 + i * 22);
        String[] btns = {"Easy", "Medium", "Hard", "Hint", "Notes", "Solve"};
        for (int i = 0; i < btns.length; i++) {
            int by = TOP + 125 + i * 42;
            boolean on = btns[i].equals(difficulty) || (btns[i].equals("Notes") && notesMode);
            g.setColor(on ? new Color(70, 110, 230) : new Color(50, 50, 68));
            g.fillRoundRect(sx, by, 160, 36, 10, 10);
            g.setColor(Color.WHITE); g.setFont(new Font("SansSerif", Font.BOLD, 15));
            String label = i < 3 ? "New: " + btns[i] : btns[i].equals("Notes") ? "Notes: " + (notesMode ? "ON" : "OFF") : btns[i];
            FontMetrics fm = g.getFontMetrics();
            g.drawString(label, sx + (160 - fm.stringWidth(label)) / 2, by + 24);
        }
        for (int n = 1; n <= 9; n++) {
            int bx = sx + ((n - 1) % 3) * 54, by = TOP + GRID - 160 + ((n - 1) / 3) * 54;
            g.setColor(new Color(50, 50, 68)); g.fillRoundRect(bx, by, 48, 48, 10, 10);
            g.setColor(Color.WHITE); g.setFont(new Font("SansSerif", Font.BOLD, 22));
            g.drawString(String.valueOf(n), bx + 18, by + 32);
        }

        if (won || paused) {
            g.setColor(new Color(0, 0, 0, 150)); g.fillRect(PAD, TOP, GRID, GRID);
            g.setColor(Color.WHITE); g.setFont(new Font("SansSerif", Font.BOLD, 40));
            String msg = won ? "SOLVED!" : "PAUSED";
            FontMetrics fm = g.getFontMetrics();
            g.drawString(msg, PAD + (GRID - fm.stringWidth(msg)) / 2, TOP + GRID / 2);
            if (won) {
                g.setFont(new Font("SansSerif", Font.PLAIN, 17)); fm = g.getFontMetrics();
                String sub = String.format("Time %02d:%02d  ·  Mistakes %d  ·  Pick a new game", seconds / 60, seconds % 60, mistakes);
                g.drawString(sub, PAD + (GRID - fm.stringWidth(sub)) / 2, TOP + GRID / 2 + 34);
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("Sudoku");
            Sudoku s = new Sudoku();
            f.add(s); f.pack(); f.setResizable(false);
            f.setLocationRelativeTo(null);
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            f.setVisible(true);
            s.requestFocusInWindow();
        });
    }
}
