package com.marionette.evolver.supermariobros;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.marionette.evolver.supermariobros.optimizationfunctions.SMBComputation;
import com.marionette.evolver.supermariobros.optimizationfunctions.SMBDistanceFunction;
import com.marionette.evolver.supermariobros.optimizationfunctions.SMBNoveltySearch;
import org.javaneat.evolution.NEATInnovationMap;
import org.javaneat.evolution.nsgaii.NEATRecombiner;
import org.javaneat.evolution.nsgaii.NEATSpeciator;
import org.javaneat.evolution.nsgaii.mutators.NEATEnableGeneMutator;
import org.javaneat.evolution.nsgaii.mutators.NEATLinkAdditionMutator;
import org.javaneat.evolution.nsgaii.mutators.NEATLinkSplitMutator;
import org.javaneat.evolution.nsgaii.mutators.NEATWeightMutator;
import org.javaneat.genome.NEATGenome;
import org.jnsgaii.examples.defaultoperatorframework.RouletteWheelLinearSelection;
import org.jnsgaii.observation.DummyNSGAII;
import org.jnsgaii.operators.DefaultOperator;
import org.jnsgaii.operators.Mutator;
import org.jnsgaii.operators.Recombiner;
import org.jnsgaii.operators.Selector;
import org.jnsgaii.population.PopulationData;
import org.jnsgaii.visualization.DefaultVisualization;
import org.jppf.client.JPPFClient;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Created by Mitchell on 4/14/2016.
 */
public final class GenerationVisualizer {
    private GenerationVisualizer() {
    }

    public static void main(String[] args) {
        NEATSpeciator neatSpeciator = new NEATSpeciator();
        NEATInnovationMap neatInnovationMap = new NEATInnovationMap(-1, -1);
        List<Mutator<NEATGenome>> mutators = Arrays.asList(new NEATWeightMutator(), new NEATEnableGeneMutator(), new NEATLinkAdditionMutator(neatInnovationMap), new NEATLinkSplitMutator(neatInnovationMap));
        Recombiner<NEATGenome> recombiner = new NEATRecombiner(neatInnovationMap);
        Selector<NEATGenome> selector = new RouletteWheelLinearSelection<>();
        DefaultOperator<NEATGenome> operator = new DefaultOperator<>(mutators, recombiner, selector, neatSpeciator);

        //String[] aspectDescriptions = operator.getAspectDescriptions();
        //String[] scoreDescriptions = {"Novelty", "Distance", "Score", "Speed"};

        Kryo kryo = new Kryo();

        Stream<PopulationData<NEATGenome>> populationDataStream = IntStream.rangeClosed(1, 478).mapToObj(i -> {
            try (Input input = new Input(new FileInputStream("generations/" + i + "_population.pd"))) {
                //noinspection unchecked
                return (PopulationData<NEATGenome>) kryo.readClassAndObject(input);
            } catch (FileNotFoundException e) {
                throw new Error(e);
            }
        });

        DummyNSGAII<NEATGenome> dummyNSGAII = new DummyNSGAII<>(populationDataStream);

        DefaultVisualization.startInterface(operator, Arrays.asList(new SMBDistanceFunction(), new SMBNoveltySearch(null, null)), Arrays.asList(new SMBComputation()), dummyNSGAII, new JPPFClient());
        while (dummyNSGAII.loadGeneration()) {
            System.out.println("Loaded generation " + dummyNSGAII.getCurrentGeneration());
        }
    }
}
