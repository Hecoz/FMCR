package engine.sdg;

import engine.graph.ReachabilityEngine;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;


public class ConstructReachabilitySDG {
	public static ReachabilityEngine ConstructReachability(Map<Integer, Set<Integer>> sdg){
		ReachabilityEngine reachSDG = new ReachabilityEngine();
		
		for (Map.Entry<Integer, Set<Integer>> entry:sdg.entrySet()){
			Integer key = entry.getKey();
			Set<Integer> valueSet = entry.getValue();
			
			Iterator<Integer> itr = valueSet.iterator();
			while(itr.hasNext()){
				Integer value = itr.next();
				reachSDG.addEdge(key, value);
			}
		}
		
		return reachSDG;
	}
}
