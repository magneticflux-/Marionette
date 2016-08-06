package com.marionette.evolver.supermariobros.optimizationfunctions;

import org.javaneat.genome.NEATGenome;
import org.jnsgaii.functions.DefaultOptimizationFunction;
import org.jnsgaii.properties.Properties;

import java.util.HashMap;

/**
 * Created by Mitchell on 6/10/2016.
 */
public class NEATConnectionCostFunction extends DefaultOptimizationFunction<NEATGenome> {
    @Override
    public double evaluateIndividual(NEATGenome object, HashMap<String, Object> computationResults, Properties properties) {
        return (double) computationResults.get(NEATConnectionCostComputation.ID);
    }

    @Override
    public double min(Properties properties) {
        return 0;
    }

    @Override
    public double max(Properties properties) {
        return 100000000;
    }

    @Override
    public boolean isDeterministic() {
        return true;
    }

    @Override
    public int compare(Double o1, Double o2) {
        return -Double.compare(o1, o2); //Smaller is better
    }
}
