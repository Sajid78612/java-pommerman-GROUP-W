package players.groupW;

import players.optimisers.ParameterSet;
import utils.Pair;

import java.util.ArrayList;
import java.util.Map;

public class MyMCTSParams implements ParameterSet {

    public int maxRolloutDepth = 10;
    public double epsilon = 1e-6;

    // Change this for testing
    public int maxNumIterations = 100;

    @Override
    public void setParameterValue(String s, Object o) {

    }

    @Override
    public Object getParameterValue(String s) {
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
    public Pair<String, ArrayList<Object>> getParameterParent(String s) {
        return null;
    }

    @Override
    public Map<Object, ArrayList<String>> getParameterChildren(String s) {
        return null;
    }

    @Override
    public Map<String, String[]> constantNames() {
        return null;
    }
}
