package com.marionette.evolver.supermariobros.optimizationfunctions;

import com.google.common.collect.Multiset;
import com.marionette.evolver.supermariobros.optimizationfunctions.keys.NoveltySearchDoubleKey;
import com.marionette.evolver.supermariobros.optimizationfunctions.keys.NoveltySearchIntKey;
import org.apache.commons.collections4.comparators.ComparableComparator;
import org.apache.commons.math3.util.FastMath;
import org.javaneat.genome.NEATGenome;
import org.jnsgaii.functions.OptimizationFunction;
import org.jnsgaii.population.individual.Individual;
import org.jnsgaii.properties.Key;
import org.jnsgaii.properties.Properties;
import org.jppf.JPPFException;
import org.jppf.client.JPPFClient;
import org.jppf.client.JPPFJob;
import org.jppf.node.protocol.AbstractTask;
import org.jppf.node.protocol.Task;

import java.util.*;

/**
 * Created by Mitchell Skaggs on 3/25/2016.
 */
public class SMBNoveltySearch implements OptimizationFunction<NEATGenome> {

    public static final String NOVELTY_BEHAVIOR_LIST_KEY = "noveltyBehaviorList";
    public static final boolean FAST_NOVELTY_CALC = true;
    private transient final SMBNoveltyBehaviorList noveltyBehaviorList;
    private transient final JPPFClient client;

    public SMBNoveltySearch(SMBNoveltyBehaviorList noveltyBehaviorList, JPPFClient client) {
        this.noveltyBehaviorList = noveltyBehaviorList;
        this.client = client;
    }

    private SMBNoveltySearch() {
        noveltyBehaviorList = new SMBNoveltyBehaviorList();
        client = null;
    }

    @Override
    public double[] evaluate(List<Individual<NEATGenome>> individuals, HashMap<String, Object>[] computationResults, Properties properties) {
        JPPFJob job = new JPPFJob("Optimization Function \"SMBNoveltySearch\"");
        job.setDataProvider(new HashMapDataProvider());
        job.getDataProvider().setParameter(NOVELTY_BEHAVIOR_LIST_KEY, noveltyBehaviorList);

        final int numDistances = properties.getInt(NoveltySearchIntKey.NOVELTY_DISTANCE_COUNT);
        final int numEntries = properties.getInt(NoveltySearchIntKey.NOVELTY_CACHE_MAX_ENTRIES);
        final double noveltyThreshold = properties.getDouble(NoveltySearchDoubleKey.NOVELTY_THRESHOLD);

        for (int i = 0; i < individuals.size(); i++) {
            try {
                final MarioBrosData computationResult = (MarioBrosData) computationResults[i].get(SMBComputation.ID);

                job.add(new AbstractTask<Double>() {
                    @Override
                    public void run() {
                        try {
                            SMBNoveltyBehaviorList noveltyBehaviorList = getDataProvider().getParameter(NOVELTY_BEHAVIOR_LIST_KEY);

                            List<Double> distances = new ArrayList<>();
                            for (Multiset.Entry<MarioBrosData> entry : noveltyBehaviorList.getBehaviorList().entrySet()) {
                                double distance = getDistance(computationResult, entry.getElement());
                                for (int j = 0; j < entry.getCount(); j++) {
                                    distances.add(distance);
                                }
                            }
                            Collections.sort(distances);

                            double average = distances.stream().mapToDouble(d -> d).limit(numDistances).average().orElse(0);
                            if (average >= noveltyThreshold) {
                                setResult(average);
                            } else {
                                setResult(0d);
                            }
                        } catch (Throwable t) {
                            setThrowable(t);
                        }
                    }
                });
            } catch (JPPFException e) {
                throw new Error(e);
            }
        }
        job.setBlocking(true);

        List<Task<?>> results = client.submitJob(job);
        double[] scores = new double[individuals.size()];

        for (int i = 0; i < individuals.size(); i++) {
            //noinspection ThrowableResultOfMethodCallIgnored
            if (results.get(i).getThrowable() != null)
                throw new Error(results.get(i).getThrowable());
            scores[i] = (Double) results.get(i).getResult();
            if (scores[i] >= noveltyThreshold || noveltyBehaviorList.getBehaviorList().size() == 0) {
                noveltyBehaviorList.add((MarioBrosData) computationResults[i].get(SMBComputation.ID), numDistances, numEntries);
            }
        }

        System.out.println("Unique novel behaviors: " + noveltyBehaviorList.getBehaviorList().entrySet().size());
        System.out.println("Total behaviors: " + noveltyBehaviorList.getBehaviorList().size());

        return scores;
    }

    private static double getDistance(MarioBrosData data1, MarioBrosData data2) {
        if (FAST_NOVELTY_CALC) {
            return FastMath.sqrt(
                    FastMath.pow(data1.getLastDataPoint().getMarioX() - data2.getLastDataPoint().getMarioX(), 2)
                            + FastMath.pow(data1.getLastDataPoint().getMarioY() - data2.getLastDataPoint().getMarioY(), 2));
        } else {
            double sum = 0;

            MarioBrosData.DataPoint dataPoint1 = null, dataPoint2 = null;
            Iterator<MarioBrosData.DataPoint> dataPointIterator1 = data1.dataPoints.iterator(), dataPointIterator2 = data2.dataPoints.iterator();

            while (dataPointIterator1.hasNext() || dataPointIterator2.hasNext()) {
                if (dataPointIterator1.hasNext())
                    dataPoint1 = dataPointIterator1.next();
                if (dataPointIterator2.hasNext())
                    dataPoint2 = dataPointIterator2.next();

                assert dataPoint1 != null && dataPoint2 != null;

                sum += FastMath.pow(dataPoint1.getScore() - dataPoint2.getScore(), 2);
                sum += FastMath.pow(dataPoint1.getMarioX() - dataPoint2.getMarioX(), 2);
                sum += FastMath.pow(dataPoint1.getMarioY() - dataPoint2.getMarioY(), 2);
                sum += FastMath.pow(dataPoint1.getMarioState() - dataPoint2.getMarioState(), 2);
            }

            return FastMath.sqrt(sum);
        }
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
        return false;
    }

    @Override
    public Comparator<Double> getComparator() {
        return ComparableComparator.comparableComparator(); //Higher is better
    }

    @Override
    public Key[] requestProperties() {
        return new Key[]{
                NoveltySearchDoubleKey.NOVELTY_THRESHOLD,
                NoveltySearchIntKey.NOVELTY_DISTANCE_COUNT,
                NoveltySearchIntKey.NOVELTY_CACHE_MAX_ENTRIES
        };
    }
}
