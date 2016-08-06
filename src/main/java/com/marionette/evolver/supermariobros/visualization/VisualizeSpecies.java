package com.marionette.evolver.supermariobros.visualization;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import org.javaneat.evolution.nsgaii.NEATSpeciator;
import org.javaneat.genome.NEATGenome;
import org.jnsgaii.population.PopulationData;
import org.jnsgaii.visualization.SpeciesVisualization;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

/**
 * Created by Mitchell on 5/22/2016.
 */
public class VisualizeSpecies {
    public static void main(String[] args) throws FileNotFoundException {
        Kryo kryo = new Kryo();

        Input in = new Input(new FileInputStream("generations/276_population.pd"));
        @SuppressWarnings("unchecked")
        PopulationData<NEATGenome> populationData = (PopulationData<NEATGenome>) kryo.readClassAndObject(in);
        in.close();

        SpeciesVisualization.startVisualization(populationData.getTruncatedPopulation(), new NEATSpeciator(), 276);
    }
}
