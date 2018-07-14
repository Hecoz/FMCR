package engine;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.Vector;

import com.sun.xml.internal.bind.v2.runtime.output.SAXOutput;
import engine.config.Configuration;
import engine.trace.Trace;
import pattern.DDMUtil;
import pattern.Pattern;

public class StartExploring implements Runnable {

	private Trace traceObj;

	private Vector<String> schedule_prefix;

	private Queue<List<String>> exploreQueue;
	static List<Trace> traces = new ArrayList<>();

	public static class BoxInt {

		volatile int  value;

		public BoxInt(int initial) {
			this.value = initial;
		}

		public synchronized int getValue() {
			return this.value;
		}

		public synchronized void increase() {
			this.value++;
		}

		public synchronized void decrease() {
			this.value--;
		}
	}

	public final static BoxInt executorsCount = new BoxInt(0);

	public StartExploring(Trace trace, Vector<String> prefix,
			Queue<List<String>> queue) {

		this.traceObj = trace;
		this.schedule_prefix = prefix;
		this.exploreQueue = queue;
	}

	public Trace getTrace() {
		return this.traceObj;
	}

	public Vector<String> getCurrentSchedulePrefix() {
		return this.schedule_prefix;
	}

	public Queue<List<String>> exploreQueue() {
		return this.exploreQueue;
	}

	/**
	 * start exploring other interleavings
	 * 
	 */
	public void run() {
		try {
			ExploreSeedInterleavings.setQueue(exploreQueue);
			
			//System.out.println("StartExploring1:" + exploreQueue);
			traceObj.finishedLoading(true);

			if (traces.size() < 2) {
				traces.add(traceObj);
			} else {
				List<Pattern> pattern = DDMUtil.getAllPatterns(traces.get(0));
				List<Pattern> pattern1 = DDMUtil.getAllPatterns(traces.get(1));

				List<Pattern> differtPatterns = DDMUtil.getDifferentPatterns(pattern1, pattern);

				for (Pattern p: differtPatterns) {
					System.out.println(p.generateStopPattern());
				}
			}



			ExploreSeedInterleavings.execute(traceObj, schedule_prefix);
			//System.out.println("StartExploring2:" + exploreQueue);
			ExploreSeedInterleavings.memUsed += ExploreSeedInterleavings.memSize(ExploreSeedInterleavings.mapPrefixEquivalent);
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println(e.getMessage());
		}
		finally {
			if (Configuration.DEBUG) {
				System.out.println("  Exploration Done with this trace! >>\n\n");
			}
		}
	}
}
