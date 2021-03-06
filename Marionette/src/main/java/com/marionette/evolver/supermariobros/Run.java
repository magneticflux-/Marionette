package com.marionette.evolver.supermariobros;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.marionette.evolver.supermariobros.optimizationfunctions.*;
import com.marionette.evolver.supermariobros.optimizationfunctions.keys.NoveltySearchDoubleKey;
import com.marionette.evolver.supermariobros.optimizationfunctions.keys.NoveltySearchIntKey;
import org.javaneat.evolution.NEATInnovationMap;
import org.javaneat.evolution.nsgaii.NEATPopulationGenerator;
import org.javaneat.evolution.nsgaii.NEATRecombiner;
import org.javaneat.evolution.nsgaii.NEATSpeciatorEx;
import org.javaneat.evolution.nsgaii.keys.NEATIntKey;
import org.javaneat.evolution.nsgaii.mutators.NEATEnableGeneMutator;
import org.javaneat.evolution.nsgaii.mutators.NEATLinkAdditionMutator;
import org.javaneat.evolution.nsgaii.mutators.NEATLinkSplitMutator;
import org.javaneat.evolution.nsgaii.mutators.NEATWeightMutator;
import org.javaneat.genome.NEATGenome;
import org.jnsgaii.cluster.computations.JPPFJobComputation;
import org.jnsgaii.computations.Computation;
import org.jnsgaii.examples.defaultoperatorframework.RouletteWheelSquareRootSelection;
import org.jnsgaii.functions.OptimizationFunction;
import org.jnsgaii.multiobjective.NSGAII;
import org.jnsgaii.operators.DefaultOperator;
import org.jnsgaii.operators.Mutator;
import org.jnsgaii.operators.Recombiner;
import org.jnsgaii.operators.Selector;
import org.jnsgaii.population.PopulationData;
import org.jnsgaii.properties.Key;
import org.jnsgaii.properties.Properties;
import org.jnsgaii.visualization.DefaultVisualization;
import org.jppf.client.JPPFClient;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Mitchell Skaggs on 3/13/2016.
 */
public final class Run {
    private static final int GENERATION_TO_LOAD = 164;
    private static final boolean LOAD_FROM_DISK = true;

    private Run() {
    }

