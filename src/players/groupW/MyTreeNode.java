package players.groupW;

import core.GameState;
import players.heuristics.CustomHeuristic;
import players.heuristics.StateHeuristic;
import utils.Types;
import utils.Utils;
import utils.Vector2d;

import java.util.ArrayList;
import java.util.Random;

public class MyTreeNode {
    // Parent & children nodes
    private MyTreeNode parent;
    private MyTreeNode[] children;

    // The game state
    private GameState gameState;

    // MCTS parameters
    private MyMCTSParams params;

    // The number of actions allowed (branching factor of the search tree)
    private int numActions;
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

    /**
     * Constructor for our tree node
     * @param params The set of MCTS parameters
     * @param numActions The number of actions allowed, aka branching factor
     * @param actions We pass the static list of actions from the top to save memory
     */
    MyTreeNode(MyMCTSParams params, int numActions, Types.ACTIONS[] actions) {
        this(params, numActions, actions, null, -1, null, 0, new double[actions.length], new double[actions.length]);
    }

    private MyTreeNode(MyMCTSParams params,
                       int numActions,
                       Types.ACTIONS[] actions,
                       MyTreeNode parent,
                       int childIndex,
                       StateHeuristic stateHeuristic,
                       int forwardModelCallsCount,
                       double[] raveVisits,
                       double[] raveWins
    ){
        this.params = params;
        this.forwardModelCallsCount = forwardModelCallsCount;
        this.parent = parent;
        this.numActions = numActions;
        this.actions = actions;
        this.children = new MyTreeNode[numActions];
        this.childIndex = childIndex;
        this.raveVisits = raveVisits;
        this.raveWins = raveWins;

        if(parent != null) {
            currentDepth = parent.currentDepth + 1;
            this.stateHeuristic = stateHeuristic;
        }
        else{
            currentDepth = 0;
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

            // Select a child node
            MyTreeNode selected = selectChildNode(state);

            // Simulate
            double result = selected.rollOut(state);

            // Backpropagate
            backpropagate(selected, result);

            // Basic stopping condition for now
            numIterations++;
            stop = numIterations >= params.maxNumIterations;
        }
    }

    /**
     * Returns the best action to take after the MCTS search is completed
     * @return
     */
    public int findBestAction() {
        if (children == null || children.length == 0)
            return -1; // null or empty

        int largestIdx = 0;
        for (int i = 1; i < children.length; i++ )
        {
            if (children[i].numberOfVisits > children[largestIdx].numberOfVisits)
                largestIdx = i;
        }

        return largestIdx;
    }

    MyTreeNode selectChildNode(GameState state){
        MyTreeNode current = this;

        while (!state.isTerminal() && current.currentDepth < params.maxRolloutDepth)
        {
            // Expand => go further down the tree
            if (current.notFullyExpanded()) {
                return current.expandNode(state);
            }
            // Done expanding, collect upper confidence bound values
            else {
                current = current.upperConfidenceBound(state);
            }
        }

        return current;
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

    /**
     * Select the next move and rolllll
     * @param state Game state at the node
     * @return the best move (child of this node)
     */
    private MyTreeNode expandNode(GameState state) {

        int bestAction = 0;
        double bestValue = -1;

        // Pick a random action
        for (int i = 0; i < children.length; i++) {
            double x = random.nextDouble();
            if (x > bestValue && children[i] == null) {
                bestAction = i;
                bestValue = x;
            }
        }

        // Roll the state
        rollState(state, actions[bestAction]);

        MyTreeNode treeNode = new MyTreeNode(
                params,
                numActions,
                actions,
                this,
                bestAction,
                stateHeuristic,
                forwardModelCallsCount,
                this.raveVisits,
                this.raveWins
                );

        children[bestAction] = treeNode;
        return treeNode;
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

    // Note uct is upper confidence bound applied to trees
    private MyTreeNode upperConfidenceBound(GameState state) {
        MyTreeNode selected = null;
        double bestValue = -Double.MAX_VALUE;

        // RAVE
        double R = 2;

        for (MyTreeNode child : this.children)
        {
            double childValue =  child.totalValue / (child.numberOfVisits + params.epsilon);

            childValue = Utils.normalise(childValue, bounds[0], bounds[1]);

            // Normal
//            double uctValue = childValue + 2 * Math.sqrt(Math.log(this.numberOfVisits + 1) / (child.numberOfVisits + params.epsilon));

            // RAVE
            double beta = Math.sqrt(R / (R + 3.0 * child.numberOfVisits));
            double uctValue = (1.0 - beta) * childValue
                    + beta * (raveWins[child.childIndex] / raveVisits[child.childIndex])
                    + params.K * Math.sqrt(Math.log(this.numberOfVisits + 1) / (child.numberOfVisits + params.epsilon));

            // Break ties randomly
            uctValue = Utils.noise(uctValue, params.epsilon, random.nextDouble());

            // small sampleRandom numbers: break ties in unexpanded nodes
            if (uctValue > bestValue) {
                selected = child;
                bestValue = uctValue;
            }
        }

        if (selected == null)
        {
            throw new RuntimeException("Warning! returning null: " + bestValue + " : " + this.children.length + " " +
                    + bounds[0] + " " + bounds[1]);
        }

        //Roll the state:
        rollState(state, actions[selected.childIndex]);

        return selected;
    }

    private double rollOut(GameState state)
    {
        int thisDepth = this.currentDepth;

        while (thisDepth <= params.maxRolloutDepth && !state.isTerminal()) {
            int action = safeRandomAction(state);
            rollState(state, actions[action]);
            thisDepth++;
        }

        return stateHeuristic.evaluateState(state);
    }

    // TODO huge possibility to improve performance here
    // E.g. by not making next action for simulation random
    // But basing it on the opponents somehow
    // For now, this method is basically copied from the SingleTreeNode
    private int safeRandomAction(GameState state)
    {
        Types.TILETYPE[][] board = state.getBoard();
        ArrayList<Types.ACTIONS> actionsToTry = Types.ACTIONS.all();
        int width = board.length;
        int height = board[0].length;

        while(actionsToTry.size() > 0) {
            int nAction = random.nextInt(actionsToTry.size());
            Types.ACTIONS act = actionsToTry.get(nAction);
            Vector2d dir = act.getDirection().toVec();

            Vector2d pos = state.getPosition();
            int x = pos.x + dir.x;
            int y = pos.y + dir.y;

            if (x >= 0 && x < width && y >= 0 && y < height)
                if(board[y][x] != Types.TILETYPE.FLAMES)
                    return nAction;

            actionsToTry.remove(nAction);
        }

        //Uh oh...
        return random.nextInt(numActions);
    }

    private void backpropagate(MyTreeNode selected, double result){
        MyTreeNode node = selected;
        while(node != null)
        {
            // Increase number of visits
            node.numberOfVisits++;
            // Update node value
            node.totalValue += result;

            if(node.childIndex != -1){
                node.raveVisits[node.childIndex]++;
                node.raveWins[node.childIndex] += result;
            }

            // Update bounds
            if (result < node.bounds[0]) {
                node.bounds[0] = result;
            }
            if (result > node.bounds[1]) {
                node.bounds[1] = result;
            }

            node = node.parent;
        }
    }

    /**
     * Whether the current search tree is fully expanded
     * @return
     */
    private boolean notFullyExpanded() {
        for (MyTreeNode node : children) {
            if (node == null) {
                return true;
            }
        }

        return false;
    }
}
