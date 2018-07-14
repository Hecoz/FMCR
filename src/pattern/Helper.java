package pattern;

import engine.trace.AbstractNode;
import engine.trace.IMemNode;
import engine.trace.ReadNode;
import engine.trace.WriteNode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Helper {

    /**
     * get all similar nodes
     * @param node
     * @param nodes
     * @param withLabel whether cosidering line number info
     * @return
     */
    private static List<IMemNode> getAllSimilarNodes(IMemNode node, List<IMemNode> nodes, boolean withLabel) {
        List<IMemNode> result = new ArrayList<>();
        //the node itself is a similar node of itself
        result.add(node);
        if(withLabel) {
            for(IMemNode iMemNode : nodes) {
                if(isSimilarNode(iMemNode, node)) {
                    result.add(iMemNode);
                }
            }
        } else {
            for(IMemNode iMemNode : nodes) {
                if(isSimilarNodeWithoutLabel(iMemNode, node)) {
                    result.add(iMemNode);
                }
            }
        }

        return result;
    }


    private static boolean isSimilarNode(IMemNode node1, IMemNode node2) {
        if (!(node1.getType() == node2.getType() && node1.getAddr().equals(node2.getAddr()) && node1.getGID() != node2.getGID())) {
            return false;
        }

        if(node1.getType() == AbstractNode.TYPE.READ) {
            return ((ReadNode)node1).getLabel().equals(((ReadNode)node2).getLabel());
        } else {
            return ((WriteNode)node1).getLabel().equals(((WriteNode)node2).getLabel());
        }
    }

    private static  boolean isSimilarNodeWithoutLabel(IMemNode node1, IMemNode node2) {
        return node1.getType() == node2.getType() && node1.getAddr().equals(node2.getAddr()) && node1.getGID() != node2.getGID();
    }

    public static List<Pattern> getALLSimilarPatternFromNodes(Pattern pattern, List<IMemNode> nodes) {

        List<IMemNode> patternNodes = pattern.getNodes();
        List<Pattern> patterns = new ArrayList<>();

        if(patternNodes.size() == 2) {
            IMemNode node1 = patternNodes.get(0);
            IMemNode node2 = patternNodes.get(1);

            List<IMemNode> similar1 = getAllSimilarNodes(node1, nodes, true);
            List<IMemNode> similar2 = getAllSimilarNodes(node2, nodes, true);

            for (IMemNode similarnode1 : similar1) {
                for (IMemNode similarNode2 : similar2) {
                    if(similarNode2.getTid() != similarnode1.getTid()) {
                        patterns.add(new Pattern(Arrays.asList(similarnode1, similarNode2)));
                    }
                }
            }
        } else if(patternNodes.size() == 3) {
            IMemNode node1 = patternNodes.get(0);
            IMemNode node2 = patternNodes.get(1);
            IMemNode node3 = patternNodes.get(2);

            List<IMemNode> similar1 = getAllSimilarNodes(node1, nodes, true);
            List<IMemNode> similar3 = getAllSimilarNodes(node3, nodes, true);


            // get simialr nodes without considering label(line number info of the node)
            List<IMemNode> similar2 = getAllSimilarNodes(node2, nodes, false);

            for (IMemNode similarnode1 : similar1) {
                for (IMemNode similarNode3 : similar3) {
                    if(similarnode1.getTid() == similarNode3.getTid() && similarnode1.getGID() < similarNode3.getGID()) {
                        for(IMemNode similarNode2: similar2) {
                            if(similarNode2.getTid() != similarnode1.getTid()) {
                                patterns.add(new Pattern(Arrays.asList(similarnode1, similarNode2, similarNode3)));
                            }
                        }

                    }
                }
            }
        }

        return patterns;

    }
}
