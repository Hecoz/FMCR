package pattern;

import engine.trace.AbstractNode;
import engine.trace.IMemNode;
import engine.trace.ReadNode;
import engine.trace.WriteNode;

import javax.swing.*;
import java.util.*;

public class Pattern {
    private List<IMemNode> nodes;
    private PatternType patternType;


    public Pattern(List<IMemNode> nodes) {
        this.nodes = nodes;
        this.patternType = parsePatternType(nodes);
    }

    public PatternType getPatternType() {
        return patternType;
    }

    //两个pattern类型相同
    //各个node除了tid和gid不同剩下都相同

    /**
     * 严格的判断是否两个pattern相同
     * @param pattern1
     * @param pattern2
     * @return
     */
    public static boolean isTheSamePatternStrict(Pattern pattern1, Pattern pattern2){

        if (pattern1.getNodes().size() != pattern2.getNodes().size()) {
            return false;
        }

        if(pattern1.getPatternType() != pattern2.getPatternType()) {
            return false;
        }

        List<IMemNode> nodes1 = pattern1.getNodes();
        List<IMemNode> nodes2 = pattern2.getNodes();

        for(int i = 0; i < nodes1.size(); i++) {
            if(nodes1.get(i).getType() != nodes2.get(i).getType()) {
                return  false;
            } else {
                if(nodes1.get(i).getType() == AbstractNode.TYPE.READ) {
                    if(!((ReadNode)nodes1.get(i)).getLabel().equals(((ReadNode)nodes2.get(i)).getLabel())) {
                        return false;
                    }
                } else {
                    if(!((WriteNode)nodes1.get(i)).getLabel().equals(((WriteNode)nodes2.get(i)).getLabel())) {
                        return false;
                    }
                }
            }

            if(!nodes1.get(i).getAddr().equals(nodes2.get(i).getAddr())) {
                return false;
            }
        }
        return true;
    }

    /**
     * 宽松的判断是否是同一pattern
     * @param pattern1
     * @param pattern2
     * @return
     */
    public static boolean isTheSamePatternLoose(Pattern pattern1, Pattern pattern2) {
        //TODO  complete the loose judge logic
        return isTheSamePatternStrict(pattern1, pattern2);
    }
    /**
     * factory method get all patterns appear in trace rw nodes
     * @param nodes
     * @param window
     * @return
     */
    public static List<Pattern> getPatternsFromNodes(List<IMemNode> nodes, int window ) {
        List<Pattern> patterns = new ArrayList<>();

        for(int i = 0; i < nodes.size(); i++) {
            IMemNode node = nodes.get(i);

            if (window == 0) {
                patterns.addAll(getPatterns(nodes, node, i + 1, nodes.size()));
            } else {
                int end = nodes.size() > i + window ? i + window : nodes.size();
                patterns.addAll(getPatterns(nodes, node, i + 1, end));
            }
        }

        return patterns;
    }

    public static List<Pattern> getPatternsFromLengthTwoPattern(List<Pattern> patterns) {
        Pattern currentPattern, nextPattern, generatedPattern;
        List<Pattern> result = new ArrayList<>();
        for(int i = 0; i < patterns.size(); i++) {
            currentPattern = patterns.get(i);
            if(currentPattern.getNodes().size() != 2) continue;
            for(int j = i + 1; j < patterns.size(); j++) {
                nextPattern = patterns.get(j);
                if(nextPattern.getNodes().size() != 2) continue;
                generatedPattern = tryConstructFalconPattern(currentPattern, nextPattern);
                if (generatedPattern != null) {
                    result.add(generatedPattern);
                }
            }
        }

        return  result;
    }


