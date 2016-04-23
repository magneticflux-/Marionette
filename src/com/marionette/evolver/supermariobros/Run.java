package com.marionette.evolver.supermariobros;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.grapeshot.halfnes.CPURAM;
import com.grapeshot.halfnes.ui.HeadlessUI;
import com.grapeshot.halfnes.ui.PuppetController;
import com.marionette.evolver.supermariobros.optimizationfunctions.SMBDistanceFunction;
import com.marionette.evolver.supermariobros.optimizationfunctions.SMBNoveltySearch;
import com.marionette.evolver.supermariobros.optimizationfunctions.SMBScoreFunction;
import org.apache.commons.math3.util.FastMath;
import org.javaneat.evolution.RunDemo;
import org.javaneat.evolution.nsgaii.MarioBrosData;
import org.javaneat.evolution.nsgaii.NEATPopulationGenerator;
import org.javaneat.evolution.nsgaii.NEATRecombiner;
import org.javaneat.evolution.nsgaii.NEATSpeciator;
import org.javaneat.evolution.nsgaii.keys.NEATDoubleKey;
import org.javaneat.evolution.nsgaii.keys.NEATIntKey;
import org.javaneat.evolution.nsgaii.mutators.NEATLinkAdditionMutator;
import org.javaneat.evolution.nsgaii.mutators.NEATLinkRemovalMutator;
import org.javaneat.evolution.nsgaii.mutators.NEATLinkSplitMutator;
import org.javaneat.evolution.nsgaii.mutators.NEATWeightMutator;
import org.javaneat.genome.NEATGenome;
import org.javaneat.phenome.NEATPhenome;
import org.jnsgaii.OptimizationFunction;
import org.jnsgaii.examples.defaultoperatorframework.RouletteWheelSquareRootSelection;
import org.jnsgaii.multiobjective.NSGA_II;
import org.jnsgaii.operators.DefaultOperator;
import org.jnsgaii.operators.Mutator;
import org.jnsgaii.operators.Recombiner;
import org.jnsgaii.operators.Selector;
import org.jnsgaii.population.PopulationData;
import org.jnsgaii.population.individual.Individual;
import org.jnsgaii.properties.Key;
import org.jnsgaii.properties.Properties;
import org.jnsgaii.visualization.DefaultVisualization;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by Mitchell on 3/13/2016.
 */
public final class Run {
    private static final ThreadLocal<HeadlessUI> ui = new ThreadLocal<>();
    private static final int visionSize = 11;
    private static final int generationToLoad = 2048;

    private Run() {
    }

