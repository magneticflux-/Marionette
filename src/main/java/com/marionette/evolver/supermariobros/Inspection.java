package com.marionette.evolver.supermariobros;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import org.javaneat.genome.NEATGenome;
import org.jnsgaii.population.PopulationData;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

/**
 * Created by Mitchell on 8/27/2016.
 */
public class Inspection {
    public static void main(String[] args) throws FileNotFoundException {
        Kryo kryo = new Kryo();

        Input in1 = new Input(new FileInputStream("generations/56_population.pd"));
        @SuppressWarnings("unchecked")
        PopulationData<NEATGenome> populationData1 = (PopulationData<NEATGenome>) kryo.readClassAndObject(in1);
        in1.close();

        Input in2 = new Input(new FileInputStream("generations/57_population.pd"));
        @SuppressWarnings("unchecked")
        PopulationData<NEATGenome> populationData2 = (PopulationData<NEATGenome>) kryo.readClassAndObject(in2);
        in2.close();
    }
}
