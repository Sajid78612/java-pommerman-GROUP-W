package players.groupW.EMCTS;

import core.GameState;
import players.Player;
import players.groupW.MyMCTSParams;
import players.groupW.MyTreeNode;
import players.optimisers.ParameterizedPlayer;
import utils.Types;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Random;

public class EMCTSPlayer extends ParameterizedPlayer {

    private static final ArrayList<Types.ACTIONS> actions = Types.ACTIONS.all();

    // Our MCTS parameter set
    EMCTSParams params;

    // Use later to measure avg duration of moves per game
    private ArrayList<Long> durations = new ArrayList<>();
    // Counter to print method execution duration
    private int printDurationCounter = 0;
    // Print duration every n method executions
    private int n = 50;

    private Types.ACTIONS[] currentGenome;

    private Random random = new Random();

    public EMCTSPlayer(long seed, int playerID) {
        super(seed, playerID);
        reset(seed, playerID);
    }

    @Override
    public Types.ACTIONS act(GameState gameState) {
        long startTime = System.nanoTime();

        // Number of actions available
        int numActions = actions.size();

        // Pass current game state to root node for MCTS search
        EMCTSNode rootNode;
        if(currentGenome == null){
            rootNode = new EMCTSNode(params,
                    actions.toArray(new Types.ACTIONS[0]) // This just converts the list of all actions to an array
            );
            rootNode.init();
        } else{
            rootNode = new EMCTSNode(params, actions.toArray(new Types.ACTIONS[0]), currentGenome);
        }
        rootNode.setCurrentGameState(gameState);

        // Find best action
        rootNode.search(); // This call will terminate after a certain amount of time
        currentGenome = rootNode.findBestAction();
        int bestAction = Types.ACTIONS.all().indexOf(currentGenome[0]);

        // Shift current genome
        for(int i = 0; i < currentGenome.length; i++){
            if(i == currentGenome.length - 1){
                currentGenome[i] = actions.get(random.nextInt(actions.size()));
                break;
            }
            currentGenome[i] = currentGenome[i + 1];
        }

        long endTime = System.nanoTime();
        long duration = (endTime - startTime) / 1000000; // ms
        durations.add(duration);

        // Print every nth duration
//        printDurationCounter++;
//        if(printDurationCounter % n == 0)
//            System.out.println("Duration: " + duration + "ms");

        return actions.get(bestAction);
    }

    @Override
    public int[] getMessage() {
        return new int[0];
    }

    @Override
    public Player copy() {
        return null;
    }

    @Override
    public void reset(long l, int i) {
        this.params = (EMCTSParams) getParameters();
        if (this.params == null) {
            this.params = new EMCTSParams();
            super.setParameters(this.params);
        }
    }
}
