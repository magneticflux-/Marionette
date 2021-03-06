package com.marionette.evolver.supermariobros.optimizationfunctions;

import org.javaneat.genome.NEATGenome;
import org.javaneat.visualization.Visualizer;
import org.jnsgaii.computations.DefaultComputation;
import org.jnsgaii.population.individual.Individual;
import org.jnsgaii.properties.Properties;

import edu.uci.ics.jung.algorithms.layout.FRLayout;

/**
 * Created by Mitchell Skaggs on 6/12/2016.
 */
public class NEATConnectionCostComputation extends DefaultComputation<NEATGenome, Double> {
    public static final String ID = "NEATConnectionCostComputation";

    @Override
    public Double computeIndividual(Individual<NEATGenome> individual, Properties properties) {
        FRLayout<Long, Visualizer.Edge> layout = Visualizer.getLayout(true, SMBComputation.VISION_SIZE, 6, individual.getIndividual());

        double sumSquaredDistance = 0;
        for (Visualizer.Edge edge : layout.getGraph().getEdges())
            //noinspection ConstantConditions
            sumSquaredDistance += layout.apply(edge.fromNode).distanceSq(layout.apply(edge.toNode));

        return sumSquaredDistance;
    }

    @Override
    public String getComputationID() {
        return ID;
    }

    @Override
    public boolean isDeterministic() {
        return true;
    }
}
