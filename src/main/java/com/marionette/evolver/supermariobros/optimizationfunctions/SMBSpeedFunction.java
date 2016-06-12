package com.marionette.evolver.supermariobros.optimizationfunctions;

import org.javaneat.genome.NEATGenome;
import org.jnsgaii.functions.DefaultOptimizationFunction;
import org.jnsgaii.population.individual.Individual;
import org.jnsgaii.properties.Key;
import org.jnsgaii.properties.Properties;

import java.util.HashMap;
import java.util.List;

/**
 * Created by Mitchell on 4/14/2016.
 */
public class SMBSpeedFunction extends DefaultOptimizationFunction<NEATGenome> {
    @Override
    public double[] evaluate(List<Individual<NEATGenome>> individuals, HashMap<String, Object>[] computationResults, Properties properties) {
        return super.evaluate(individuals, computationResults, properties);
    }

    @Override
    public double min(Properties properties) {
        return 0;
    }

    @Override
    public double max(Properties properties) {
        return 50;
    }

    @Override
    public int compare(Double o1, Double o2) {
        return Double.compare(o1, o2); // Larger is better
    }

    @Override
    public double evaluateIndividual(NEATGenome object, HashMap<String, Object> computationResults, Properties properties) {
        MarioBrosData data = (MarioBrosData) computationResults.get(SMBComputation.ID);
        int start = data.dataPoints.get(0).marioX;
        int end = data.dataPoints.get(data.dataPoints.size() - 1).marioX;
        int distance = end - start;
        int time = data.dataPoints.get(0).time - data.dataPoints.get(data.dataPoints.size() - 1).time;
        if (time != 0)
            return distance / (double) time;
        else
            return 0;
    }

    @Override
    public Key[] requestProperties() {
        return new Key[0];
    }
}
