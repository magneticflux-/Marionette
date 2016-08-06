package com.marionette.evolver.supermariobros.optimizationfunctions;

import com.grapeshot.halfnes.CPURAM;
import com.grapeshot.halfnes.PrefsSingleton;
import com.grapeshot.halfnes.ui.HeadlessUI;
import com.grapeshot.halfnes.ui.PuppetController;
import org.apache.commons.math3.util.FastMath;
import org.javaneat.genome.NEATGenome;
import org.javaneat.phenome.NEATPhenome;
import org.jnsgaii.computations.DefaultComputation;
import org.jnsgaii.population.individual.Individual;
import org.jnsgaii.properties.Properties;

/**
 * Created by Mitchell on 6/5/2016.
 */
public class SMBComputation extends DefaultComputation<NEATGenome, MarioBrosData> {
    public static final String ID = "SMBComputation";
    public static final int VISION_SIZE = 11;
    public static final double PRESS_THRESHOLD = .5;

    /**
     * Turns a 2d array into a longer 1d array
     *
     * @param arr A square 2d array
     * @return A 1d array with each 2d row concatenated
     */
    public static double[] unwind2DArray(double[][] arr) {
        double[] out = new double[arr.length * arr[0].length];
        int currentOutIndex = 0;
        for (double[] anArr : arr) {
            System.arraycopy(anArr, 0, out, currentOutIndex, anArr.length);
            currentOutIndex += anArr.length;
        }
        return out;
    }

    private static void simulationLoop(HeadlessUI ui, PuppetController controller1, MarioBrosData data, NEATPhenome network) {
        CPURAM cpuram = ui.getNESCPURAM();
        int maxDistance = 0;
        int timeout = 0;
        int currentFrame = 0;
        boolean goesRight = network.getConnectionList().stream()
                .anyMatch(neatConnection -> neatConnection.getToIndex() == 1 + network.getNumInputs() + 3);
        while (true) {
            controller1.resetButtons();

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
                data.addDataPoint(new MarioBrosData.DataPoint(score, marioX, marioY, marioState));
            }

            currentFrame++;
            if (marioState == 8)
                timeout++;
            if (marioX > maxDistance) {
                maxDistance = marioX;
                timeout = 0;
            }
            if (lives < 3 || timeout > 240 || marioState == 0x0B || !goesRight || marioY >= 208) {
                //System.out.println(lives + " " + timeout + " " + marioState + " " + goesRight + " " + marioX);
                break;
            }

            final double[][] vision = new double[VISION_SIZE][VISION_SIZE];

            computeVision(cpuram, marioX, marioY, vision);

            double[] visionUnwound = unwind2DArray(vision);
            double[] reactions = network.stepTime(visionUnwound, 5);

            if (reactions[0] > PRESS_THRESHOLD) controller1.pressButton(PuppetController.Button.UP);
            if (reactions[1] > PRESS_THRESHOLD) controller1.pressButton(PuppetController.Button.DOWN);
            if (reactions[2] > PRESS_THRESHOLD) controller1.pressButton(PuppetController.Button.LEFT);
            if (reactions[3] > PRESS_THRESHOLD) controller1.pressButton(PuppetController.Button.RIGHT);
            if (reactions[4] > PRESS_THRESHOLD) controller1.pressButton(PuppetController.Button.A);
            if (reactions[5] > PRESS_THRESHOLD) controller1.pressButton(PuppetController.Button.B);

            ui.runFrame();
        }

    }

    public static void computeVision(CPURAM cpuram, int marioX, int marioY, double[][] vision) {
        for (int dx = -vision[0].length / 2; dx < vision[0].length / 2; dx += 1)
            for (int dy = -vision.length / 2; dy < vision.length / 2; dy += 1) {
                int x = marioX + (dx * 16) + 8;
                int y = marioY + (dy * 16) - 16;
                int page = (int) FastMath.floor(x / 256) % 2;
                int subx = (int) FastMath.floor((x % 256) / 16);
                int suby = (int) FastMath.floor((y - 32) / 16);
                int addr = 0x500 + page * VISION_SIZE * 16 + suby * 16 + subx;
                if (suby >= VISION_SIZE || suby < 0) {
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
    }

    private static MarioBrosData computeFitnessPortable(NEATGenome candidate) {
        final NEATPhenome network = new NEATPhenome(candidate);

        PrefsSingleton.get().putBoolean("soundEnable", false);
        PrefsSingleton.get().putBoolean("Sleep", false);
        final HeadlessUI ui = new HeadlessUI(SMBComputation.class.getClassLoader().getResource("roms/Super Mario Bros..nes"), false);

        PuppetController controller1 = ui.getController1();

        for (int i = 0; i < 31; i++)
            // Exact frame number until it can begin.
            ui.runFrame();

        controller1.pressButton(PuppetController.Button.START);
        ui.runFrame();
        controller1.releaseButton(PuppetController.Button.START);

        for (int i = 0; i < 162; i++)
            // Exact frame number until Mario gains control
            ui.runFrame();

        MarioBrosData data = new MarioBrosData();

        simulationLoop(ui, controller1, data, network);

        if (data.dataPoints.size() < 2)
            data.dataPoints.add(new MarioBrosData.DataPoint(data.dataPoints.get(0)));

        return data;
    }

    @Override
    public MarioBrosData computeIndividual(Individual<NEATGenome> individual, Properties properties) {
        return computeFitnessPortable(individual.getIndividual());
    }

    @Override
    public String getComputationID() {
        return ID;
    }

    @Override
    public boolean isDeterministic() {
        return true;
    }
}
