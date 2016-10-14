package com.marionette.evolver.supermariobros;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.pool.KryoCallback;
import com.esotericsoftware.kryo.pool.KryoPool;
import com.marionette.evolver.supermariobros.optimizationfunctions.InfiniteMarioComputation;
import com.marionette.evolver.supermariobros.optimizationfunctions.MarioBrosData;
import com.marionette.evolver.supermariobros.optimizationfunctions.NEATNetworkModularityFunction;
import com.marionette.evolver.supermariobros.optimizationfunctions.SMBComputation;
import com.marionette.evolver.supermariobros.optimizationfunctions.SMBDistanceFunction;
import com.marionette.evolver.supermariobros.optimizationfunctions.SMBNoveltyBehaviorList;
import com.marionette.evolver.supermariobros.optimizationfunctions.SMBNoveltySearch;
import com.marionette.evolver.supermariobros.optimizationfunctions.keys.NoveltySearchDoubleKey;
import com.marionette.evolver.supermariobros.optimizationfunctions.keys.NoveltySearchIntKey;

import org.javaneat.evolution.NEATInnovationMap;
import org.javaneat.evolution.nsgaii.NEATPopulationGenerator;
import org.javaneat.evolution.nsgaii.NEATRecombiner;
import org.javaneat.evolution.nsgaii.NEATSpeciator;
import org.javaneat.evolution.nsgaii.keys.NEATIntKey;
import org.javaneat.evolution.nsgaii.mutators.NEATEnableGeneMutator;
import org.javaneat.evolution.nsgaii.mutators.NEATLinkAdditionMutator;
import org.javaneat.evolution.nsgaii.mutators.NEATLinkSplitMutator;
import org.javaneat.evolution.nsgaii.mutators.NEATWeightMutator;
import org.javaneat.genome.ConnectionGene;
import org.javaneat.genome.NEATGenome;
import org.jnsgaii.cluster.computations.JPPFJobComputation;
import org.jnsgaii.computations.Computation;
import org.jnsgaii.examples.defaultoperatorframework.RouletteWheelLogarithmicSelection;
import org.jnsgaii.functions.OptimizationFunction;
import org.jnsgaii.multiobjective.NSGAII;
import org.jnsgaii.multiobjective.population.FrontedIndividual;
import org.jnsgaii.operators.DefaultOperator;
import org.jnsgaii.operators.Mutator;
import org.jnsgaii.operators.Recombiner;
import org.jnsgaii.operators.Selector;
import org.jnsgaii.population.PopulationData;
import org.jnsgaii.population.individual.Individual;
import org.jnsgaii.properties.Key;
import org.jnsgaii.properties.Properties;
import org.jnsgaii.visualization.TabbedVisualizationWindow;
import org.jppf.client.JPPFClient;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collectors;

import javax.swing.WindowConstants;

/**
 * Created by Mitchell on 7/28/2016.
 */
@SuppressWarnings("Duplicates")
public class InfiniteMarioRun {
    private static final int GENERATION_TO_LOAD = 0;
    private static final boolean LOAD_FROM_DISK = false;

    private InfiniteMarioRun() {
    }

