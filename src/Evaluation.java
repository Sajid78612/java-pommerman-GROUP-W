import core.Game;
import players.KeyController;
import players.Player;
import players.SimplePlayer;
import players.groupW.EMCTS.EMCTSPlayer;
import players.mcts.MCTSParams;
import players.mcts.MCTSPlayer;
import players.rhea.RHEAPlayer;
import players.rhea.utils.Constants;
import players.rhea.utils.RHEAParams;
import utils.Types;

import java.util.ArrayList;
import java.util.Random;

public class Evaluation {
    public static void main(String[] args) {

        // Game parameters
        long seed = System.currentTimeMillis();
        int boardSize = Types.BOARD_SIZE;
        boolean useSeparateThreads = false;
        Random random = new Random();

        // Key controllers for human player s (up to 2 so far).
        KeyController ki1 = new KeyController(true);
        KeyController ki2 = new KeyController(false);

        // Partial observability: Can be 0, 1, 2 (PO) or -1 for full observability
        int visionRange = -1;
        Types.DEFAULT_VISION_RANGE = visionRange;
        Game game = new Game(seed, boardSize, Types.GAME_MODE.FFA, "");

        // Create players
        ArrayList<Player> players = new ArrayList<>();
        int playerID = Types.TILETYPE.AGENT0.getKey();

        MCTSParams mctsParams = new MCTSParams();
        mctsParams.stop_type = mctsParams.STOP_ITERATIONS;
        mctsParams.heuristic_method = mctsParams.CUSTOM_HEURISTIC;

        RHEAParams rheaParams = new RHEAParams();
        rheaParams.heurisic_type = Constants.CUSTOM_HEURISTIC;

        // Our player
        players.add(new EMCTSPlayer(seed, playerID++));

        players.add(new SimplePlayer(seed, playerID++));
        players.add(new SimplePlayer(seed, playerID++));

        players.add(new RHEAPlayer(seed, playerID++, rheaParams));
//        players.add(new MCTSPlayer(seed, playerID++, new MCTSParams()));
//        players.add(new RHEAPlayer(seed, playerID++, rheaParams));

        // Make sure we have exactly NUM_PLAYERS players
        assert players.size() == Types.NUM_PLAYERS : "There should be " + Types.NUM_PLAYERS +
                " added to the game, but there are " + players.size();


        // Assign players and run the game.
        game.setPlayers(players);

        //Run a single game with the players
//        Run.runGame(game, ki1, ki2, useSeparateThreads);

        // Generate 10 seeds, then run 5 times per seed
        long[] seeds = new long[10];
        for(int i = 0; i < 10; i++)
            seeds[i] = random.nextLong();

        /* Run with no visuals, N Times: */
        int N = 5;
//        Run.runGames(game, new long[]{seed}, N, useSeparateThreads);
        Run.runGames(game, seeds, N, useSeparateThreads);

    }
}