    public static void main(String[] args) throws FileNotFoundException {
        JPPFClient client = new JPPFClient();//"Marionette");
        Kryo kryo = new Kryo();

        //noinspection MagicNumber
        Properties properties = new Properties()
                .setValue(Key.DoubleKey.DefaultDoubleKey.INITIAL_ASPECT_ARRAY, new double[]{
                        .8, 1, // Crossover STR/PROB
                        10, 1, 1, // Speciator maxd/disj/exce
                        .2, .5, // Weight mutation
                        .2, .25, // Enable gene mutation
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
                        .125 / 4, 1, // Enable gene mutation STR
                        .125 / 4, 1, // Enable gene mutation PROB
                        .125 / 4, 1, // Link addition STR
                        .125 / 4, 1, // Link addition PROB
                        .125 / 4, 1, // Link split STR
                        .125 / 4, 1, // Link split PROB
                })
                .setInt(Key.IntKey.DefaultIntKey.POPULATION_SIZE, 1000)
                .setInt(NEATIntKey.INPUT_COUNT, SMBComputation.VISION_SIZE * SMBComputation.VISION_SIZE)
                .setInt(NEATIntKey.OUTPUT_COUNT, 6)
                .setInt(NEATIntKey.INITIAL_LINK_COUNT, 1)
                .setDouble(NoveltySearchDoubleKey.NOVELTY_THRESHOLD, 25)
                .setInt(NoveltySearchIntKey.NOVELTY_DISTANCE_COUNT, 10);

        @SuppressWarnings({"unchecked", "ConstantConditions"})
        PopulationData<NEATGenome> loadedPopulation = LOAD_FROM_DISK ? (PopulationData<NEATGenome>) kryo.readClassAndObject(new Input(new FileInputStream("generations/" + GENERATION_TO_LOAD + "_population.pd"))) : null;
        @SuppressWarnings("ConstantConditions")
        NEATInnovationMap neatInnovationMap = LOAD_FROM_DISK ? (NEATInnovationMap) kryo.readClassAndObject(new Input(new FileInputStream("generations/" + GENERATION_TO_LOAD + "_innovations.nim"))) : new NEATInnovationMap(properties.getInt(NEATIntKey.INPUT_COUNT), properties.getInt(NEATIntKey.OUTPUT_COUNT));
        @SuppressWarnings("ConstantConditions")
        SMBNoveltyBehaviorList noveltyBehaviorList = LOAD_FROM_DISK ? (SMBNoveltyBehaviorList) kryo.readClassAndObject(new Input(new FileInputStream("generations/" + GENERATION_TO_LOAD + "_novelty.nbl"))) : new SMBNoveltyBehaviorList();
        @SuppressWarnings("ConstantConditions")
        NEATPopulationGenerator neatPopulationGenerator = LOAD_FROM_DISK ? NEATPopulationGenerator.createNEATPopulationGenerator(neatInnovationMap, loadedPopulation.getTruncatedPopulation()) : NEATPopulationGenerator.createNEATPopulationGenerator(neatInnovationMap);

        NEATSpeciatorEx speciatorEx = new NEATSpeciatorEx();
        List<Mutator<NEATGenome>> mutators = Arrays.asList(new NEATWeightMutator(), new NEATEnableGeneMutator(), new NEATLinkAdditionMutator(neatInnovationMap), new NEATLinkSplitMutator(neatInnovationMap));
        Recombiner<NEATGenome> recombiner = new NEATRecombiner(neatInnovationMap);
        Selector<NEATGenome> selector = new RouletteWheelSquareRootSelection<>();
        DefaultOperator<NEATGenome> operator = new DefaultOperator<>(mutators, recombiner, selector, speciatorEx);

        @SuppressWarnings("ConstantConditions")
        SMBNoveltySearch noveltySearch = new SMBNoveltySearch(noveltyBehaviorList, client);
        SMBDistanceFunction distanceFunction = new SMBDistanceFunction();
        //NEATConnectionCostFunction connectionCostOptimizationFunction = new NEATConnectionCostFunction();

        SMBComputation smbComputation = new SMBComputation();
        //NEATConnectionCostComputation neatConnectionCostComputation = new NEATConnectionCostComputation();
        JPPFJobComputation<NEATGenome, MarioBrosData> jppfSMBComputation = JPPFJobComputation.wrapOptimizationFunction(smbComputation, client);
        //JPPFJobComputation<NEATGenome, Double> jppfNEATConnectionCostComputation = JPPFJobComputation.wrapOptimizationFunction(neatConnectionCostComputation, client);

        List<OptimizationFunction<NEATGenome>> optimizationFunctions = Arrays.asList(
                distanceFunction,
                noveltySearch
                //connectionCostOptimizationFunction
        );

        List<Computation<NEATGenome, ?>> computations = Arrays.asList(
                //smbComputation
                jppfSMBComputation
                //jppfNEATConnectionCostComputation
        );

        NSGAII<NEATGenome> nsgaii = new NSGAII<>(properties, operator, optimizationFunctions, neatPopulationGenerator, GENERATION_TO_LOAD, computations);

        nsgaii.addObserver(populationData -> {
            try (
                    Output out1 = new Output(new FileOutputStream("generations/" + populationData.getCurrentGeneration() + "_population.pd"));
                    Output out2 = new Output(new FileOutputStream("generations/" + populationData.getCurrentGeneration() + "_novelty.nbl"));
                    Output out3 = new Output(new FileOutputStream("generations/" + populationData.getCurrentGeneration() + "_innovations.nim"))
            ) {
                kryo.writeClassAndObject(out1, populationData);
                out1.close();

                kryo.writeClassAndObject(out2, noveltyBehaviorList);
                out2.close();

                kryo.writeClassAndObject(out3, neatInnovationMap);
                out3.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        });

        DefaultVisualization.startInterface(operator, optimizationFunctions, computations, nsgaii, client);

        //noinspection MagicNumber
        for (int i = 0; i < 1000000; i++) {
            nsgaii.runGeneration();
        }
    }
}
