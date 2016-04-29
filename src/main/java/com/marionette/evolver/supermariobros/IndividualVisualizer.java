package com.marionette.evolver.supermariobros;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import org.javaneat.genome.NEATGenome;
import org.javaneat.visualization.Visualizer;
import org.jnsgaii.multiobjective.population.FrontedIndividual;
import org.jnsgaii.population.PopulationData;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Mitchell on 4/16/2016.
 */
public final class IndividualVisualizer {
    private IndividualVisualizer() {
    }

    public static void main(String[] args) throws FileNotFoundException {
        Kryo kryo = new Kryo();

        Input in = new Input(new FileInputStream("generations/1748.bin"));
        @SuppressWarnings("unchecked")
        PopulationData<NEATGenome> populationData = (PopulationData<NEATGenome>) kryo.readClassAndObject(in);
        in.close();

        List<FrontedIndividual<NEATGenome>> genomes = new ArrayList<>(populationData.getTruncatedPopulation().getPopulation());
        final int[] scoreOrder = {0, 3, 2, 1}; // Least to most important
        for (int i : scoreOrder)
            genomes.sort((o1, o2) -> -Double.compare(o1.getScore(i), o2.getScore(i)));


        Visualizer.getImage(true, 11, 6, genomes.get(0).getIndividual());
    }
}
