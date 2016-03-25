package com.marionette.evolver.supermariobros.optimizationfunctions;

import com.google.common.collect.TreeMultiset;
import com.marionette.evolver.supermariobros.Run;
import org.javaneat.evolution.nsgaii.MarioBrosData;
import org.javaneat.evolution.nsgaii.keys.NEATDoubleKey;
import org.javaneat.evolution.nsgaii.keys.NEATIntKey;
import org.javaneat.genome.NEATGenome;
import org.jnsgaii.OptimizationFunction;
import org.jnsgaii.population.individual.Individual;
import org.jnsgaii.properties.Key;
import org.jnsgaii.properties.Properties;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Created by Mitchell on 3/25/2016.
 */
public class SMBNoveltySearch implements OptimizationFunction<NEATGenome> {

    private final Collection<MarioBrosData> history = new LinkedHashSet<>();

    @Override
    public double[] evaluate(List<Individual<NEATGenome>> individuals, Properties properties) {
        Run.verifyScores(individuals);
        double[] scores = new double[individuals.size()];

        IntStream stream = IntStream.range(0, scores.length);

        stream.forEach(
                value -> scores[value] = evaluateIndividual(individuals.get(value).getIndividual(), properties)
        );
        return scores;
    }

    private double evaluateIndividual(NEATGenome individual, Properties properties) {
        //final int numDistances = properties.getInt(NEATIntKey.NOVELTY_DISTANCE_COUNT);
        if (history.size() < 1)
            history.add(individual.marioBrosData);
        TreeMultiset<Double> distances = history.parallelStream().map(value -> Run.getDistance(individual.marioBrosData, value)).collect(Collectors.toCollection(TreeMultiset::create));
        double average = distances.parallelStream().mapToDouble(value -> value).average().orElseGet(() -> Double.NaN);
        if (average > properties.getDouble(NEATDoubleKey.NOVELTY_THRESHOLD))
            history.add(individual.marioBrosData);
        return average;
    }


    @Override
    public double min(Properties properties) {
        return 0;
    }

    @Override
    public double max(Properties properties) {
        return 10;
    }

    @Override
    public int compare(Double o1, Double o2) {
        return Double.compare(o1, o2); // Larger is better
    }

    @Override
    public Key[] requestProperties() {
        return new Key[]{
                NEATDoubleKey.NOVELTY_THRESHOLD,
                NEATIntKey.NOVELTY_DISTANCE_COUNT
        };
    }
}