    //TODO
    public String generateStopPattern() {
        if(this.getNodes().size() == 2) {
            return "(assert ( > x" + this.getNodes().get(0).getGID() + " x" + this.getNodes().get(1).getGID() + " ))\n";
        } else if(this.getNodes().size() == 3) {
            return "(assert ( or ( > x" + this.getNodes().get(0).getGID() + " x" + this.getNodes().get(1).getGID() + ") " +
                    "( > x" + this.getNodes().get(1).getGID() + " x" +  this.getNodes().get(2).getGID() + " )))\n";
        }
        return "";
    }

    /**
     * construct one length-3 pattern from two length-2 pattern
     * @param pattern1
     * @param pattern2
     * @return
     */
    public static Pattern tryConstructFalconPattern(Pattern pattern1, Pattern pattern2) {

        List<IMemNode> nodes1 = pattern1.getNodes();
        List<IMemNode> nodes2 = pattern2.getNodes();
        Pattern result = null;

        if (nodes1.size() != 2 && nodes2.size() != 2) {
            return null;
        } else {
            IMemNode node1 = nodes1.get(0);
            IMemNode node2 = nodes1.get(1);
            IMemNode node3 = nodes2.get(0);
            IMemNode node4 = nodes2.get(1);

            //同一个线程的
            //rwr or www
            if(node2.getGID() == node3.getGID() && node1.getTid() == node4.getTid()) {
                result = new Pattern(Arrays.asList(node1, node2, node4));
            }
            return result;
        }

    }

    private static List<Pattern> getPatterns(List<IMemNode> nodes, IMemNode currentNode, int start, int end) {
        List<Pattern> tempPattern = new ArrayList<>();

        if (currentNode.getType() == AbstractNode.TYPE.READ) {
            for (int i = start; i < end; i++) {
                IMemNode node = nodes.get(i);

                if (node.getType() == AbstractNode.TYPE.WRITE &&
                        node.getAddr().equals(currentNode.getAddr()) &&
                        node.getTid() != currentNode.getTid()){
                    tempPattern.add(new Pattern(Arrays.asList(currentNode, node)));
                    break;
                }
            }
        } else {
            for (int i = start; i < end; i++) {
                IMemNode node = nodes.get(i);

                // wr node
                // access the same location
                // in different thread
                if (node.getType() == AbstractNode.TYPE.READ &&
                        node.getAddr().equals(currentNode.getAddr()) &&
                        node.getTid() != currentNode.getTid()) {
                    tempPattern.add(new Pattern(Arrays.asList(currentNode, node)));
                    continue;
                }

                // ww node
                if (node.getType() == AbstractNode.TYPE.WRITE &&
                        node.getAddr().equals(currentNode.getAddr()) &&
                        node.getTid() != currentNode.getTid()){
                    tempPattern.add(new Pattern(Arrays.asList(currentNode, node)));
                    break;
                }
            }
        }

        return tempPattern;
    }


    public List<IMemNode> getNodes() {
        return nodes;
    }



    private PatternType parsePatternType(List<IMemNode> nodes) {
        if (nodes.size() < 2) {
            throw new IllegalStateException("the number of nodes less than 2");
        }

        if (nodes.size() <= 3) {
            StringBuilder type = new StringBuilder();
            for (IMemNode node : nodes) {
                if (node.getType() == AbstractNode.TYPE.READ) {
                    type.append("R");
                } else if (node.getType() ==  AbstractNode.TYPE.WRITE) {
                    type.append("W");
                } else {
                    throw new IllegalArgumentException();
                }
            }
            return PatternType.valueOf(type.toString());
        }

        return null;
    }

    @Override
    public String toString() {
        return "Pattern{" +
                "nodes=" + nodes +
                ", patternType=" + patternType +
                '}';
    }
}

class IllegalPatternNodes extends Exception {
    public IllegalPatternNodes() {
    }

    public IllegalPatternNodes(String message) {
        super(message);
    }

    public IllegalPatternNodes(String message, Throwable cause) {
        super(message, cause);
    }

    public IllegalPatternNodes(Throwable cause) {
        super(cause);
    }

    public IllegalPatternNodes(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}

class IllegalNodeType extends Exception{

}