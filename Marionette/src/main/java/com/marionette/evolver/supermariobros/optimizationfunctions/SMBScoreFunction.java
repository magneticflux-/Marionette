package com.marionette.evolver.supermariobros.optimizationfunctions;

import org.apache.commons.collections4.comparators.ComparableComparator;
import org.javaneat.genome.NEATGenome;
import org.jnsgaii.functions.DefaultOptimizationFunction;
import org.jnsgaii.population.individual.Individual;
import org.jnsgaii.properties.Key;
import org.jnsgaii.properties.Properties;

import java.util.Comparator;
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
    public boolean isDeterministic() {
        return true;
    }

    @Override
    public double evaluateIndividual(NEATGenome object, HashMap<String, Object> computationResults, Properties properties) {
        MarioBrosData data = (MarioBrosData) computationResults.get(SMBComputation.ID);
        return data.dataPoints.parallelStream().mapToInt(MarioBrosData.DataPoint::getScore).max().orElseGet(() -> -1);
    }

    @Override
    public Key[] requestProperties() {
        return new Key[0];
    }

    @Override
    public Comparator<Double> getComparator() {
        return ComparableComparator.comparableComparator(); // Larger is better
    }
}
