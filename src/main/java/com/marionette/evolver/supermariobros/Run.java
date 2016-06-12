package com.marionette.evolver.supermariobros;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.marionette.evolver.supermariobros.optimizationfunctions.*;
import org.javaneat.evolution.nsgaii.NEATPopulationGenerator;
import org.javaneat.evolution.nsgaii.NEATRecombiner;
import org.javaneat.evolution.nsgaii.NEATSpeciator;
import org.javaneat.evolution.nsgaii.keys.NEATDoubleKey;
import org.javaneat.evolution.nsgaii.keys.NEATIntKey;
import org.javaneat.evolution.nsgaii.mutators.NEATLinkAdditionMutator;
import org.javaneat.evolution.nsgaii.mutators.NEATLinkSplitMutator;
import org.javaneat.evolution.nsgaii.mutators.NEATWeightMutator;
import org.javaneat.genome.NEATGenome;
import org.jnsgaii.cluster.JPPFJobComputation;
import org.jnsgaii.computations.Computation;
import org.jnsgaii.examples.defaultoperatorframework.RouletteWheelSquareRootSelection;
import org.jnsgaii.functions.OptimizationFunction;
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
import org.jppf.client.JPPFClient;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by Mitchell on 3/13/2016.
 */
public final class Run {
    private static final int GENERATION_TO_LOAD = -1;
    private static final boolean LOAD_FROM_DISK = false;

    private Run() {
    }

    public static void main(String[] args) throws FileNotFoundException {
        JPPFClient client = new JPPFClient("Marionette");
        Kryo kryo = new Kryo();
        @SuppressWarnings({"unchecked", "ConstantConditions"})
        PopulationData<NEATGenome> loadedPopulation = LOAD_FROM_DISK ? (PopulationData<NEATGenome>) kryo.readClassAndObject(new Input(new FileInputStream("generations/" + GENERATION_TO_LOAD + ".bin"))) : null;

        //noinspection MagicNumber
        Properties properties = new Properties()
                .setValue(Key.DoubleKey.DefaultDoubleKey.INITIAL_ASPECT_ARRAY, new double[]{
                        .8, 1, // Crossover STR/PROB
                        4, 1, 1, // Speciator maxd/disj/exce
                        .2, .5, // Weight mutation
                        .75, .2, // Link addition
                        .3, .2, // Link split
                })
                .setValue(Key.DoubleKey.DefaultDoubleKey.ASPECT_MODIFICATION_ARRAY, new double[]{
                        .125 / 4, 1, // Crossover STR
                        .125 / 4, 1, // Crossover PROB
                        .125 / 4, 1, // Speciator MAX MATING DISTANCE
                        .125 / 4, 1, // Speciator DISJOINT COEFFICIENT
                        .125 / 4, 1, // Speciator EXCESS COEFFICIENT
                        .125 / 4, 1, // Weight mutation STR
                        .125 / 4, 1, // Weight mutation PROB
                        .125 / 4, 1, // Link addition STR
                        .125 / 4, 1, // Link addition PROB
                        .125 / 4, 1, // Link split STR
                        .125 / 4, 1, // Link split PROB
                })
                .setInt(Key.IntKey.DefaultIntKey.POPULATION_SIZE, 100)
                .setInt(NEATIntKey.INPUT_COUNT, SMBComputation.VISION_SIZE * SMBComputation.VISION_SIZE)
                .setInt(NEATIntKey.OUTPUT_COUNT, 6)
                .setInt(NEATIntKey.INITIAL_LINK_COUNT, 1)
                .setDouble(NEATDoubleKey.NOVELTY_THRESHOLD, 10)
                .setInt(NEATIntKey.NOVELTY_DISTANCE_COUNT, 10);
        @SuppressWarnings("ConstantConditions")
        NEATPopulationGenerator neatPopulationGenerator = LOAD_FROM_DISK ? new NEATPopulationGenerator(loadedPopulation.getTruncatedPopulation().getPopulation().stream().map(individual -> new Individual<>(individual.getIndividual(), individual.aspects)).collect(Collectors.toList())) : new NEATPopulationGenerator();

        NEATSpeciator speciator = new NEATSpeciator();
        List<Mutator<NEATGenome>> mutators = Arrays.asList(new NEATWeightMutator(), new NEATLinkAdditionMutator(), new NEATLinkSplitMutator());
        Recombiner<NEATGenome> recombiner = new NEATRecombiner();
        Selector<NEATGenome> selector = new RouletteWheelSquareRootSelection<>();
        DefaultOperator<NEATGenome> operator = new DefaultOperator<>(mutators, recombiner, selector, speciator);

        @SuppressWarnings("ConstantConditions")
        SMBNoveltySearch noveltySearch = LOAD_FROM_DISK ? (SMBNoveltySearch) kryo.readClassAndObject(new Input(new FileInputStream("generations/" + GENERATION_TO_LOAD + "_novelty.bin"))) : new SMBNoveltySearch();
        SMBDistanceFunction distanceFunction = new SMBDistanceFunction();
        NEATConnectionCostFunction connectionCostOptimizationFunction = new NEATConnectionCostFunction();

        SMBComputation smbComputation = new SMBComputation();
        NEATConnectionCostComputation neatConnectionCostComputation = new NEATConnectionCostComputation();
        JPPFJobComputation<NEATGenome, MarioBrosData> jppfSMBComputation = JPPFJobComputation.wrapOptimizationFunction(smbComputation, client);
        JPPFJobComputation<NEATGenome, Double> jppfNEATConnectionCostComputation = JPPFJobComputation.wrapOptimizationFunction(neatConnectionCostComputation, client);

        List<OptimizationFunction<NEATGenome>> optimizationFunctions = Arrays.asList(
                distanceFunction,
                noveltySearch,
                connectionCostOptimizationFunction
        );

        List<Computation<NEATGenome, ?>> computations = Arrays.asList(
                jppfSMBComputation,
                jppfNEATConnectionCostComputation
        );

        NSGA_II<NEATGenome> nsga_ii = new NSGA_II<>(properties, operator, optimizationFunctions, neatPopulationGenerator, GENERATION_TO_LOAD, computations);

        nsga_ii.addObserver(populationData -> {
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

        DefaultVisualization.startInterface(operator, optimizationFunctions, computations, nsga_ii);

        //noinspection MagicNumber
        for (int i = 0; i < 1000000; i++) {
            nsga_ii.runGeneration();
        }
    }
}
