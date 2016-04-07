package com.marionette.evolver.supermariobros;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.grapeshot.halfnes.CPURAM;
import com.grapeshot.halfnes.NES;
import com.grapeshot.halfnes.ui.HeadlessUI;
import com.grapeshot.halfnes.ui.PuppetController;
import org.apache.commons.math3.util.FastMath;
import org.javaneat.genome.NEATGenome;
import org.javaneat.phenome.NEATPhenome;
import org.jnsgaii.population.PopulationData;

import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.concurrent.atomic.AtomicReference;

public final class Playback {
    private static final int visionSize = 11;

    private Playback() {
    }

    @SuppressWarnings("MagicNumber")
    public static void main(String[] args) throws InterruptedException, FileNotFoundException {
        Kryo kryo = new Kryo();

        Input in = new Input(new FileInputStream("generations/532.bin"));
        @SuppressWarnings("unchecked")
        PopulationData<NEATGenome> populationData = (PopulationData<NEATGenome>) kryo.readClassAndObject(in);
        in.close();

        NEATGenome genome = populationData.getTruncatedPopulation().getPopulation().get(0).getIndividual();

        NEATPhenome network = new NEATPhenome(genome);

        final AtomicReference<BufferedImage> image = new AtomicReference<>(new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB));

        JFrame frame = new JFrame();
        frame.add(new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.drawImage(image.get(), 0, 0, this);
            }
        });

        HeadlessUI ui = new HeadlessUI("roms/Super Mario Bros..nes", true);
        NES nes = new NES(ui);
        PuppetController controller1 = (PuppetController) nes.getcontroller1();

        CPURAM cpuram;

        for (int i = 0; i < 31; i++)
            // Exact frame number until it can begin.
            ui.runFrame();

        controller1.pressButton(PuppetController.Button.START);
        ui.runFrame();
        controller1.releaseButton(PuppetController.Button.START);

        for (int i = 0; i < 162; i++)
            // Exact frame number until Mario gains control
            ui.runFrame();

        int maxDistance = 0;
        int timeout = 0;

        while (true) {
            Thread.sleep(10);
            controller1.resetButtons();

            cpuram = nes.getCPURAM();

            int score = 0;
            int time = 0;
            byte world = (byte) cpuram.read(0x075F);
            byte level = (byte) cpuram.read(0x0760);
            byte lives = (byte) (cpuram.read(0x075A) + 1);
            int marioX = cpuram.read(0x6D) * 0x100 + cpuram.read(0x86);
            int marioY = cpuram.read(0x03B8) + 16;
            int marioState = cpuram.read(0x000E);
            for (int i = 0x07DD; i <= 0x07E2; i++)
                score += cpuram._read(i) * FastMath.pow(10, (0x07E2 - i + 1));
            for (int i = 0x07F8; i <= 0x07FA; i++)
                time += cpuram._read(i) * FastMath.pow(10, (0x07FA - i));

            int points = ((time - 400) * 10) + (marioX / 2) + (level * 250) + (world * 2000);
            System.out.println(String.format("Points: %d, Time: %d, Score: %d, World: %d, Level: %d, Lives: %d, MarioX: %d, MarioY: %d"
                            + (nes.getCPURAM().read(0x000E) == 0x0B ? ", DYING" : ", STATE: " + nes.getCPURAM().read(0x000E)), points, time, score, world, level,
                    lives, marioX, marioY));

            timeout++;
            if (marioX > maxDistance) {
                maxDistance = marioX;
                timeout = 0;
            }
            // System.out.println("Lives: " + lives + " Timeout: " + timeout + " Distance: " + marioX);
            if (lives < 3 || timeout > 120 || marioState == 0x0B) {
                break;
            }

            // System.out.println("Timeout: " + timeout + ", Time: " + time);

            final int[][] vision = new int[visionSize][visionSize];

            for (int dx = -vision[0].length / 2; dx < vision[0].length / 2; dx += 1)
                for (int dy = -vision.length / 2; dy < vision.length / 2; dy += 1) {
                    int x = marioX + (dx * 16) + 8;
                    int y = marioY + (dy * 16) - 16;
                    int page = (int) FastMath.floor(x / 256) % 2;
                    int subx = (int) FastMath.floor((x % 256) / 16);
                    int suby = (int) FastMath.floor((y - 32) / 16);
                    int addr = 0x500 + page * visionSize * 16 + suby * 16 + subx;
                    if (suby >= 13 || suby < 0) {
                        // System.out.println("Outside level.");
                        vision[dy + (vision.length / 2)][dx + (vision[0].length / 2)] = 0;
                    } else {
                        // System.out.println("Block data at " + dx + ", " + dy + ": " + cpuram.read(addr));
                        vision[dy + (vision.length / 2)][dx + (vision[0].length / 2)] = cpuram.read(addr) == 0 ? 0 : 1;
                    }
                }

            for (int i = 0; i <= 4; i++) {
                int enemy = cpuram.read(0xF + i);
                if (enemy != 0) {
                    int ex = cpuram.read(0x6E + i) * 0x100 + cpuram.read(0x87 + i);
                    int ey = cpuram.read(0xCF + i) + 24;
                    int enemyMarioDeltaX = (ex - marioX) / 16;
                    int enemyMarioDeltaY = (ey - marioY) / 16;
                    try {
                        vision[enemyMarioDeltaY + (vision.length / 2)][enemyMarioDeltaX + (vision[0].length / 2)] = -1;
                    } catch (ArrayIndexOutOfBoundsException ignored) {
                    }
                }
            }
            double[] visionUnwound = unwind2DArray(vision);
            double[] reactions = network.stepTime(visionUnwound, 5);

            if (reactions[0] > 0) controller1.pressButton(PuppetController.Button.UP);
            if (reactions[1] > 0) controller1.pressButton(PuppetController.Button.DOWN);
            if (reactions[2] > 0) controller1.pressButton(PuppetController.Button.LEFT);
            if (reactions[3] > 0) controller1.pressButton(PuppetController.Button.RIGHT);
            if (reactions[4] > 0) controller1.pressButton(PuppetController.Button.A);
            if (reactions[5] > 0) controller1.pressButton(PuppetController.Button.B);
            ui.runFrame();
            image.set(ui.getLastFrame());
        }
    }

    private static double[] unwind2DArray(int[][] arr) {
        double[] out = new double[arr.length * arr[0].length];
        int i = 0;
        for (int x = 0; x < arr[0].length; x++) {
            for (int[] anArr : arr) {
                out[i] = anArr[x];
                i++;
            }
        }
        return out;
    }
}
