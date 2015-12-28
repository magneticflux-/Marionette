package com.marionette.evolver.supermariobros;

import com.grapeshot.halfnes.CPURAM;
import com.grapeshot.halfnes.NES;
import com.grapeshot.halfnes.ui.HeadlessUI;
import com.grapeshot.halfnes.ui.PuppetController;
import com.javaneat.genome.ConnectionGene;
import com.javaneat.genome.NEATGenome;
import com.javaneat.phenome.NEATPhenome;

import org.apache.commons.math3.util.FastMath;
import org.uncommons.watchmaker.framework.FitnessEvaluator;

import java.util.List;

public class SuperMarioBrosFitness implements FitnessEvaluator<NEATGenome> {
    private ThreadLocal<NES> nes = new ThreadLocal<>();
    private ThreadLocal<HeadlessUI> ui = new ThreadLocal<>();

    @SuppressWarnings("MagicNumber")
    @Override
    public double getFitness(NEATGenome candidate, List<? extends NEATGenome> population) {
        // long startTime = System.nanoTime();

        boolean movesRight = false; // Throw out ones that don't move right or jump before testing starts
        boolean jumps = false;
        for (ConnectionGene gene : candidate.getConnectionGeneList()) {
            if (gene.getToNode() == candidate.getManager().getOutputOffset() + 3) {
                movesRight = true;
                if (jumps) break;
            }
            if (gene.getToNode() == candidate.getManager().getOutputOffset() + 4) {
                jumps = true;
                if (movesRight) break;
            }
        }
        if (!(movesRight && jumps)) {
            // System.out.println("Candidate could not walk or jump. Terminating...");
            candidate.setScore(20 - candidate.getConnectionGeneList().size() * 2);
            return candidate.getScore();
        }

        NEATPhenome network = new NEATPhenome(candidate);
        if (nes.get() == null || ui.get() == null) {
            ui.set(new HeadlessUI("C:\\Users\\Mitchell\\Desktop\\fceux-2.2.2-win32\\ROMs\\Super Mario Bros..nes", false));
            nes.set(new NES(ui.get()));
        }
        NES nes = this.nes.get();
        HeadlessUI ui = this.ui.get();
        nes.reset();

        CPURAM cpuram;
        PuppetController controller1 = (PuppetController) nes.getcontroller1();

        for (int i = 0; i < 31; i++)
            // Exact frame number until it can begin.
            ui.runFrame();

        controller1.pressButton(PuppetController.Button.START);
        ui.runFrame();
        controller1.releaseButton(PuppetController.Button.START);

        for (int i = 0; i < 162; i++)
            // Exact frame number until Mario gains control
            ui.runFrame();

        int fitness = 0;
        int maxDistance = 0;
        int timeout = 0;

        while (true) {
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

            int points = ((time - 400) * 10) + (marioX) + (level * 250) + (world * 2000);

            timeout++;
            if (marioX > maxDistance) {
                maxDistance = marioX;
                timeout = 0;
            }
            // System.out.println("Lives: " + lives + " Timeout: " + timeout + " Distance: " + marioX);
            if (lives <= 2 || timeout > 30 + (marioX / 250) || marioState == 0x0B) {
                fitness = points;
                if (!(marioState == 0 || marioState == 4 || marioState == 5 || marioState == 7)) {
                    break;
                } else {
                    timeout = 0;
                }
            }

            final int[][] vision = new int[13][13];

            for (int dx = -vision[0].length / 2; dx < vision[0].length / 2; dx += 1)
                for (int dy = -vision.length / 2; dy < vision.length / 2; dy += 1) {
                    int x = marioX + (dx * 16) + 8;
                    int y = marioY + (dy * 16) - 16;
                    int page = (int) FastMath.floor(x / 256) % 2;
                    int subx = (int) FastMath.floor((x % 256) / 16);
                    int suby = (int) FastMath.floor((y - 32) / 16);
                    int addr = 0x500 + page * 13 * 16 + suby * 16 + subx;
                    if (suby >= 13 || suby < 0) {
                        // System.out.println("Outside level.");
                        vision[dy + (vision.length / 2)][dx + (vision[0].length / 2)] = 0;
                    } else {
                        // System.out.println("Block data at " + dx + ", " + dy + ": " + nes.cpuram.read(addr));
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
            double[] reactions = network.stepTime(visionUnwound);

            if (reactions[0] > 0) controller1.pressButton(PuppetController.Button.UP);
            if (reactions[1] > 0) controller1.pressButton(PuppetController.Button.DOWN);
            if (reactions[2] > 0) controller1.pressButton(PuppetController.Button.LEFT);
            if (reactions[3] > 0) controller1.pressButton(PuppetController.Button.RIGHT);
            if (reactions[4] > 0) controller1.pressButton(PuppetController.Button.A);
            if (reactions[5] > 0) controller1.pressButton(PuppetController.Button.B);
            // if (reactions[6] > 0) input.keyPressed(SELECT);
            // if (reactions[7] > 0) input.keyPressed(START);

            ui.runFrame();
        }
        fitness -= candidate.getConnectionGeneList().size() * 2;
        // System.out.println("Finished one evaluation that took " + (System.nanoTime() - startTime) / 1000000000f + " seconds.");
        fitness = fitness >= 0 ? fitness : 0;

        candidate.setScore(fitness);
        return candidate.getScore();
    }

    public static double[] unwind2DArray(int[][] arr) {
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

    @Override
    public boolean isNatural() {
        return true;
    }
}