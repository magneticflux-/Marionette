package com.marionette.evolver.supermariobros.optimizationfunctions;

import com.google.common.collect.TreeMultiset;
import com.marionette.evolver.supermariobros.Run;
import org.apache.commons.math3.util.FastMath;
import org.javaneat.evolution.nsgaii.MarioBrosData;
import org.javaneat.evolution.nsgaii.keys.NEATDoubleKey;
import org.javaneat.evolution.nsgaii.keys.NEATIntKey;
import org.javaneat.genome.NEATGenome;
import org.jnsgaii.OptimizationFunction;
import org.jnsgaii.population.individual.Individual;
import org.jnsgaii.properties.Key;
import org.jnsgaii.properties.Properties;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Created by Mitchell on 3/25/2016.
 */
public class SMBNoveltySearch implements OptimizationFunction<NEATGenome> {

    private final Collection<MarioBrosData> history = new ArrayList<>();

    public SMBNoveltySearch() {
    }

    private static double getDistance(MarioBrosData data1, MarioBrosData data2) {
        double sum = 0;

        MarioBrosData.DataPoint dataPoint1 = null, dataPoint2 = null;
        Iterator<MarioBrosData.DataPoint> dataPointIterator1 = data1.dataPoints.iterator(), dataPointIterator2 = data2.dataPoints.iterator();

        while (dataPointIterator1.hasNext() || dataPointIterator2.hasNext()) {
            if (dataPointIterator1.hasNext())
                dataPoint1 = dataPointIterator1.next();
            if (dataPointIterator2.hasNext())
                dataPoint2 = dataPointIterator2.next();

            assert dataPoint1 != null && dataPoint2 != null;

            sum += FastMath.pow(dataPoint1.score - dataPoint2.score, 2);
            sum += FastMath.pow(dataPoint1.time - dataPoint2.time, 2);
            sum += FastMath.pow(dataPoint1.world - dataPoint2.world, 2);
            sum += FastMath.pow(dataPoint1.level - dataPoint2.level, 2);
            sum += FastMath.pow(dataPoint1.lives - dataPoint2.lives, 2);
            sum += FastMath.pow(dataPoint1.marioX - dataPoint2.marioX, 2);
            sum += FastMath.pow(dataPoint1.marioY - dataPoint2.marioY, 2);
            sum += FastMath.pow(dataPoint1.marioState - dataPoint2.marioState, 2);
        }

        return FastMath.sqrt(sum);
    }

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
        int numDistances = properties.getInt(NEATIntKey.NOVELTY_DISTANCE_COUNT);
        if (history.size() < 1)
            history.add(individual.marioBrosData);
        TreeMultiset<Double> distances = history.parallelStream().map(value -> getDistance(individual.marioBrosData, value)).collect(Collectors.toCollection(TreeMultiset::create));
        double average = distances.stream().limit(numDistances).mapToDouble(value -> value).average().orElseGet(() -> Double.NaN);
        if (average > properties.getDouble(NEATDoubleKey.NOVELTY_THRESHOLD))
            history.add(individual.marioBrosData);
        if (average < 10)
            return 0;
        return average;
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
