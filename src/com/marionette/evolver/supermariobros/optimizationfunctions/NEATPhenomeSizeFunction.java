package com.marionette.evolver.supermariobros.optimizationfunctions;

import com.marionette.evolver.supermariobros.Run;
import org.javaneat.genome.ConnectionGene;
import org.javaneat.genome.NEATGenome;
import org.javaneat.genome.NeuronType;
import org.jnsgaii.DefaultOptimizationFunction;
import org.jnsgaii.population.individual.Individual;
import org.jnsgaii.properties.Properties;

import java.util.List;

/**
 * Created by Mitchell on 3/25/2016.
 */
public class NEATPhenomeSizeFunction extends DefaultOptimizationFunction<NEATGenome> {

    @Override
    public double[] evaluate(List<Individual<NEATGenome>> individuals, Properties properties) {
        Run.verifyScores(individuals);
        return super.evaluate(individuals, properties);
    }

    @Override
    public double evaluateIndividual(NEATGenome object, Properties properties) {
        return object.getConnectionGeneList().parallelStream().filter(ConnectionGene::getEnabled).count() +
                object.getNeuronGeneList().parallelStream().filter(neuronGene -> neuronGene.getNeuronType() == NeuronType.HIDDEN).count();
    }

    @Override
    public double min(Properties properties) {
        return 0;
    }

    @Override
    public double max(Properties properties) {
        return 25;
    }

    @Override
    public int compare(Double o1, Double o2) {
        return -Double.compare(o1, o2); // Smaller is better
    }
}
