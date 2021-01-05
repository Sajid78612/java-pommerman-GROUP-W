package players.groupW.EMCTS;

import players.optimisers.ParameterSet;
import utils.Pair;
import utils.Types;

import java.util.ArrayList;
import java.util.Map;

public class EMCTSParams implements ParameterSet {

    public int maxRolloutDepth = 4;
    public double epsilon = 1e-6;

    // Change this for testing
    // Was 40 for tests
    public int maxNumIterations = 8;

    public int branchingFactor = 2;

    public double currentBest = -Double.MAX_VALUE;
    public Types.ACTIONS[] currentBestGenome;

    @Override
    public void setParameterValue(String param, Object value) {

    }

    @Override
    public Object getParameterValue(String root) {
        return null;
    }

    @Override
    public ArrayList<String> getParameters() {
        return null;
    }

    @Override
    public Map<String, Object[]> getParameterValues() {
        return null;
    }

    @Override
    public Pair<String, ArrayList<Object>> getParameterParent(String parameter) {
        return null;
    }

    @Override
    public Map<Object, ArrayList<String>> getParameterChildren(String root) {
        return null;
    }

    @Override
    public Map<String, String[]> constantNames() {
        return null;
    }
}
