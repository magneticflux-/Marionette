package com.marionette.evolver.supermariobros;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.List;
import java.util.Random;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import org.apache.commons.math3.util.FastMath;
import org.uncommons.maths.random.Probability;
import org.uncommons.watchmaker.framework.CandidateFactory;
import org.uncommons.watchmaker.framework.EvolutionObserver;
import org.uncommons.watchmaker.framework.EvolutionaryOperator;
import org.uncommons.watchmaker.framework.FitnessEvaluator;
import org.uncommons.watchmaker.framework.GenerationalEvolutionEngine;
import org.uncommons.watchmaker.framework.PopulationData;
import org.uncommons.watchmaker.framework.SelectionStrategy;
import org.uncommons.watchmaker.framework.selection.TournamentSelection;
import org.uncommons.watchmaker.framework.termination.UserAbort;
import org.uncommons.watchmaker.swing.ObjectSwingRenderer;
import org.uncommons.watchmaker.swing.evolutionmonitor.EvolutionMonitor;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.io.Output;
import com.javaneat.evolution.NEATEvolutionaryOperator;
import com.javaneat.evolution.NEATGenomeManager;
import com.javaneat.evolution.NEATGenotypeFactory;
import com.javaneat.genome.NEATGenome;
import com.javaneat.phenome.NEATPhenome;

public class Run
{
	public static void main(String[] args)
	{
		SelectionStrategy<Object> selectionStrategy = new TournamentSelection(Probability.ONE);
		final Kryo kryo = new Kryo();
		Random rng = new Random(0);

		final int numInputs = 169;
		final int numOutputs = 6;
		final int populationSize = 400;
		final double eliteFraction = 0.1;
		final double disjointGeneCoefficient = 2;
		final double excessGeneCoefficient = 2;
		final double weightDifferenceCoefficient = 1;
		final int speciesTarget = 20;
		final int speciesStagnantTimeLimit = 500;
		final double speciesCutoff = 5;
		final double speciesCutoffDelta = 0.5;
		final double mutationWeightProb = 0.85;
		final double mutationAddLinkProb = 0.3;
		final double mutationAddNodeProb = 0.3;
		final double mutationWeightRange = 1;
		NEATGenomeManager manager = new NEATGenomeManager(numInputs, numOutputs, disjointGeneCoefficient, excessGeneCoefficient, weightDifferenceCoefficient,
				speciesTarget, speciesCutoff, speciesCutoffDelta, populationSize, speciesStagnantTimeLimit, mutationWeightProb, mutationAddLinkProb,
				mutationAddNodeProb, mutationWeightRange);

		CandidateFactory<NEATGenome> candidateFactory = new NEATGenotypeFactory(manager);
		EvolutionaryOperator<NEATGenome> evolutionScheme = new NEATEvolutionaryOperator(manager);
		FitnessEvaluator<NEATGenome> fitnessEvaluator = new SuperMarioBrosFitness();

		GenerationalEvolutionEngine<NEATGenome> ge = new GenerationalEvolutionEngine<NEATGenome>(candidateFactory, evolutionScheme, fitnessEvaluator,
				selectionStrategy, rng);
		ge.addEvolutionObserver(new EvolutionObserver<NEATGenome>()
		{
			private long	startTime	= System.nanoTime();

			public void populationUpdate(PopulationData<? extends NEATGenome> data)
			{
				try (Output output = new Output(new FileOutputStream("NEAT-Mario/" + "generation_" + data.getGenerationNumber() + ".pop"));)
				{
					kryo.writeClassAndObject(output, data.getBestCandidate());
				}
				catch (KryoException | FileNotFoundException e)
				{
					e.printStackTrace();
				}

				System.out.printf("Generation %d: %s\n", data.getGenerationNumber(), data.getBestCandidate());
				System.out.println("Max fitness: " + data.getBestCandidateFitness());
				System.out.println("Time taken: " + (System.nanoTime() - startTime) / 1000000000f + " seconds");
				startTime = System.nanoTime();
				if (!Double.isFinite(data.getBestCandidateFitness()))
				{
					System.err.println("Fitness was infinite.");
					System.err.println("Genome: " + data.getBestCandidate());
					System.err.println("Species: " + data.getBestCandidate().getSpecies());
					System.exit(0);
				}
			}
		});

		final UserAbort abort = new UserAbort();
		final EvolutionMonitor<NEATGenome> monitor = new EvolutionMonitor<NEATGenome>(new ObjectSwingRenderer(), false);
		synchronized (monitor.getGUIComponent().getTreeLock())
		{
			((JTabbedPane) monitor.getGUIComponent().getComponents()[0]).add(new JPanel()
			{
				private static final long	serialVersionUID	= 1L;

				{
					this.setName("Abort Button");
					this.setLayout(new BorderLayout());
					this.add(new JButton("ABORT")
					{
						private static final long	serialVersionUID	= 1L;

						{
							this.setBackground(Color.RED);
							this.setMaximumSize(new Dimension(100, 50));
							this.setPreferredSize(new Dimension(100, 50));

							this.addActionListener(new ActionListener()
							{
								@Override
								public void actionPerformed(final ActionEvent e)
								{
									abort.abort();
									System.out.println("*** ABORT SEQUENCE ACTIVATED ***");
								}
							});
						}
					}, BorderLayout.PAGE_START);
				}
			});
		}
		monitor.showInFrame("Evolution", true);
		ge.addEvolutionObserver(monitor);

		final NEATGenome result = ge.evolve(populationSize, (int) FastMath.round(populationSize * eliteFraction), abort);
		System.out.println("Fittest individual: " + result);
		new NEATPhenome(result);
	}
}
