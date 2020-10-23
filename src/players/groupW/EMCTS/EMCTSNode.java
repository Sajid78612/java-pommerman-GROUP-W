package players.groupW.EMCTS;

import core.GameState;
import players.groupW.MyTreeNode;
import players.heuristics.CustomHeuristic;
import players.heuristics.StateHeuristic;
import utils.Types;
import utils.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

public class EMCTSNode {
    private EMCTSNode parent;
    private ArrayList<EMCTSNode> children;

    // The game state
    private GameState gameState;

    // MCTS parameters
    private EMCTSParams params;

    // The number of actions allowed (branching factor of the search tree)
    private Types.ACTIONS[] actions;

    // Current depth in the search tree
    private int currentDepth;

    private static final Random random = new Random();

    private int childIndex;

    private int forwardModelCallsCount;

    private StateHeuristic stateHeuristic;

    private double totalValue;

    private int numberOfVisits;

    private double[] bounds = new double[]{Double.MAX_VALUE, -Double.MAX_VALUE};

    private int numIterations = 0;

    private double[] raveVisits;
    private double[] raveWins;

    // EMCTS stuff
    private Types.ACTIONS[] genome;
    private static final int GENOME_LENGTH = 5;
    private ArrayList<Tuple<Types.ACTIONS[], Double>> scoreBoard;

    class Tuple<X, Y> {
        public final X x;
        public final Y y;
        public Tuple(X x, Y y) {
            this.x = x;
            this.y = y;
        }
    }

    /**
     * Constructor for our tree node
     * @param params The set of MCTS parameters
     * @param actions We pass the static list of actions from the top to save memory
     */
    EMCTSNode(EMCTSParams params, Types.ACTIONS[] actions) {
        this(params, actions, null, null, 0, new double[actions.length], new double[actions.length], new Types.ACTIONS[GENOME_LENGTH]);
    }

    // Called by EMCTS Player
    EMCTSNode(EMCTSParams params, Types.ACTIONS[] actions, Types.ACTIONS[] genome) {
        this(params, actions, null, null, 0, new double[actions.length], new double[actions.length], genome);
    }

    private EMCTSNode(EMCTSParams params,
                       Types.ACTIONS[] actions,
                       EMCTSNode parent,
                       StateHeuristic stateHeuristic,
                       int forwardModelCallsCount,
                       double[] raveVisits,
                       double[] raveWins,
                      Types.ACTIONS[] genome
    ){
        this.params = params;
        this.forwardModelCallsCount = forwardModelCallsCount;
        this.parent = parent;
        this.actions = actions;
        this.children = new ArrayList<>();
        this.raveVisits = raveVisits;
        this.raveWins = raveWins;
        this.genome = genome;

        if(parent != null) {
            currentDepth = parent.currentDepth + 1;
            this.stateHeuristic = stateHeuristic;
            this.scoreBoard = parent.scoreBoard;
        }
        else{
            currentDepth = 0;
            this.scoreBoard = new ArrayList<>();
        }
    }

    public void init(){
        // Init genome randomly
        genome = new Types.ACTIONS[GENOME_LENGTH];
        for(int i = 0; i < genome.length; i++){
            genome[i] = Types.ACTIONS.all().get(random.nextInt(actions.length));
        }
    }

    /**
     * Performs the MCTS search
     */
    public void search(){
        boolean stop = false;
        numIterations = 0;

        while(!stop){
            // Copy game state to isolate changes
            GameState state = gameState.copy();

            // Simulate
            EMCTSNode selected = selectChildNode(state);

            // Basic stopping condition for now
            numIterations++;
            stop = numIterations >= params.maxNumIterations;
        }
    }

    public Types.ACTIONS[] findBestAction() {
        // Loop over the score board and get the best genome
        Types.ACTIONS[] bestGenome = new Types.ACTIONS[GENOME_LENGTH];
        double bestScore = -Double.MAX_VALUE;
        for(var tuple : scoreBoard){
            if(tuple.y > bestScore){
                bestScore = tuple.y;
                bestGenome = tuple.x;
            }
        }

        return bestGenome;
    }

    EMCTSNode selectChildNode(GameState state){
        EMCTSNode current = this;

        while (!state.isTerminal() && current.currentDepth < params.maxRolloutDepth)
        {
            // Expand => go further down the tree
            if (current.notFullyExpanded()) {
                return current.expandNode(state);
            }
            // Done expanding, evaluate leaf nodes
            else {
                current = current.evaluate(state);
            }
        }

        return current;
    }

    /**
     * Select the next move and rolllll
     * @param state Game state at the node
     * @return the best move (child of this node)
     */
    private EMCTSNode expandNode(GameState state) {
        // Randomly pick a gene to mutate
        // TODO: Important - change random picking to heuristic

        int genePosition = random.nextInt(GENOME_LENGTH);
        int childGene = random.nextInt(actions.length);

        Types.ACTIONS[] childGenome = new Types.ACTIONS[GENOME_LENGTH];
        System.arraycopy(genome, 0, childGenome, 0, GENOME_LENGTH);
        childGenome[genePosition] = actions[childGene];

        // Roll the state
        // rollState(state, actions[bestAction]);

        // Branch out
        EMCTSNode childNode = new EMCTSNode(
                params,
                actions,
                this,
                stateHeuristic,
                forwardModelCallsCount,
                this.raveVisits,
                this.raveWins,
                childGenome
        );

        children.add(childNode);
        return childNode;
    }

    // Evaluate the leaf node
    private EMCTSNode evaluate(GameState state) {
        EMCTSNode best = null;
        double bestScore = -Double.MAX_VALUE;

        for(EMCTSNode child : children){
            // Copy game state
            GameState copy = state.copy();
            // Roll out
            for(Types.ACTIONS action : child.genome){
                rollState(copy, action);
                if(state.isTerminal())
                    break;
            }

            double result = Utils.noise(stateHeuristic.evaluateState(state), params.epsilon, random.nextDouble());
            scoreBoard.add(new Tuple<>(child.genome, result));

            if(result > bestScore){
                best = child;
                bestScore = result;
            }
        }

        return best;
    }

    private void rollState(GameState gs, Types.ACTIONS act)
    {
        //Simple, all random first, then my position.
        int nPlayers = 4;
        Types.ACTIONS[] playerActions = new Types.ACTIONS[4];

        // Get our player ID
        int playerId = gs.getPlayerId() - Types.TILETYPE.AGENT0.getKey();

        // Assign player action for rollout
        for(int i = 0; i < nPlayers; ++i)
        {
            if(playerId == i)
            {
                // Apply our move
                playerActions[i] = act;
            }else {
                // Pick random move for opponents
                // TODO this could be improved by a heuristic
                int actionIdx = random.nextInt(gs.nActions());
                playerActions[i] = Types.ACTIONS.all().get(actionIdx);
            }
        }

        // Roll out game state with
        gs.next(playerActions);
    }

    /**
     * Whether the current search tree is fully expanded
     * @return
     */
    private boolean notFullyExpanded() {
        return children.size() < params.branchingFactor;
    }

    /**
     * Apply current game state to the node
     * @param gameState The incoming game state
     */
    public void setCurrentGameState(GameState gameState){
        this.gameState = gameState;

        // TODO Change heuristic
        // Not sure whether we're allowed to use their heuristics
        this.stateHeuristic = new CustomHeuristic(gameState);
    }
}
