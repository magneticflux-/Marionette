package com.marionette.evolver.supermariobros;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.grapeshot.halfnes.CPURAM;
import com.grapeshot.halfnes.ui.HeadlessUI;
import com.grapeshot.halfnes.ui.PuppetController;
import org.apache.commons.math3.util.FastMath;
import org.javaneat.evolution.RunDemo;
import org.javaneat.evolution.nsgaii.MarioBrosData;
import org.javaneat.genome.NEATGenome;
import org.javaneat.phenome.NEATPhenome;
import org.jnsgaii.multiobjective.population.FrontedIndividual;
import org.jnsgaii.population.PopulationData;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.WindowConstants;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public final class Playback {
    private static final int visionSize = 11;

    private Playback() {
    }

    @SuppressWarnings("MagicNumber")
    private static void startPlayback(NEATGenome genome) throws InterruptedException {
        final AtomicReference<BufferedImage> image = new AtomicReference<>(new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB));

        JFrame frame = new JFrame();
        frame.add(new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (image.get() != null)
                    g.drawImage(image.get(), 0, 0, image.get().getWidth() * 4, image.get().getHeight() * 4, this);
            }
        });
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setVisible(true);
        frame.setSize(1100, 1000);

        final NEATPhenome network = new NEATPhenome(genome);
        HeadlessUI ui = new HeadlessUI("roms/Super Mario Bros..nes", true);
        ui.getNes().reset();

        CPURAM cpuram;
        PuppetController controller1 = ui.getController1();

        boolean goesRight = genome.getConnectionGeneList().stream().anyMatch(connectionGene -> connectionGene.getToNode() == genome.getManager().getOutputOffset() + 3);

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
        int currentFrame = 0;

        MarioBrosData data = new MarioBrosData();

        while (true) {
            long startTimeMS = System.currentTimeMillis();
            controller1.resetButtons();

            cpuram = ui.getNESCPURAM();

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

            if (currentFrame % 30 == 0) {
                data.addDataPoint(new MarioBrosData.DataPoint(score, time, world, level, lives, marioX, marioY, marioState));
            }

            currentFrame++;
            if (marioState == 8)
                timeout++;
            if (marioX > maxDistance) {
                maxDistance = marioX;
                timeout = 0;
            }
            if (lives < 3 || timeout > 240 || marioState == 0x0B || !goesRight) {
                //System.out.println(lives + " " + timeout + " " + marioState + " " + goesRight + " " + marioX);
                break;
            }

            final int[][] vision = new int[visionSize][visionSize];

            for (int dx = -vision[0].length / 2; dx < vision[0].length / 2; dx += 1)
                for (int dy = -vision.length / 2; dy < vision.length / 2; dy += 1) {
                    int x = marioX + (dx * 16) + 8;
                    int y = marioY + (dy * 16) - 16;
                    int page = (int) FastMath.floor(x / 256) % 2;
                    int subx = (int) FastMath.floor((x % 256) / 16);
                    int suby = (int) FastMath.floor((y - 32) / 16);
                    int addr = 0x500 + page * visionSize * 16 + suby * 16 + subx;
                    if (suby >= visionSize || suby < 0) {
                        // System.out.println("Outside level.");
                        vision[dy + (vision.length / 2)][dx + (vision[0].length / 2)] = 0;
                    } else {
                        // System.out.println("Block data at " + dx + ", " + dy + ": " + nes.cpuram.read(addr));
                        vision[dy + (vision.length / 2)][dx + (vision[0].length / 2)] = cpuram.read(addr);
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
                        vision[enemyMarioDeltaY + (vision.length / 2)][enemyMarioDeltaX + (vision[0].length / 2)] = -enemy;
                    } catch (ArrayIndexOutOfBoundsException ignored) {
                    }
                }
            }

            double[] visionUnwound = RunDemo.NESFitness.unwind2DArray(vision);
            double[] reactions = network.stepTime(visionUnwound, 5);

            if (reactions[0] > 0) controller1.pressButton(PuppetController.Button.UP);
            if (reactions[1] > 0) controller1.pressButton(PuppetController.Button.DOWN);
            if (reactions[2] > 0) controller1.pressButton(PuppetController.Button.LEFT);
            if (reactions[3] > 0) controller1.pressButton(PuppetController.Button.RIGHT);
            if (reactions[4] > 0) controller1.pressButton(PuppetController.Button.A);
            if (reactions[5] > 0) controller1.pressButton(PuppetController.Button.B);
            // if (reactions[6] > 0) input.keyPressed(SELECT);
            // if (reactions[7] > 0) input.keyPressed(START);

            ui.runFrame();
            image.set(ui.getLastFrame());
            frame.repaint();

            long elapsedTimeMS = System.currentTimeMillis() - startTimeMS;
            if (16 - elapsedTimeMS > 0)
                Thread.sleep(16 - elapsedTimeMS);
        }
        Thread.sleep(2000);
        frame.dispose();
    }

    @SuppressWarnings("MagicNumber")
    public static void main(String[] args) throws FileNotFoundException, InterruptedException {
        Kryo kryo = new Kryo();

        Input in = new Input(new FileInputStream("generations/906.bin"));
        @SuppressWarnings("unchecked")
        PopulationData<NEATGenome> populationData = (PopulationData<NEATGenome>) kryo.readClassAndObject(in);
        in.close();

        List<FrontedIndividual<NEATGenome>> genomes = new ArrayList<>(populationData.getTruncatedPopulation().getPopulation());
        genomes.sort((o1, o2) -> -Double.compare(o1.getScore(1), o2.getScore(1)));

        FrontedIndividual<NEATGenome> individual = genomes.get(0);

        System.out.println(Arrays.toString(individual.getScores()));
        individual.getIndividual().marioBrosData.dataPoints.forEach(System.out::println);

        startPlayback(individual.getIndividual());
    }
}
