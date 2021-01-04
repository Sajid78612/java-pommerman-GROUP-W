package players.groupW.EMCTS;

import core.GameState;
import players.heuristics.CustomHeuristic;
import players.heuristics.StateHeuristic;
import utils.Types;
import utils.Utils;

import java.util.ArrayList;
import java.util.Random;

public class EMCTSNode {
    // Parent node
    private final EMCTSNode parent;
    // Children of the current node. The length of this list is determined
    // by the EMCTS param "branchingFactor"
    private final ArrayList<EMCTSNode> children;

    // The game state
    private GameState gameState;

    // EMCTS parameters
    private final EMCTSParams params;

    // Available actions
    private final Types.ACTIONS[] actions;

    // Current depth in the search tree
    private final int currentDepth;

    // Java random number engine
    private static final Random random = new Random();

    // Heuristic for evaluating game states
    private StateHeuristic stateHeuristic;

    private double[] raveVisits;
    private double[] raveWins;

    // The genome (sequence of actions)
    private Types.ACTIONS[] genome;

    // The length of the genome
    private static final int GENOME_LENGTH = 5;

    // List of all the genomes of leaf nodes and their scores
    private ArrayList<Tuple<Types.ACTIONS[], Double>> scoreBoard;

    /**
     * Helper class for storing genomes and their scores
     * @param <X>
     * @param <Y>
     */
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
        this(params, actions, null, null, new double[actions.length], new double[actions.length], new Types.ACTIONS[GENOME_LENGTH]);
    }

    /**
     * Constructor called by EMCTS Player
     * @param params EMCTS params
     * @param actions Available actions
     * @param genome The current genome
     */
    EMCTSNode(EMCTSParams params, Types.ACTIONS[] actions, Types.ACTIONS[] genome) {
        this(params, actions, null, null, new double[actions.length], new double[actions.length], genome);
    }

    private EMCTSNode(EMCTSParams params,
                       Types.ACTIONS[] actions,
                       EMCTSNode parent,
                       StateHeuristic stateHeuristic,
                       double[] raveVisits,
                       double[] raveWins,
                      Types.ACTIONS[] genome
    ){
        this.params = params;
        this.parent = parent;
        this.actions = actions;
        this.children = new ArrayList<>();
        this.raveVisits = raveVisits;
        this.raveWins = raveWins;
        this.genome = genome;

        // If there is a parent node increase the depth and get state heuristic and score board from parent
        if(parent != null) {
            currentDepth = parent.currentDepth + 1;
            this.stateHeuristic = stateHeuristic;
            this.scoreBoard = parent.scoreBoard;
        }
        // If this node is the root node, initialise with depth 0 and empty score board
        else{
            currentDepth = 0;
            this.scoreBoard = new ArrayList<>();
        }
    }

    /**
     * Initialise function called only for root node
     */
    public void init(){
        // Initialise genome randomly
        genome = new Types.ACTIONS[GENOME_LENGTH];
        for(int i = 0; i < genome.length; i++){
            genome[i] = Types.ACTIONS.all().get(random.nextInt(actions.length));
        }
    }

    /**
     * Performs the EMCTS search until stopping condition is met
     */
    public void search(){
        // Stop flag
        boolean stop = false;
        // Number of iterations
        int numIterations = 0;

        // Store the current best in parameters to reduce search time during evaluation
        params.currentBest = -Double.MAX_VALUE;

        // Create EMCTS trees until the stopping condition is met
        while(!stop){
            // Copy game state to isolate changes
            GameState state = gameState.copy();

            // Simulate
            emctsSearch(state, this);

            // Basic stopping condition for now
            numIterations++;
            stop = numIterations >= params.maxNumIterations;
        }
    }

    public Types.ACTIONS[] findBestAction() {
        // Loop over the score board and get the best genome
//        Types.ACTIONS[] bestGenome = new Types.ACTIONS[GENOME_LENGTH];
//        double bestScore = -Double.MAX_VALUE;
//        for(Tuple<Types.ACTIONS[], Double> tuple : scoreBoard){
//            if(tuple.y > bestScore){
//                bestScore = tuple.y;
//                bestGenome = tuple.x;
//            }
//        }
//
//        return bestGenome;
        return params.currentBestGenome;
    }

    /**
     * Create the EMCTS search tree by expanding nodes depth first
     * @param state The game state
     * @param node the current node in the search tree
     */
    void emctsSearch(GameState state, EMCTSNode node){
        if (!state.isTerminal() && node.currentDepth < params.maxRolloutDepth - 1)
        {
            // Expand => go further down the tree
            for(int i = 0; i < params.branchingFactor; i++){
                EMCTSNode child = node.expandNode(state);
                child.emctsSearch(state, child);
            }
        } else if(node.currentDepth == params.maxRolloutDepth - 1) {
            // Create last layer of children => leaf nodes
            for(int i = 0; i < params.branchingFactor; i++){
                node.expandNode(state);
            }
            // Evaluate children
            node.evaluate(state);
        }
    }

    /**
     * Create a child node with a modified genome
     * @param state Game state at the node
     * @return the best move (child of this node)
     */
    private EMCTSNode expandNode(GameState state) {
        // Randomly pick a gene to mutate
        int genePosition = random.nextInt(GENOME_LENGTH);
        int childGene = random.nextInt(actions.length);

        // Copy parent genome, then apply the mutation
        Types.ACTIONS[] childGenome = new Types.ACTIONS[GENOME_LENGTH];
        System.arraycopy(genome, 0, childGenome, 0, GENOME_LENGTH);
        childGenome[genePosition] = actions[childGene];

        // Create child node
        EMCTSNode childNode = new EMCTSNode(
                params,
                actions,
                this,
                stateHeuristic,
                this.raveVisits,
                this.raveWins,
                childGenome
        );

        // Add created node to the parent node's children
        children.add(childNode);
        return childNode;
    }

    /**
     * Evaluate a leaf node's fitness using the state heuristic
     * @param state The game state that serves as the basis for evaluation
     */
    private void evaluate(GameState state) {
        for(EMCTSNode child : children){
            // Copy game state
            GameState copy = state.copy();
            // Roll out moves
            for(Types.ACTIONS action : child.genome){
                rollState(copy, action);
                if(copy.isTerminal())
                    break;
            }

            // Add noise to break ties
            double result = Utils.noise(stateHeuristic.evaluateState(copy), params.epsilon, random.nextDouble());
            if(result > params.currentBest){
                // Add leaf node value to score board
                scoreBoard.add(new Tuple<>(child.genome, result));
                params.currentBest = result;
                params.currentBestGenome = child.genome;
            }
        }
    }

    /**
     * Apply moves to a game state to simulate resulting states
     * @param gameState The current game state
     * @param act The action of our agent
     */
    private void rollState(GameState gameState, Types.ACTIONS act)
    {
        // Simple heuristic for opponents: Random moves
        // Assume 4 players
        int nPlayers = 4;
        // Create array for player moves (both this agent and opponents)
        Types.ACTIONS[] playerActions = new Types.ACTIONS[4];

        // Get our player ID
        int playerId = gameState.getPlayerId() - Types.TILETYPE.AGENT0.getKey();

        // Assign player action for rollout
        for(int i = 0; i < nPlayers; ++i)
        {
            if(playerId == i)
            {
                // Apply our move
                playerActions[i] = act;
            }else {
                // Pick random move for opponents
                int actionIdx = random.nextInt(gameState.nActions());
                playerActions[i] = Types.ACTIONS.all().get(actionIdx);
            }
        }

        // Roll out game state with
        gameState.next(playerActions);
    }

    /**
     * Apply current game state to the node
     * @param gameState The incoming game state
     */
    public void setCurrentGameState(GameState gameState){
        this.gameState = gameState;
        this.stateHeuristic = new CustomHeuristic(gameState);
    }
}
