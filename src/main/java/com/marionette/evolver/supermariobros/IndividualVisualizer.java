package com.marionette.evolver.supermariobros;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.marionette.evolver.supermariobros.optimizationfunctions.SMBComputation;
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

        Input in = new Input(new FileInputStream("generations/93_population.bin"));
        @SuppressWarnings("unchecked")
        PopulationData<NEATGenome> populationData = (PopulationData<NEATGenome>) kryo.readClassAndObject(in);
        in.close();

        List<FrontedIndividual<NEATGenome>> genomes = new ArrayList<>(populationData.getTruncatedPopulation().getPopulation());

        genomes.sort((o1, o2) -> -Double.compare(o1.getScore(0), o2.getScore(0)));

        Visualizer.getImage(true, SMBComputation.VISION_SIZE, 6, genomes.get(0).getIndividual());
    }
}
