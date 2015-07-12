package com.marionette.evolver.supermariobros;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.List;

import org.apache.commons.math3.util.FastMath;
import org.uncommons.watchmaker.framework.FitnessEvaluator;

import com.grapeshot.halfnes.CPURAM;
import com.grapeshot.halfnes.NES;
import com.grapeshot.halfnes.ui.GUIImpl;
import com.javaneat.genome.ConnectionGene;
import com.javaneat.genome.NEATGenome;
import com.javaneat.phenome.NEATPhenome;

public class SuperMarioBrosFitness implements FitnessEvaluator<NEATGenome>
{
	public static double[] unwind2DArray(int[][] arr)
	{
		double[] out = new double[arr.length * arr[0].length];
		int i = 0;
		for (int x = 0; x < arr[0].length; x++)
		{
			for (int y = 0; y < arr.length; y++)
			{
				out[i] = arr[y][x];
				i++;
			}
		}
		return out;
	}

	private ThreadLocal<NES>	nes	= new ThreadLocal<NES>();

	@Override
	public double getFitness(NEATGenome candidate, List<? extends NEATGenome> population)
	{
		// long startTime = System.nanoTime();

		boolean movesRight = false; // Throw out ones that don't move right before testing
		boolean jumps = false;
		for (ConnectionGene gene : candidate.getConnectionGeneList())
		{
			if (gene.getToNode() == candidate.getManager().getOutputOffset() + 3)
			{
				movesRight = true;
				if (jumps) break;
			}
			if (gene.getToNode() == candidate.getManager().getOutputOffset() + 3)
			{
				jumps = true;
				if (movesRight) break;
			}
		}
		if (!movesRight)
		{
			candidate.setScore(500 - candidate.getConnectionGeneList().size() * 10);
			return candidate.getScore();
		}

		NEATPhenome network = new NEATPhenome(candidate);
		if (nes.get() == null)
		{
			nes.set(new NES(true));
			nes.get().loadROM("C:\\Users\\Mitchell\\Desktop\\fceux-2.2.2-win32\\ROMs\\Super Mario Bros..nes");
		}
		NES nes = this.nes.get();
		nes.reset();
		// NESFitnessEvaluator.loadSavestate(nes);

		final GUIImpl gui = ((GUIImpl) nes.getGUI());
		CPURAM cpuram = nes.getCPURAM();
		final KeyListener input = gui.getKeyListeners()[0];

		final KeyEvent U = new KeyEvent(gui, 0, 0, 0, KeyEvent.VK_UP, '^');
		final KeyEvent D = new KeyEvent(gui, 0, 0, 0, KeyEvent.VK_DOWN, 'v');
		final KeyEvent L = new KeyEvent(gui, 0, 0, 0, KeyEvent.VK_LEFT, '<');
		final KeyEvent R = new KeyEvent(gui, 0, 0, 0, KeyEvent.VK_RIGHT, '>');

		final KeyEvent A = new KeyEvent(gui, 0, 0, 0, KeyEvent.VK_X, 'A');
		final KeyEvent B = new KeyEvent(gui, 0, 0, 0, KeyEvent.VK_Z, 'B');
		final KeyEvent SELECT = new KeyEvent(gui, 0, 0, 0, KeyEvent.VK_SHIFT, 'E');
		final KeyEvent START = new KeyEvent(gui, 0, 0, 0, KeyEvent.VK_ENTER, 'T');

		for (int i = 0; i < 31; i++)
			// Exact frame number until it can begin.
			nes.frameAdvance();

		input.keyPressed(START);
		nes.frameAdvance();
		input.keyReleased(START);

		int fitness = 0;
		int maxDistance = 0;
		int timeout = 0;

		while (true)
		{
			input.keyReleased(U);
			input.keyReleased(D);
			input.keyReleased(L);
			input.keyReleased(R);
			input.keyReleased(A);
			input.keyReleased(B);
			input.keyReleased(SELECT);
			input.keyReleased(START);

			cpuram = nes.getCPURAM();

			int score = 0;
			int time = 0;
			byte world = (byte) cpuram.read(0x075F);
			byte level = (byte) cpuram.read(0x0760);
			byte lives = (byte) (cpuram.read(0x075A) + 1);
			int marioX = cpuram.read(0x6D) * 0x100 + cpuram.read(0x86);
			int marioY = cpuram.read(0x03B8) + 16;
			int marioState = cpuram.read(0x000E);
			for (int i = 0x07DD; i <= 0x07E2; i++)
				score += cpuram._read(i) * FastMath.pow(10, (0x07E2 - i + 1));
			for (int i = 0x07F8; i <= 0x07FA; i++)
				time += cpuram._read(i) * FastMath.pow(10, (0x07FA - i));

			int points = (score / 5) + (time * 10) + (marioX / 4) + (lives * 500) + (level * 250) + (world * 2000);

			timeout++;
			if (marioX > maxDistance)
			{
				maxDistance = marioX;
				timeout = 0;
			}
			// System.out.println("Lives: " + lives + " Timeout: " + timeout + " Distance: " + marioX);
			if (lives <= 2 || timeout > 60 || marioState == 0x0B)
			{
				fitness = points;
				break;
			}

			final int[][] vision = new int[13][13];

			for (int dx = -vision[0].length / 2; dx < vision[0].length / 2; dx += 1)
				for (int dy = -vision.length / 2; dy < vision.length / 2; dy += 1)
				{
					int x = marioX + (dx * 16) + 8;
					int y = marioY + (dy * 16) - 16;
					int page = (int) FastMath.floor(x / 256) % 2;
					int subx = (int) FastMath.floor((x % 256) / 16);
					int suby = (int) FastMath.floor((y - 32) / 16);
					int addr = 0x500 + page * 13 * 16 + suby * 16 + subx;
					if (suby >= 13 || suby < 0)
					{
						// System.out.println("Outside level.");
						vision[dy + (vision.length / 2)][dx + (vision[0].length / 2)] = 0;
					}
					else
					{
						// System.out.println("Block data at " + dx + ", " + dy + ": " + nes.cpuram.read(addr));
						vision[dy + (vision.length / 2)][dx + (vision[0].length / 2)] = cpuram.read(addr);
					}
				}

			for (int i = 0; i <= 4; i++)
			{
				int enemy = cpuram.read(0xF + i);
				if (enemy != 0)
				{
					int ex = cpuram.read(0x6E + i) * 0x100 + cpuram.read(0x87 + i);
					int ey = cpuram.read(0xCF + i) + 24;
					int enemyMarioDeltaX = (ex - marioX) / 16;
					int enemyMarioDeltaY = (ey - marioY) / 16;
					try
					{
						vision[enemyMarioDeltaY + (vision.length / 2)][enemyMarioDeltaX + (vision[0].length / 2)] = -enemy;
					}
					catch (ArrayIndexOutOfBoundsException e)
					{
					}
				}
			}

			double[] visionunwound = unwind2DArray(vision);
			double[] reactions = network.stepTime(visionunwound);

			if (reactions[0] > 0) input.keyPressed(U);
			if (reactions[1] > 0) input.keyPressed(D);
			if (reactions[2] > 0) input.keyPressed(L);
			if (reactions[3] > 0) input.keyPressed(R);
			if (reactions[4] > 0) input.keyPressed(A);
			if (reactions[5] > 0) input.keyPressed(B);
			// if (reactions[6] > 0) input.keyPressed(SELECT);
			// if (reactions[7] > 0) input.keyPressed(START);

			nes.frameAdvance();
		}
		fitness -= candidate.getConnectionGeneList().size() * 5;
		// fitness -= 5400; // The approximate minimum
		// System.out.println("Finished one evaluation that took " + (System.nanoTime() - startTime) / 1000000000f + " seconds.");
		fitness = fitness >= 0 ? fitness : 0;

		candidate.setScore(fitness);
		return candidate.getScore();
	}

	@Override
	public boolean isNatural()
	{
		return true;
	}
}