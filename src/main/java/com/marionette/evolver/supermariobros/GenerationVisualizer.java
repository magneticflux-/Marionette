package com.marionette.evolver.supermariobros;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import org.javaneat.evolution.nsgaii.NEATRecombiner;
import org.javaneat.evolution.nsgaii.NEATSpeciator;
import org.javaneat.evolution.nsgaii.mutators.NEATLinkAdditionMutator;
import org.javaneat.evolution.nsgaii.mutators.NEATLinkRemovalMutator;
import org.javaneat.evolution.nsgaii.mutators.NEATLinkSplitMutator;
import org.javaneat.evolution.nsgaii.mutators.NEATWeightMutator;
import org.javaneat.genome.NEATGenome;
import org.jnsgaii.examples.defaultoperatorframework.RouletteWheelLinearSelection;
import org.jnsgaii.operators.DefaultOperator;
import org.jnsgaii.operators.Mutator;
import org.jnsgaii.operators.Recombiner;
import org.jnsgaii.operators.Selector;
import org.jnsgaii.population.PopulationData;
import org.jnsgaii.properties.HasAspectRequirements;
import org.jnsgaii.visualization.DefaultVisualization;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Created by Mitchell on 4/14/2016.
 */
public final class GenerationVisualizer {
    private GenerationVisualizer() {
    }

    public static void main(String[] args) throws FileNotFoundException {
        NEATSpeciator neatSpeciator = new NEATSpeciator();
        List<Mutator<NEATGenome>> mutators = Arrays.asList(new NEATWeightMutator(), new NEATLinkAdditionMutator(), new NEATLinkRemovalMutator(), new NEATLinkSplitMutator());
        Recombiner<NEATGenome> recombiner = new NEATRecombiner();
        Selector<NEATGenome> selector = new RouletteWheelLinearSelection<>();
        HasAspectRequirements operator = new DefaultOperator<>(mutators, recombiner, selector, neatSpeciator);

        String[] aspectDescriptions = operator.getAspectDescriptions();
        String[] scoreDescriptions = {"Novelty", "Distance", "Score", "Speed"};

        ThreadLocal<Kryo> kryo = ThreadLocal.withInitial(Kryo::new);
        AtomicInteger inc = new AtomicInteger(1);

        Stream<PopulationData<NEATGenome>> populationDataStream = IntStream.rangeClosed(1, 2887).parallel().mapToObj(i -> {
            Input in = null;
            try {
                in = new Input(new FileInputStream("generations/" + i + ".bin"));
            } catch (FileNotFoundException e) {
                throw new Error(e);
            }
            @SuppressWarnings("unchecked")
            PopulationData<NEATGenome> populationData = (PopulationData<NEATGenome>) kryo.get().readClassAndObject(in);
            in.close();
            //System.out.println(inc.getAndAdd(1));
            return populationData;
        });

        DefaultVisualization.displayGenerationGraph(aspectDescriptions, scoreDescriptions, populationDataStream);
    }
}
