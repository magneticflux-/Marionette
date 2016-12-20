package com.marionette.evolver.supermariobros.optimizationfunctions;

import edu.uci.ics.jung.graph.DirectedOrderedSparseMultigraph;
import org.apache.commons.collections4.comparators.ComparableComparator;
import org.javaneat.genome.NEATGenome;
import org.javaneat.phenome.NEATConnection;
import org.javaneat.phenome.NEATPhenome;
import org.jnsgaii.functions.OptimizationFunction;
import org.jnsgaii.population.individual.Individual;
import org.jnsgaii.properties.Key;
import org.jnsgaii.properties.Properties;
import org.jppf.JPPFException;
import org.jppf.client.JPPFClient;
import org.jppf.client.JPPFJob;
import org.jppf.node.protocol.AbstractTask;
import org.jppf.node.protocol.Task;
import org.slm4j.ModularityOptimizer;
import org.slm4j.Network;
import org.slm4j.VOSClusteringTechnique;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

/**
 * Created by Mitchell Skaggs on 8/10/2016.
 */
public class NEATNetworkModularityFunction implements OptimizationFunction<NEATGenome> {
    private transient final JPPFClient client;

    public NEATNetworkModularityFunction(JPPFClient client) {
        this.client = client;
    }

    private NEATNetworkModularityFunction() {
        this.client = null;
    }

    @Override
    public double[] evaluate(List<Individual<NEATGenome>> individuals, HashMap<String, Object>[] computationResults, Properties properties) {
        if (individuals.size() == 0)
            //noinspection unchecked
            return new double[0];

        JPPFJob job = new JPPFJob("Optimization Function \"NEATNetworkModularityFunction\"");

        for (int i = 0; i < individuals.size(); i++) {
            try {
                NEATPhenome phenome = new NEATPhenome(individuals.get(i).getIndividual());
                job.add(new AbstractTask<Double>() {
                    @Override
                    public void run() {
                        try {
                            DirectedOrderedSparseMultigraph<Integer, Integer> multigraph = new DirectedOrderedSparseMultigraph<>();
                            phenome.getConnectionList().forEach(new Consumer<NEATConnection>() {
                                int edgeNum = 0;

                                @Override
                                public void accept(NEATConnection neatConnection) {
                                    multigraph.addEdge(edgeNum++, neatConnection.getFromIndex(), neatConnection.getToIndex());
                                }
                            });
                            Network network = ModularityOptimizer.readJUNGGraph(multigraph, 1);
                            double resolution = 1d / (2 * network.getTotalEdgeWeight() + network.getTotalEdgeWeightSelfLinks());
                            VOSClusteringTechnique vosClusteringTechnique = new VOSClusteringTechnique(network, resolution);
                            Random r = new Random();
                            vosClusteringTechnique.runIteratedLouvainAlgorithmWithMultilevelRefinement(100, r);

                            setResult(vosClusteringTechnique.calcQualityFunction());
                        } catch (Throwable e) {
                            setThrowable(e);
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

        for (int i = 0; i < results.size(); i++) {
            //noinspection ThrowableResultOfMethodCallIgnored
            if (results.get(i).getThrowable() != null)
                throw new Error(results.get(i).getThrowable());
            scores[i] = (Double) results.get(i).getResult();
        }

        return scores;
    }

    @Override
    public double min(Properties properties) {
        return 0;
    }

    @Override
    public double max(Properties properties) {
        return 100;
    }

    @Override
    public boolean isDeterministic() {
        return true;
    }

    @Override
    public Comparator<Double> getComparator() {
        return ComparableComparator.comparableComparator(); //Higher is better
    }

    @Override
    public Key[] requestProperties() {
        return new Key[0];
    }
}
