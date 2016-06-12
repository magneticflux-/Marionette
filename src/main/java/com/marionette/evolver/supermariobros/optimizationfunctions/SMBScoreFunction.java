package com.marionette.evolver.supermariobros.optimizationfunctions;

import org.javaneat.genome.NEATGenome;
import org.jnsgaii.functions.DefaultOptimizationFunction;
import org.jnsgaii.population.individual.Individual;
import org.jnsgaii.properties.Key;
import org.jnsgaii.properties.Properties;

import java.util.HashMap;
import java.util.List;

/**
 * Created by Mitchell on 3/25/2016.
 */
public class SMBScoreFunction extends DefaultOptimizationFunction<NEATGenome> {

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
        return 1500;
    }

    @Override
    public int compare(Double o1, Double o2) {
        return Double.compare(o1, o2); // Larger is better
    }

    @Override
    public double evaluateIndividual(NEATGenome object, HashMap<String, Object> computationResults, Properties properties) {
        MarioBrosData data = (MarioBrosData) computationResults.get(SMBComputation.ID);
        return data.dataPoints.parallelStream().mapToInt(value -> value.score).max().orElseGet(() -> -1);
    }

    @Override
    public Key[] requestProperties() {
        return new Key[0];
    }
}