    public static void main(String[] args) throws FileNotFoundException {
        JPPFClient jppfClient = new JPPFClient();//"Marionette");
        KryoPool kryoPool = new KryoPool.Builder(Kryo::new).build();

        //noinspection MagicNumber
        Properties properties = new Properties()
                .setValue(Key.DoubleKey.DefaultDoubleKey.INITIAL_ASPECT_ARRAY, new double[]{
                        .8, 1, // Crossover STR/PROB
                        5, 1, 1, // Speciator maxd/disj/exce
                        .2, 1, // Weight mutation
                        .2, .3, // Enable gene mutation
                        1, .8, // Link addition
                        .4, .8, // Link split
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
                .setInt(Key.IntKey.DefaultIntKey.POPULATION_SIZE, 500)
                .setInt(NEATIntKey.INPUT_COUNT, 11 * 11 + 6 + 4 * 3 + 1)
                .setInt(NEATIntKey.OUTPUT_COUNT, 6)
                .setInt(NEATIntKey.INITIAL_LINK_COUNT, 10)
                .setInt(NoveltySearchIntKey.NOVELTY_CACHE_MAX_ENTRIES, 2000)
                .setDouble(NoveltySearchDoubleKey.NOVELTY_THRESHOLD, 0)
                .setInt(NoveltySearchIntKey.NOVELTY_DISTANCE_COUNT, 25)
                .setInt(NEATIntKey.TARGET_SPECIES, 25);

        @SuppressWarnings("ConstantConditions")
        PopulationData<NEATGenome> loadedPopulation = LOAD_FROM_DISK ? kryoPool.run(kryo -> {
            try {
                //noinspection unchecked
                return (PopulationData<NEATGenome>) kryo.readClassAndObject(new Input(new FileInputStream("generations/" + GENERATION_TO_LOAD + "_population.pd")));
            } catch (FileNotFoundException e) {
                return null;
            }
        }) : null;

        @SuppressWarnings("ConstantConditions")
        NEATInnovationMap neatInnovationMap = LOAD_FROM_DISK ? kryoPool.run(kryo -> {
            try {
                return (NEATInnovationMap) kryo.readClassAndObject(new Input(new FileInputStream("generations/" + GENERATION_TO_LOAD + "_innovations.nim")));
            } catch (FileNotFoundException e) {
                return null;
            }
        }) : new NEATInnovationMap(properties.getInt(NEATIntKey.INPUT_COUNT), properties.getInt(NEATIntKey.OUTPUT_COUNT));

        @SuppressWarnings("ConstantConditions")
        SMBNoveltyBehaviorList noveltyBehaviorList = LOAD_FROM_DISK ? kryoPool.run(kryo -> {
            try {
                return (SMBNoveltyBehaviorList) kryo.readClassAndObject(new Input(new FileInputStream("generations/" + GENERATION_TO_LOAD + "_novelty.nbl")));
            } catch (FileNotFoundException e) {
                return null;
            }
        }) : new SMBNoveltyBehaviorList();

        @SuppressWarnings("ConstantConditions")
        NEATPopulationGenerator neatPopulationGenerator = LOAD_FROM_DISK ? NEATPopulationGenerator.createNEATPopulationGenerator(neatInnovationMap, loadedPopulation.getTruncatedPopulation().getPopulation().stream().map(individual -> new Individual<>(individual.getIndividual(), individual.aspects)).collect(Collectors.toList())) : NEATPopulationGenerator.createNEATPopulationGenerator(neatInnovationMap);

        NEATSpeciator speciator = new NEATSpeciator();
        List<Mutator<NEATGenome>> mutators = Arrays.asList(new NEATWeightMutator(), new NEATEnableGeneMutator(), new NEATLinkAdditionMutator(neatInnovationMap), new NEATLinkSplitMutator(neatInnovationMap));
        Recombiner<NEATGenome> recombiner = new NEATRecombiner(neatInnovationMap);
        Selector<NEATGenome> selector = new RouletteWheelLogarithmicSelection<>();//new RouletteWheelSquareRootSelection<>();
        DefaultOperator<NEATGenome> defaultOperator = new DefaultOperator<>(mutators, recombiner, selector, speciator);

        @SuppressWarnings("ConstantConditions")
        SMBNoveltySearch noveltySearch = new SMBNoveltySearch(noveltyBehaviorList, jppfClient);
        SMBDistanceFunction distanceFunction = new SMBDistanceFunction();
        NEATNetworkModularityFunction modularityFunction = new NEATNetworkModularityFunction(jppfClient);
        //NEATConnectionCostFunction connectionCostOptimizationFunction = new NEATConnectionCostFunction();

        SMBComputation smbComputation = new InfiniteMarioComputation();
        //NEATConnectionCostComputation neatConnectionCostComputation = new NEATConnectionCostComputation();
        JPPFJobComputation<NEATGenome, MarioBrosData> jppfSMBComputation = JPPFJobComputation.wrapOptimizationFunction(smbComputation, jppfClient);
        //JPPFJobComputation<NEATGenome, Double> jppfNEATConnectionCostComputation = JPPFJobComputation.wrapOptimizationFunction(neatConnectionCostComputation, jppfClient);

        List<OptimizationFunction<NEATGenome>> optimizationFunctions = Arrays.asList(
                distanceFunction,
                noveltySearch,
                modularityFunction
                //connectionCostOptimizationFunction
        );

        List<Computation<NEATGenome, ?>> computations = Arrays.asList(
                //smbComputation
                jppfSMBComputation
                //jppfNEATConnectionCostComputation
        );

        NSGAII<NEATGenome> nsgaii = new NSGAII<>(properties, defaultOperator, optimizationFunctions, neatPopulationGenerator, GENERATION_TO_LOAD, computations);

        nsgaii.addObserver(speciator);
        nsgaii.addObserver(populationData -> {
            ExecutorService executorService = Executors.newCachedThreadPool();
            executorService.submit(() -> {
                try (Output out = new Output(new FileOutputStream("generations/" + populationData.getCurrentGeneration() + "_population.pd"))) {
                    kryoPool.run(new KryoCallback<Void>() {
                        @Override
                        public Void execute(Kryo kryo) {
                            kryo.writeClassAndObject(out, populationData);
                            return null;
                        }
                    });
                    out.close();
                } catch (FileNotFoundException e) {
                    throw new Error(e);
                }
            });
            executorService.submit(() -> {
                try (Output out = new Output(new FileOutputStream("generations/" + populationData.getCurrentGeneration() + "_novelty.nbl"))) {
                    kryoPool.run(new KryoCallback<Void>() {
                        @Override
                        public Void execute(Kryo kryo) {
                            kryo.writeClassAndObject(out, noveltyBehaviorList);
                            return null;
                        }
                    });
                    out.close();
                } catch (FileNotFoundException e) {
                    throw new Error(e);
                }
            });
            executorService.submit(() -> {
                try (Output out = new Output(new FileOutputStream("generations/" + populationData.getCurrentGeneration() + "_innovations.nim"))) {
                    kryoPool.run(new KryoCallback<Void>() {
                        @Override
                        public Void execute(Kryo kryo) {
                            kryo.writeClassAndObject(out, neatInnovationMap);
                            return null;
                        }
                    });
                    out.close();
                } catch (FileNotFoundException e) {
                    throw new Error(e);
                }
            });
            executorService.shutdown();
            try {
                boolean succeeded = executorService.awaitTermination(1, java.util.concurrent.TimeUnit.MINUTES);
                if (!succeeded)
                    throw new Error();
            } catch (InterruptedException e) {
                throw new Error(e);
            }
        });

        //DefaultVisualization.startInterface(defaultOperator, optimizationFunctions, computations, nsgaii, jppfClient);
        TabbedVisualizationWindow tabbedVisualizationWindow = new TabbedVisualizationWindow()
                .addCurrentScoreDistributionsTab(nsgaii, optimizationFunctions)
                .addPriorScoresTab(nsgaii, optimizationFunctions)
                .addMedianAspectValuesTab(nsgaii, defaultOperator)
                .addElapsedTimesTab(nsgaii, optimizationFunctions, computations)
                .addJPPFJobProgressTab(nsgaii, jppfClient)
                .addGenerationStatisticsTab(nsgaii, Arrays.asList(
                        new TabbedVisualizationWindow.StatisticFunction<NEATGenome>() {
                            @Override
                            public String getName() {
                                return "Compatible Mates";
                            }

                            @Override
                            public double[] apply(PopulationData<NEATGenome> populationData) {
                                return populationData.getTruncatedPopulation().getPopulation().stream().mapToDouble(
                                        (ToDoubleFunction<FrontedIndividual<NEATGenome>>) value -> populationData.getTruncatedPopulation().getPopulation().parallelStream().filter(
                                                (Predicate<FrontedIndividual<NEATGenome>>) neatGenomeFrontedIndividual -> speciator.apply(value, neatGenomeFrontedIndividual)).count()).toArray();
                            }
                        },
                        new TabbedVisualizationWindow.StatisticFunction<NEATGenome>() {
                            @Override
                            public String getName() {
                                return "Neural Network Size";
                            }

                            @Override
                            public double[] apply(PopulationData<NEATGenome> populationData) {
                                return populationData.getTruncatedPopulation().getPopulation().stream().mapToDouble(
                                        (ToDoubleFunction<FrontedIndividual<NEATGenome>>) value -> value.getIndividual().getConnectionGeneList().stream()
                                                .filter(ConnectionGene::getEnabled).count()).toArray();
                            }
                        },
                        speciator.getNumSpeciesStatisticFunction(),
                        speciator.getSpeciesSizeStatisticFunction()
                ));

        tabbedVisualizationWindow.setLocation(10, 10);
        tabbedVisualizationWindow.setSize(900, 900);
        tabbedVisualizationWindow.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        tabbedVisualizationWindow.setVisible(true);

        //TODO Have NSGAII interface accept PopulationData instances and pass them through to its listeners, load history unobtrusively

        //noinspection MagicNumber
        for (int i = 0; i < 1000000; i++) {
            nsgaii.runGeneration();
        }
    }
}
