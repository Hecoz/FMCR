package exploration;

import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.management.ThreadInfo;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import edu.illinois.imunit.internal.parsing.*;
import sun.misc.Unsafe;


public class Scheduler {

    private static final String BANG = "!";
    private static final String UNABLE_TO_OBTAIN_INSTANCE_OF = "Unable to obtain instance of: ";

    private static Map<Thread, ThreadInfo> liveThreadInfos;
    private static SortedSet<ThreadInfo> pausedThreadInfos;
    private static Set<ThreadInfo> blockedThreadInfos;

}
