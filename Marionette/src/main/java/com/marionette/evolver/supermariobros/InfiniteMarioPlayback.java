package com.marionette.evolver.supermariobros;

import ch.idsia.agents.IAgent;
import ch.idsia.benchmark.mario.engine.generalization.Enemy;
import ch.idsia.benchmark.mario.engine.input.MarioInput;
import ch.idsia.benchmark.mario.environments.MarioEnvironment;
import ch.idsia.benchmark.mario.options.FastOpts;
import ch.idsia.benchmark.mario.options.MarioOptions;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.marionette.evolver.supermariobros.optimizationfunctions.InfiniteMarioComputation;
import org.javaneat.genome.NEATGenome;
import org.javaneat.phenome.NEATPhenome;
import org.jnsgaii.multiobjective.population.FrontedIndividual;
import org.jnsgaii.population.PopulationData;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Mitchell on 7/28/2016.
 */
public class InfiniteMarioPlayback {
    public static void main(String[] args) throws FileNotFoundException, InterruptedException {
        MarioOptions.javaInit();
        String options = FastOpts.S_MARIO_SMALL + FastOpts.VIS_ON_2X + " fps 30 rfw 21 rfh 21 mm 2" + FastOpts.L_ENEMY(Enemy.GOOMBA, Enemy.GREEN_KOOPA, Enemy.GREEN_KOOPA_WINGED, Enemy.RED_KOOPA) + FastOpts.L_RANDOM_SEED(1) + FastOpts.AI_ZL_1_1 + FastOpts.L_LENGTH(1024 + 1024) + FastOpts.L_DEAD_ENDS_OFF + FastOpts.L_HIDDEN_BLOCKS_ON + FastOpts.S_TIME_LIMIT_800;
        MarioOptions.reset(true, options);

        Kryo kryo = new Kryo();

        Input in = new Input(new FileInputStream("generations/644_population.pd"));
        @SuppressWarnings("unchecked")
        PopulationData<NEATGenome> populationData = (PopulationData<NEATGenome>) kryo.readClassAndObject(in);
        in.close();

        List<FrontedIndividual<NEATGenome>> genomes = new ArrayList<>(populationData.getTruncatedPopulation().getPopulation());
        genomes.sort((o1, o2) -> -Double.compare(o1.getScore(0), o2.getScore(0)));
        FrontedIndividual<NEATGenome> individual = genomes.get(0);

        MarioEnvironment environment = MarioEnvironment.getInstance();
        IAgent agent = new InfiniteMarioComputation.NEATGenomeAgent(new NEATPhenome(individual.getIndividual()));
        environment.reset(agent);

        while (!environment.isLevelFinished()) {
            environment.tick();
            agent.observe(environment);
            MarioInput result = agent.actionSelection();
            environment.performAction(result);
            agent.receiveReward((float) environment.getIntermediateReward());
            System.out.println("X: " + environment.getLevelScene().mario.x);
        }
        System.out.println(environment.getEvaluationInfo());
        Thread.sleep(5);
        System.exit(0);
    }
}
