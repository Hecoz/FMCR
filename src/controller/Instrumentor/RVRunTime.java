package controller.Instrumentor;

import java.util.HashMap;
import java.util.Vector;

public class RVRunTime {

    public static HashMap<Long, String> threadTidNameMap;   //线程 ID 与对应名字的 map
    public static HashMap<Long, Integer> threadTidIndexMap;  //线程 ID 与对应的索引的 map
    final static String MAIN_NAME = "0";
    public static long globalEventID;
    public static int currentIndex = 0;

    public static Vector<String> failure_trace = new Vector<String>();



    public static void init() {
        long tid = Thread.currentThread().getId();
        threadTidNameMap = new HashMap<Long, String>();
        threadTidNameMap.put(tid, MAIN_NAME);
        threadTidIndexMap = new HashMap<Long, Integer>();
        threadTidIndexMap.put(tid, 1);
        globalEventID = 0;
    }



    public static void logInitialWrite(int ID, final Object o, int SID, final Object v) {

    }


    /**
     * When starting a new thread, a consistent unique identifier of the thread
     * is created, and stored into a map with the thread id as the key. The
     * unique identifier, i.e, name, is a concatenation of the name of the
     * parent thread with the order of children threads forked by the parent
     * thread.
     * 当创建一个新线程的时候调用，并保存线程相关信息
     * @param ID
     * @param o
     */
    public static void logBeforeStart(int ID, final Object o) {

    }

    public static void logThreadBegin()
    {
        //Scheduler.beginThread();
    }

    /**
     *
     * @param ID
     * @param o
     * @param SID
     * @param v
     * @param write
     */
    public static void logFieldAcc(int ID, final Object o, int SID, final Object v, final boolean write) {

    }

    public static void logJoin(int ID, final Object o) {

    }

    /**
     * log the lock events
     * @param ID
     * @param lock
     */

    public static void logLock(int ID, final Object lock)
    {
    }
}
