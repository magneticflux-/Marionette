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
import org.jnsgaii.population.individual.Individual;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.WindowConstants;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static com.marionette.evolver.supermariobros.Run.pressThreshold;
import static com.marionette.evolver.supermariobros.Run.visionSize;

public final class Playback {

    private Playback() {
    }

    @SuppressWarnings("MagicNumber")
    private static void startPlayback(NEATGenome genome) throws InterruptedException {
        final AtomicReference<BufferedImage> image = new AtomicReference<>(new BufferedImage(256, 224, BufferedImage.TYPE_INT_ARGB));

        JFrame frame = new JFrame();
        frame.add(new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (image.get() != null)
                    g.drawImage(image.get(), 0, 0, image.get().getWidth() * 4, image.get().getHeight() * 4, this);
            }

            @Override
            public Dimension getPreferredSize() {
                return new Dimension(image.get().getWidth() * 4, image.get().getHeight() * 4);
            }
        });
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);

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

            Run.computeVision(cpuram, marioX, marioY, vision);

            double[] visionUnwound = RunDemo.NESFitness.unwind2DArray(vision);
            double[] reactions = network.stepTime(visionUnwound, 5);

            if (reactions[0] > pressThreshold) controller1.pressButton(PuppetController.Button.UP);
            if (reactions[1] > pressThreshold) controller1.pressButton(PuppetController.Button.DOWN);
            if (reactions[2] > pressThreshold) controller1.pressButton(PuppetController.Button.LEFT);
            if (reactions[3] > pressThreshold) controller1.pressButton(PuppetController.Button.RIGHT);
            if (reactions[4] > pressThreshold) controller1.pressButton(PuppetController.Button.A);
            if (reactions[5] > pressThreshold) controller1.pressButton(PuppetController.Button.B);
            // if (reactions[6] > pressThreshold) input.keyPressed(SELECT);
            // if (reactions[7] > pressThreshold) input.keyPressed(START);

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

        Input in = new Input(new FileInputStream("generations/269.bin"));
        @SuppressWarnings("unchecked")
        PopulationData<NEATGenome> populationData = (PopulationData<NEATGenome>) kryo.readClassAndObject(in);
        in.close();

        List<FrontedIndividual<NEATGenome>> genomes = new ArrayList<>(populationData.getTruncatedPopulation().getPopulation());

        genomes.sort((o1, o2) -> {
            assert o1.getIndividual().marioBrosData != null;
            assert o2.getIndividual().marioBrosData != null;
            //return -Double.compare(o1.getIndividual().marioBrosData.getLastDistance(), o2.getIndividual().marioBrosData.getLastDistance());
            return -Double.compare(o1.getScore(1), o2.getScore(1));
        });

        FrontedIndividual<NEATGenome> individual = genomes.get(0);

        System.out.println(Arrays.toString(individual.getScores()));
        individual.getIndividual().marioBrosData.dataPoints.forEach(System.out::println);
        System.out.println(individual.getIndividual().marioBrosData.dataPoints.get(0).world);
        for (Individual<NEATGenome> i : genomes)
            startPlayback(i.getIndividual());
    }
}
