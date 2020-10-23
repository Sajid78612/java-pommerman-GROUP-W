package players.groupW;

import core.GameState;
import players.Player;
import players.optimisers.ParameterSet;
import players.optimisers.ParameterizedPlayer;
import utils.Types;

import java.util.ArrayList;

public class MyMCTSPlayer extends ParameterizedPlayer {

    private static final ArrayList<Types.ACTIONS> actions = Types.ACTIONS.all();

    // Our MCTS parameter set
    MyMCTSParams params;

    // Use later to measure avg duration of moves per game
    private ArrayList<Long> durations = new ArrayList<>();
    // Counter to print method execution duration
    private int printDurationCounter = 0;
    // Print duration every n method executions
    private int n = 50;

    public MyMCTSPlayer(long seed, int pId) {
        this(seed, pId, new MyMCTSParams());
    }

    MyMCTSPlayer(long seed, int pId, ParameterSet params) {
        super(seed, pId, params);
        reset(seed, pId);
    }

    @Override
    public void reset(long l, int i) {
        this.params = (MyMCTSParams) getParameters();
        if (this.params == null) {
            this.params = new MyMCTSParams();
            super.setParameters(this.params);
        }
    }

    @Override
    public Types.ACTIONS act(GameState gameState) {
        long startTime = System.nanoTime();

        // Number of actions available
        int numActions = actions.size();

        // Pass current game state to root node for MCTS search
        MyTreeNode rootNode = new MyTreeNode(params, numActions,
                actions.toArray(new Types.ACTIONS[0]) // This just converts the list of all actions to an array
        );
        rootNode.setCurrentGameState(gameState);

        // Find best action
        rootNode.search(); // This call will terminate after a certain amount of time
        int bestAction = rootNode.findBestAction();

        long endTime = System.nanoTime();
        long duration = (endTime - startTime) / 1000000; // ms
        durations.add(duration);

        // Print every nth duration
        printDurationCounter++;
        if(printDurationCounter % n == 0)
            System.out.println("Duration: " + duration + "ms");

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
}
