package com.marionette.evolver.supermariobros.optimizationfunctions;

import ch.idsia.agents.AgentOptions;
import ch.idsia.agents.controllers.IMarioDebugDraw;
import ch.idsia.agents.controllers.MarioAIBase;
import ch.idsia.benchmark.mario.engine.LevelScene;
import ch.idsia.benchmark.mario.engine.SimulatorOptions;
import ch.idsia.benchmark.mario.engine.VisualizationComponent;
import ch.idsia.benchmark.mario.engine.generalization.Enemy;
import ch.idsia.benchmark.mario.engine.generalization.Entity;
import ch.idsia.benchmark.mario.engine.generalization.EntityType;
import ch.idsia.benchmark.mario.engine.generalization.Tile;
import ch.idsia.benchmark.mario.engine.input.MarioInput;
import ch.idsia.benchmark.mario.engine.input.MarioKey;
import ch.idsia.benchmark.mario.engine.sprites.Mario;
import ch.idsia.benchmark.mario.engine.sprites.Sprite;
import ch.idsia.benchmark.mario.environments.IEnvironment;
import ch.idsia.benchmark.mario.environments.MarioEnvironment;
import ch.idsia.benchmark.mario.options.FastOpts;
import ch.idsia.benchmark.mario.options.MarioOptions;
import org.apache.commons.math3.util.FastMath;
import org.javaneat.genome.NEATGenome;
import org.javaneat.phenome.NEATPhenome;
import org.jnsgaii.population.individual.Individual;
import org.jnsgaii.properties.Properties;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.geom.Point2D;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by Mitchell on 7/27/2016.
 */
public class InfiniteMarioComputation extends SMBComputation {
    //public static final String ID = "InfiniteMarioComputation";

    private static int extractMarioMode(Mario.MarioMode marioMode) {
        switch (marioMode) {
            case SMALL:
                return 0;
            case LARGE:
                return 1;
            case FIRE_LARGE:
                return 2;
            default:
                return 0;
        }
    }

    @Override
    public String getComputationID() {
        return ID;
    }

    @Override
    public boolean isDeterministic() {
        return true;
    }

    @Override
    public MarioBrosData computeIndividual(Individual<NEATGenome> individual, Properties properties) {
        MarioOptions.javaInit();
        String options = FastOpts.S_MARIO_SMALL + FastOpts.VIS_OFF + " rfw 21 rfh 21 mm 2" + FastOpts.L_ENEMY(Enemy.GOOMBA, Enemy.GREEN_KOOPA, Enemy.GREEN_KOOPA_WINGED, Enemy.RED_KOOPA)
                + FastOpts.L_RANDOM_SEED(1) + FastOpts.AI_ZL_1_1 + FastOpts.L_LENGTH(1024 + 1024) + FastOpts.L_DEAD_ENDS_OFF + FastOpts.L_HIDDEN_BLOCKS_ON + FastOpts.S_TIME_LIMIT_800;
        MarioOptions.reset(false, options);
        IEnvironment environment = MarioEnvironment.getInstance();
        NEATGenomeAgent agent = new NEATGenomeAgent(new NEATPhenome(individual.getIndividual()));
        environment.reset(agent);

        int currentTick = 0;
        MarioBrosData data = new MarioBrosData();
        while (!environment.isLevelFinished()) {
            // UPDATE THE ENVIRONMENT
            environment.tick();
            // PUSH NEW PERCEPTS TO THE AGENT
            agent.observe(environment);
            // LET AGENT PERFORM ITS ACTION-SELECTION
            MarioInput actions = agent.actionSelection();
            // PROPAGATE ACTIONS TO THE ENVIRONMENT
            environment.performAction(actions);
            // NOTIFY AGENT ABOUT CURRENT INTERMEDIATE REWARD
            agent.receiveReward(environment.getIntermediateReward());

            if (currentTick++ % 60 == 0) {
                LevelScene levelScene = environment.getLevelScene();
                data.addDataPoint(new MarioBrosData.DataPoint(Mario.coins, (int) levelScene.mario.x, (int) levelScene.mario.y, extractMarioMode(levelScene.getMarioMode())));
            }
        }
        LevelScene levelScene = environment.getLevelScene();
        data.addDataPoint(new MarioBrosData.DataPoint(Mario.coins, (int) levelScene.mario.x, (int) levelScene.mario.y, extractMarioMode(levelScene.getMarioMode())));

        return data;
    }

