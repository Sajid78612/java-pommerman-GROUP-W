package players.groupW.EMCTS;

import core.GameState;
import players.Player;
import players.optimisers.ParameterizedPlayer;
import utils.Types;

import java.util.ArrayList;
import java.util.Random;

public class EMCTSPlayer extends ParameterizedPlayer {

    // Static final list of all the allowed actions in the game
    private static final ArrayList<Types.ACTIONS> actions = Types.ACTIONS.all();

    // Our EMCTS parameter set
    EMCTSParams params;

    // Placeholder for the currently best genome
    private Types.ACTIONS[] currentGenome;

    // Java random number engine
    private Random random = new Random();

    public EMCTSPlayer(long seed, int playerID) {
        super(seed, playerID);
        reset(seed, playerID);
    }

    @Override
    public Types.ACTIONS act(GameState gameState) {
        // Pass current game state to root node for EMCTS search
        EMCTSNode rootNode;
        if(currentGenome == null){
            rootNode = new EMCTSNode(params,
                    actions.toArray(new Types.ACTIONS[0]) // This just converts the list of all actions to an array
            );
            // If root node initialise variables (e.g. score board)
            rootNode.init();
        } else{
            // If it is not the first iteration, use values from previous iterations
            rootNode = new EMCTSNode(params, actions.toArray(new Types.ACTIONS[0]), currentGenome);
        }
        // Apply current game state
        rootNode.setCurrentGameState(gameState);

        // Find best genome (and thereby action) for this turn
        rootNode.search(); // This call will terminate after a certain number of iterations
        // Get the best genome
        currentGenome = rootNode.findBestAction();
        // Take the first action of the best genome
        int bestAction = Types.ACTIONS.all().indexOf(currentGenome[0]);

        int a = 2;
        if(currentGenome[0] == null){
            a=2;
        }
        if(bestAction == -1){
            a = 2;
        }

        // Shift current genome
        for(int i = 0; i < currentGenome.length; i++){
            if(i == currentGenome.length - 1){
                currentGenome[i] = actions.get(random.nextInt(actions.size()));
                break;
            }
            currentGenome[i] = currentGenome[i + 1];
        }

        return actions.get(bestAction);
    }

    @Override
    public int[] getMessage() {
        return new int[0];
    }

    @Override
    public Player copy() {
        return new EMCTSPlayer(seed, playerID);
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
