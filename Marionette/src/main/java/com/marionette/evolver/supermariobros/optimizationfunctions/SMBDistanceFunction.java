package com.marionette.evolver.supermariobros.optimizationfunctions;

import org.javaneat.genome.NEATGenome;
import org.jnsgaii.functions.DefaultOptimizationFunction;
import org.jnsgaii.properties.Key;
import org.jnsgaii.properties.Properties;

import java.util.HashMap;

/**
 * Created by Mitchell on 3/25/2016.
 */
public class SMBDistanceFunction extends DefaultOptimizationFunction<NEATGenome> {
    @Override
    public double min(Properties properties) {
        return 0;
    }

    @Override
    public double max(Properties properties) {
        return 2000;
    }

    @Override
    public boolean isDeterministic() {
        return true;
    }

    @Override
    public int compare(Double o1, Double o2) {
        return Double.compare(o1, o2); // Larger is better
    }

    @Override
    public double evaluateIndividual(NEATGenome object, HashMap<String, Object> computationResults, Properties properties) {
        MarioBrosData data = (MarioBrosData) computationResults.get(SMBComputation.ID);
        return data.getLastDistance();
    }

    @Override
    public Key[] requestProperties() {
        return new Key[0];
    }
}