    public static final class NEATGenomeAgent extends MarioAIBase implements IMarioDebugDraw {
        private final NEATPhenome phenome;
        private Entity[] priorEntities = new Entity[3];
        private float intermediateReward;

        public NEATGenomeAgent(NEATPhenome phenome) {
            super();
            this.phenome = phenome;
        }

        @Override
        public void reset(AgentOptions options) {
            super.reset(options);
            this.phenome.resetInternalState();
        }

        @Override
        public void receiveReward(float intermediateReward) {
            super.receiveReward(intermediateReward);
            this.intermediateReward = intermediateReward;
        }

        @Override
        public MarioInput actionSelection() {
            double[] inputs = new double[11 * 11 + 6 + 4 * 3 + 1]; // Grid, 6 Mario data, three enemy angles/distances/speeds/dangerous + reward
            int currentIndex = 0;

            for (int rowRel = -5; rowRel <= 5; rowRel++) { // 11x11
                for (int colRel = -5; colRel <= 5; colRel++) {
                    Tile tile = t.tile(rowRel, colRel);
                    inputs[currentIndex++] = tile.getCode() / 90d; // Normalize
                }
            }

            inputs[currentIndex++] = mario.state[0]; //status;
            inputs[currentIndex++] = mario.state[1]; //mode.getCode();
            inputs[currentIndex++] = mario.state[2]; //onGround ? 1 : 0;
            inputs[currentIndex++] = mario.state[3]; //mayJump ? 1 : 0;
            inputs[currentIndex++] = mario.state[4]; //mayShoot ? 1 : 0;
            inputs[currentIndex++] = mario.state[5]; //carrying ? 1 : 0;

            List<Entity> sortedEntities = e.entities.stream()
                    .filter(entity -> entity.type != EntityType.NOTHING && entity.type != EntityType.FIREBALL)
                    .collect(Collectors.toList());
            sortedEntities.sort((o1, o2) -> Double.compare(Point2D.distance(0, 0, o1.dX, o1.dY), Point2D.distance(0, 0, o2.dX, o2.dY)));

            Arrays.fill(priorEntities, null);

            for (int i = 0; i < priorEntities.length && i < sortedEntities.size(); i++) {
                Entity entity = sortedEntities.get(i);
                inputs[currentIndex++] = FastMath.atan2(entity.dY, entity.dX) / FastMath.PI;
                inputs[currentIndex++] = FastMath.sqrt(FastMath.pow(entity.dX, 2) + FastMath.pow(entity.dY, 2)) / 100;
                inputs[currentIndex++] = entity.speed.x / 10;
                inputs[currentIndex++] = entity.type == EntityType.DANGER ? 1 : 0;
                priorEntities[i] = entity;
            }

            inputs[currentIndex] = intermediateReward / 1000;

            double[] results = this.phenome.stepTime(inputs, 4);

            action.reset();
            action.set(MarioKey.UP, results[0] > PRESS_THRESHOLD);
            action.set(MarioKey.DOWN, results[1] > PRESS_THRESHOLD);
            action.set(MarioKey.LEFT, results[2] > PRESS_THRESHOLD);
            action.set(MarioKey.RIGHT, results[3] > PRESS_THRESHOLD);
            action.set(MarioKey.JUMP, results[4] > PRESS_THRESHOLD);
            action.set(MarioKey.SPEED, results[5] > PRESS_THRESHOLD);

            return super.actionSelection();
        }

