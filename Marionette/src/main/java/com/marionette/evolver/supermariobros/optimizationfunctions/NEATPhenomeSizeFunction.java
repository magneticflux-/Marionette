package com.marionette.evolver.supermariobros.optimizationfunctions;

import org.apache.commons.collections4.comparators.ComparableComparator;
import org.javaneat.genome.ConnectionGene;
import org.javaneat.genome.NEATGenome;
import org.javaneat.genome.NeuronType;
import org.jnsgaii.functions.DefaultOptimizationFunction;
import org.jnsgaii.properties.Properties;

import java.util.Comparator;
import java.util.HashMap;

/**
 * Created by Mitchell on 3/25/2016.
 */
public class NEATPhenomeSizeFunction extends DefaultOptimizationFunction<NEATGenome> {

    @Override
    public double evaluateIndividual(NEATGenome object, HashMap<String, Object> computationResults, Properties properties) {
        return object.getConnectionGeneList().stream().filter(ConnectionGene::getEnabled).count() +
                object.getNeuronGeneList().stream().filter(neuronGene -> neuronGene.getNeuronType() == NeuronType.HIDDEN).count();
    }

    @Override
    public double min(Properties properties) {
        return 0;
    }

    @Override
    public double max(Properties properties) {
        return 1000;
    }

    @Override
    public boolean isDeterministic() {
        return true;
    }

    @Override
    public Comparator<Double> getComparator() {
        return ComparableComparator.<Double>comparableComparator().reversed(); //Lower is better
    }
}