    public static void main(String[] args) throws FileNotFoundException {
        Kryo kryo = new Kryo();
        @SuppressWarnings("unchecked") PopulationData<NEATGenome> loadedPopulation = (PopulationData<NEATGenome>) kryo.readClassAndObject(new Input(new FileInputStream("generations/" + generationToLoad + ".bin")));

        //noinspection MagicNumber
        Properties properties = new Properties()
                .setValue(Key.DoubleKey.DefaultDoubleKey.INITIAL_ASPECT_ARRAY, new double[]{
                        .5, 1, // Crossover STR/PROB
                        4, 1, 1, // Speciator maxd/disj/exce
                        1, .25, // Weight mutation
                        1, .4, // Link addition
                        0, 0, // Link removal
                        .1, .3, // Link split
                })
                .setValue(Key.DoubleKey.DefaultDoubleKey.ASPECT_MODIFICATION_ARRAY, new double[]{
                        .125 / 2, 1, // Crossover STR
                        .125 / 2, 1, // Crossover PROB
                        .125 / 1, 1, // Speciator MAX MATING DISTANCE
                        .125 / 2, 1, // Speciator DISJOINT COEFFICIENT
                        .125 / 2, 1, // Speciator EXCESS COEFFICIENT
                        .125 / 2, 1, // Weight mutation STR
                        .125 / 4, 1, // Weight mutation PROB
                        .125 / 2, 1, // Link addition STR
                        .125 / 4, 1, // Link addition PROB
                        .125 / 4, 1, // Link removal STR
                        .125 / 4, 1, // Link removal PROB
                        .125 / 2, 1, // Link split STR
                        .125 / 2, 1, // Link split PROB
                })
                .setInt(Key.IntKey.DefaultIntKey.POPULATION_SIZE, 100)
                .setInt(NEATIntKey.INPUT_COUNT, visionSize * visionSize)
                .setInt(NEATIntKey.OUTPUT_COUNT, 6)
                .setInt(NEATIntKey.INITIAL_LINK_COUNT, 2)
                .setDouble(NEATDoubleKey.NOVELTY_THRESHOLD, 10)
                .setInt(NEATIntKey.NOVELTY_DISTANCE_COUNT, 10);

        NEATPopulationGenerator neatPopulationGenerator = new NEATPopulationGenerator(loadedPopulation.getTruncatedPopulation().getPopulation().stream().map(individual -> new Individual<>(individual.getIndividual(), individual.aspects)).collect(Collectors.toList()));

        NEATSpeciator neatSpeciator = new NEATSpeciator();
        List<Mutator<NEATGenome>> mutators = Arrays.asList(new NEATWeightMutator(), new NEATLinkAdditionMutator(), new NEATLinkRemovalMutator(), new NEATLinkSplitMutator());
        Recombiner<NEATGenome> recombiner = new NEATRecombiner();
        Selector<NEATGenome> selector = new RouletteWheelSquareRootSelection<>();
        DefaultOperator<NEATGenome> operator = new DefaultOperator<>(mutators, recombiner, selector, neatSpeciator);

        SMBNoveltySearch noveltySearch = (SMBNoveltySearch) kryo.readClassAndObject(new Input(new FileInputStream("generations/" + generationToLoad + "_novelty.bin")));

        List<OptimizationFunction<NEATGenome>> optimizationFunctions = Arrays.asList(noveltySearch, new SMBDistanceFunction(), new SMBScoreFunction());

        NSGA_II<NEATGenome> nsga_ii = new NSGA_II<>(properties, operator, optimizationFunctions, neatPopulationGenerator, generationToLoad);

        nsga_ii.addObserver(populationData -> {
            System.out.println("Max distance = " + populationData.getTruncatedPopulation().getPopulation().parallelStream().mapToDouble(value -> {
                assert value.getIndividual().marioBrosData != null;
                return value.getIndividual().marioBrosData.dataPoints.parallelStream().mapToDouble(value1 -> value1.marioX).max().orElse(Double.NaN);
            }).max().orElse(Double.NaN));

            try {
                Output out1 = new Output(new FileOutputStream("generations/" + populationData.getCurrentGeneration() + ".bin"));
                Output out2 = new Output(new FileOutputStream("generations/" + populationData.getCurrentGeneration() + "_novelty.bin"));
                kryo.writeClassAndObject(out1, populationData);
                kryo.writeClassAndObject(out2, noveltySearch);

                out1.close();
                out2.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        });

        DefaultVisualization.startInterface(operator, optimizationFunctions, nsga_ii, properties);

        //noinspection MagicNumber
        for (int i = 0; i < 1000000; i++) {
            nsga_ii.runGeneration();
        }
    }

    public static void verifyScores(Collection<Individual<NEATGenome>> individuals) {
        individuals.parallelStream().filter(neatGenomeIndividual -> neatGenomeIndividual.getIndividual().marioBrosData == null).forEach(neatGenomeIndividual -> computeFitness(neatGenomeIndividual.getIndividual()));
    }

    @SuppressWarnings("MagicNumber")
    private static void computeFitness(NEATGenome candidate) {

        final NEATPhenome network = new NEATPhenome(candidate);
        if (ui.get() == null) {
            ui.set(new HeadlessUI("roms/Super Mario Bros..nes", false));
        }
        final HeadlessUI ui = Run.ui.get();
        ui.getNes().reset();

        CPURAM cpuram;
        PuppetController controller1 = ui.getController1();

        boolean goesRight = candidate.getConnectionGeneList().stream().anyMatch(connectionGene -> connectionGene.getToNode() == candidate.getManager().getOutputOffset() + 3);

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
        }

        if (data.dataPoints.size() < 2)
            data.dataPoints.add(new MarioBrosData.DataPoint(data.dataPoints.get(0)));

        candidate.marioBrosData = data;
    }

}