        @Override
        public void debugDraw(VisualizationComponent visualizationComponent, LevelScene levelScene, IEnvironment iEnvironment, Graphics graphics) {
            int xCam = (int) (levelScene.mario.xOld + (levelScene.mario.x - levelScene.mario.xOld)) - 160;
            int yCam = (int) (levelScene.mario.yOld + (levelScene.mario.y - levelScene.mario.yOld)) - 120;
            if (xCam < 0)
                xCam = 0;
            if (yCam < 0)
                yCam = 0;
            if (xCam > levelScene.level.length * LevelScene.cellSize
                    - SimulatorOptions.VISUAL_COMPONENT_WIDTH)
                xCam = levelScene.level.length * LevelScene.cellSize
                        - SimulatorOptions.VISUAL_COMPONENT_WIDTH;
            if (yCam > levelScene.level.height * LevelScene.cellSize
                    - SimulatorOptions.VISUAL_COMPONENT_HEIGHT)
                yCam = levelScene.level.height * LevelScene.cellSize
                        - SimulatorOptions.VISUAL_COMPONENT_HEIGHT;

            Mario mario = levelScene.mario;
            int marioXPixel = (int) mario.x - mario.xPicO;
            int marioYPixel = (int) mario.y - mario.yPicO;

            if (priorEntities[0] != null) {
                Sprite sprite = priorEntities[0].sprite;
                int xPixel = (int) sprite.x - sprite.xPicO;
                int yPixel = (int) sprite.y - sprite.yPicO;
                float distance = (float) FastMath.sqrt(FastMath.pow(priorEntities[0].dX, 2) + FastMath.pow(priorEntities[0].dY, 2)) / 100;
                graphics.setColor(Color.RED);

                graphics.translate(-xCam, -yCam);
                graphics.drawRect(xPixel, yPixel, sprite.wPic, sprite.hPic);
                graphics.drawLine(marioXPixel + mario.wPic / 2, marioYPixel + mario.hPic / 2, xPixel + sprite.wPic / 2, yPixel + sprite.hPic / 2);
                graphics.drawString("Distance: " + distance, (xPixel + marioXPixel) / 2, (yPixel + marioYPixel) / 2);
                graphics.translate(xCam, yCam);
            }
            if (priorEntities[1] != null) {
                Sprite sprite = priorEntities[1].sprite;
                int xPixel = (int) sprite.x - sprite.xPicO;
                int yPixel = (int) sprite.y - sprite.yPicO;
                float distance = (float) FastMath.sqrt(FastMath.pow(priorEntities[1].dX, 2) + FastMath.pow(priorEntities[1].dY, 2)) / 100;
                graphics.setColor(Color.ORANGE);

                graphics.translate(-xCam, -yCam);
                graphics.drawRect(xPixel, yPixel, sprite.wPic, sprite.hPic);
                graphics.drawLine(marioXPixel + mario.wPic / 2, marioYPixel + mario.hPic / 2, xPixel + sprite.wPic / 2, yPixel + sprite.hPic / 2);
                graphics.drawString("Distance: " + distance, (xPixel + marioXPixel) / 2, (yPixel + marioYPixel) / 2);
                graphics.translate(xCam, yCam);
            }
            if (priorEntities[2] != null) {
                Sprite sprite = priorEntities[2].sprite;
                int xPixel = (int) sprite.x - sprite.xPicO;
                int yPixel = (int) sprite.y - sprite.yPicO;
                float distance = (float) FastMath.sqrt(FastMath.pow(priorEntities[2].dX, 2) + FastMath.pow(priorEntities[2].dY, 2)) / 100;
                graphics.setColor(Color.YELLOW);

                graphics.translate(-xCam, -yCam);
                graphics.drawRect(xPixel, yPixel, sprite.wPic, sprite.hPic);
                graphics.drawLine(marioXPixel + mario.wPic / 2, marioYPixel + mario.hPic / 2, xPixel + sprite.wPic / 2, yPixel + sprite.hPic / 2);
                graphics.drawString("Distance: " + distance, (xPixel + marioXPixel) / 2, (yPixel + marioYPixel) / 2);
                graphics.translate(xCam, yCam);
            }
        }
    }
}
