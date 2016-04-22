package com.marionette.evolver.supermariobros.optimizationfunctions;

import com.marionette.evolver.supermariobros.Run;
import org.javaneat.genome.NEATGenome;
import org.jnsgaii.DefaultOptimizationFunction;
import org.jnsgaii.population.individual.Individual;
import org.jnsgaii.properties.Key;
import org.jnsgaii.properties.Properties;

import java.util.List;

/**
 * Created by Mitchell on 4/14/2016.
 */
public class SMBSpeedFunction extends DefaultOptimizationFunction<NEATGenome> {
    @Override
    public double[] evaluate(List<Individual<NEATGenome>> individuals, Properties properties) {
        Run.verifyScores(individuals);
        return super.evaluate(individuals, properties);
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
    public double evaluateIndividual(NEATGenome object, Properties properties) {
        int start = object.marioBrosData.dataPoints.get(0).marioX;
        int end = object.marioBrosData.dataPoints.get(object.marioBrosData.dataPoints.size() - 1).marioX;
        int distance = end - start;
        int time = object.marioBrosData.dataPoints.get(0).time - object.marioBrosData.dataPoints.get(object.marioBrosData.dataPoints.size() - 1).time;
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
