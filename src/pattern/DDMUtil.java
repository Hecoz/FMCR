package pattern;

import engine.trace.AbstractNode;
import engine.trace.IMemNode;
import engine.trace.Trace;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Vector;
import java.util.stream.Collectors;




public class DDMUtil {
    /**
     * extrace all patterns from trace
     * @param trace
     * @return
     */
    public static List<Pattern> getAllPatterns(Trace trace) {
        Vector<AbstractNode> nodes = trace.getFullTrace();

        HashSet<String> variables = trace.getSharedVariables();

        List<IMemNode> RWNodes = nodes.stream().filter(node -> (node.getType() == AbstractNode.TYPE.READ || node.getType() == AbstractNode.TYPE.WRITE))
                .filter(node -> variables.contains(((IMemNode)node).getAddr())).map(node -> (IMemNode)node).collect(Collectors.toList());


        List<pattern.Pattern> patterns = pattern.Pattern.getPatternsFromNodes(RWNodes, 0);
        List<pattern.Pattern> falconPatterns = pattern.Pattern.getPatternsFromLengthTwoPattern(patterns);

        patterns.addAll(falconPatterns);

        return patterns;

    }

    /**
     * get patterns appear in error patterns but not success patterns
     * @param errorPatterns
     * @param successPatterns
     * @return
     */
    public static List<pattern.Pattern> getDifferentPatterns(List<Pattern> errorPatterns, List<Pattern> successPatterns) {
        List<Pattern> differnPattern = new ArrayList<>();
        boolean hasSame = false;
        for(Pattern errorPattern: errorPatterns) {
            for(Pattern successPattern : successPatterns) {
                if(Pattern.isTheSamePatternStrict(errorPattern, successPattern)) {
                    hasSame = true;
                    break;
                }
            }

            if(!hasSame) {
                differnPattern.add(errorPattern);
            } else {
                hasSame = false;
            }
        }

        return differnPattern;
    }
}
