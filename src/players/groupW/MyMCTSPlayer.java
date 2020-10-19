package players.groupW;

import core.GameState;
import players.Player;
import players.mcts.MCTSParams;
import players.mcts.SingleTreeNode;
import players.optimisers.ParameterSet;
import players.optimisers.ParameterizedPlayer;
import utils.Types;

import java.util.ArrayList;

public class MyMCTSPlayer extends ParameterizedPlayer {

    private static final ArrayList<Types.ACTIONS> actions = Types.ACTIONS.all();

    // Our MCTS parameter set
    MyMCTSParams params;

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
