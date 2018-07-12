package controller.Instrumentor;

import controller.exploration.Scheduler;

public class RVRunTime {


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
