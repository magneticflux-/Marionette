package com.marionette.evolver.supermariobros;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.io.Output;
import org.javaneat.evolution.NEATEvolutionaryOperator;
import org.javaneat.evolution.NEATGenomeManager;
import org.javaneat.evolution.NEATGenotypeFactory;
import org.javaneat.genome.NEATGenome;
import org.javaneat.phenome.NEATPhenome;
import org.uncommons.maths.random.Probability;
import org.uncommons.watchmaker.framework.*;
import org.uncommons.watchmaker.framework.selection.TournamentSelection;
import org.uncommons.watchmaker.framework.termination.UserAbort;
import org.uncommons.watchmaker.swing.ObjectSwingRenderer;
import org.uncommons.watchmaker.swing.evolutionmonitor.EvolutionMonitor;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.Random;

@Deprecated
public final class OldRun {
    private OldRun() {
    }

    public static void main(String[] args) {
        SelectionStrategy<Object> selectionStrategy = new TournamentSelection(Probability.ONE);
        final Kryo kryo = new Kryo();
        Random rng = new Random(0);

        final int numInputs = 169;
        final int numOutputs = 6;
        final int populationSize = 400;
        // final double eliteFraction = 0.1; // Broken at the moment
        final double disjointGeneCoefficient = 2;
        final double excessGeneCoefficient = 2;
        final double weightDifferenceCoefficient = 1;
        final int speciesTarget = 20;
        final int speciesStagnantTimeLimit = 25;
        final double speciesCutoff = 10;
        final double speciesCutoffDelta = 0.5;
        final double enableMutationProb = 0.2;
        final double disableMutationProb = 0.4;
        final double mutationWeightWholeProb = 0.25;
        final double mutationWeightProb = 0.9;
        final double mutationAddLinkProb = 0.9;
        final double mutationRemoveLinkProb = 0.9;
        final double mutationAddNodeProb = 0.5;
        final double mutationWeightRange = 0.1;
        final double crossoverChance = 0.75;
        NEATGenomeManager manager = new NEATGenomeManager(numInputs, numOutputs, disjointGeneCoefficient, excessGeneCoefficient, weightDifferenceCoefficient,
                speciesTarget, speciesCutoff, speciesCutoffDelta, populationSize, speciesStagnantTimeLimit, mutationWeightWholeProb, mutationWeightProb,
                mutationAddLinkProb, mutationAddNodeProb, mutationWeightRange, enableMutationProb, disableMutationProb, crossoverChance, mutationRemoveLinkProb);

        CandidateFactory<NEATGenome> candidateFactory = new NEATGenotypeFactory(manager);
        EvolutionaryOperator<NEATGenome> evolutionScheme = new NEATEvolutionaryOperator(manager);
        FitnessEvaluator<NEATGenome> fitnessEvaluator = new SuperMarioBrosFitness();

        GenerationalEvolutionEngine<NEATGenome> ge = new GenerationalEvolutionEngine<>(candidateFactory, evolutionScheme, fitnessEvaluator,
                selectionStrategy, rng);
        ge.addEvolutionObserver(new EvolutionObserver<NEATGenome>() {
            private long startTime = System.nanoTime();

            public void populationUpdate(PopulationData<? extends NEATGenome> data) {
                try (Output output = new Output(new FileOutputStream("saves/supermariobros/" + "generation_" + data.getGenerationNumber() + ".pop"))) {
                    kryo.writeClassAndObject(output, data.getBestCandidate());
                } catch (KryoException | FileNotFoundException e) {
                    e.printStackTrace();
                }

                System.out.printf("Generation %d: %s\n", data.getGenerationNumber(), data.getBestCandidate());
                System.out.println("Max fitness: " + data.getBestCandidateFitness());
                System.out.println("Time taken: " + (System.nanoTime() - startTime) / 1000000000f + " seconds");
                startTime = System.nanoTime();
                if (!Double.isFinite(data.getBestCandidateFitness())) {
                    System.err.println("Fitness was infinite.");
                    System.err.println("Genome: " + data.getBestCandidate());
                    System.err.println("Species: " + data.getBestCandidate().getSpecies());
                    System.exit(0);
                }
            }
        });

        final UserAbort abort = new UserAbort();
        final EvolutionMonitor<NEATGenome> monitor = new EvolutionMonitor<>(new ObjectSwingRenderer(), false);
        synchronized (monitor.getGUIComponent().getTreeLock()) {
            ((JTabbedPane) monitor.getGUIComponent().getComponents()[0]).add(new JPanel() {
                private static final long serialVersionUID = 1L;

                {
                    this.setName("Abort Button");
                    this.setLayout(new BorderLayout());
                    this.add(new JButton("ABORT") {
                        private static final long serialVersionUID = 1L;

                        {
                            this.setBackground(Color.RED);
                            this.setMaximumSize(new Dimension(100, 50));
                            this.setPreferredSize(new Dimension(100, 50));

                            this.addActionListener(e -> {
                                abort.abort();
                                System.out.println("*** ABORT SEQUENCE ACTIVATED ***");
                            });
                        }
                    }, BorderLayout.PAGE_START);
                }
            });
        }
        monitor.showInFrame("Evolution", true);
        ge.addEvolutionObserver(monitor);

        final NEATGenome result = ge.evolve(populationSize, 0, abort);
        System.out.println("Fittest individual: " + result);
        new NEATPhenome(result);
    }
}