package controller.exploration;

import java.util.*;

import controller.FMCRProperties;
import controller.internaljuc.locks.Reex_Condition;
import controller.internaljuc.locks.Reex_ReentrantLock;
import controller.scheduling.ThreadInfo;
import controller.scheduling.events.EventType;
import controller.scheduling.events.JoinEventDesc;
import controller.scheduling.events.ThreadLifeEventDesc;
import controller.scheduling.filtering.DefaultFilter;
import controller.scheduling.filtering.SchedulingFilter;
import controller.scheduling.strategy.SchedulingStrategy;


public class Scheduler {


    /**
     * Constants
     */
    private static final String BANG = "!";
    private static final String UNABLE_TO_OBTAIN_INSTANCE_OF = "Unable to obtain instance of: ";

    /******************************************************************
     ******************************************************************
     *********************** SCHEDULER STATE **************************
     ******************************************************************
     ******************************************************************/

    private static Map<Thread, ThreadInfo> liveThreadInfos;
    private static SortedSet<ThreadInfo> pausedThreadInfos;
    private static Set<ThreadInfo> blockedThreadInfos;

    private static final Map<String, Thread> currentHappenedEvents = new HashMap<String, Thread>();

    private static final Reex_ReentrantLock schedulerStateLock = new Reex_ReentrantLock();
    private static final Reex_Condition schedulerWakeupCondition = schedulerStateLock.newCondition();

    /**
     * {@link SchedulingStrategy} to be used for scheduling decisions.
     */
    private static SchedulingStrategy schedulingStrategy;
    /**
     * {@link SchedulingFilter} to be used for scheduling decisions.
     */
    private static SchedulingFilter schedulingFilter;

    private static boolean exploring = false;


    /**
     * Initialize state before everything.
     */
    static{

        /**
         * 01 init liveThreadInfos
         *         pausedThreadInfos
         *         blockedThreadInfos
         *         currentHappenedEvents
         */
        initState();


        /**
         * 02 Catch any uncaught exception thrown by any thread
         *    NOTE: this can be overridden by the code under test
         */
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {

            /*
             * This method will execute in the context of the thread that raised
             * the exception
             */
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                if (e != null) {
                    e.printStackTrace();
                }
                if(e instanceof NullPointerException){

                    String message = "";
                    for( StackTraceElement traceElement : e.getStackTrace())
                        message +=traceElement.toString()+"\n";

                    if(message.isEmpty()) {
                        System.out.println("DEBUG");
                    }

                    JUnit4MCRRunner.npes.add(message);
                    failureDetected(null);
                    //Listeners.fireCompletedExploration();
                    Scheduler.endThread();
                }
                else
                {
                    failureDetected(null);
                    //Listeners.fireCompletedExploration();
                    System.exit(2);
                }
            }
        });

        /**
         * 03 Set the scheduling strategy to be used
         */
        FMCRProperties mcrProps = FMCRProperties.getFmcrProperties();
        /** Set the scheduling strategy to be used */
        String schedulingStrategyClassName = "controller.scheduling.strategy.FMCRStrategy";
        if (schedulingStrategyClassName != null) {
            try {
                schedulingStrategy = (SchedulingStrategy) Class.forName(schedulingStrategyClassName).newInstance();
                //System.out.println("\nUsing the following scheduling strategy for exploration: " + schedulingStrategy.getClass().getName());
            } catch (Exception e) {
                System.err.println(UNABLE_TO_OBTAIN_INSTANCE_OF + schedulingStrategyClassName + BANG);
                e.printStackTrace();
                System.exit(2);
            }
        }
        /** Set the scheduling filter to be used */
        String schedulingFilterClassName = mcrProps.getProperty(FMCRProperties.SCHEDULING_FILTER_KEY);
        //这里为null,default.properties中并未设置
        if (schedulingFilterClassName != null) {
            try {
                schedulingFilter = (SchedulingFilter) Class.forName(schedulingFilterClassName).newInstance();
            } catch (Exception e) {
                System.err.println(UNABLE_TO_OBTAIN_INSTANCE_OF + schedulingFilterClassName + BANG);
                e.printStackTrace();
                System.exit(2);
            }
        } else {
            schedulingFilter = new DefaultFilter();
        }
    }

    /**
     * Helper method to reinitialize non-final members for each schedule.
     */
    private static void initState() {

//        if (Listeners.debugExploration) {
//            // Use ordered map when debugging to eliminate print output
//            // non-determinism
//            liveThreadInfos = new TreeMap<Thread, ThreadInfo>(new Comparator<Thread>() {
//                @Override
//                public int compare(Thread o1, Thread o2) {
//                    int idComparision = ((Long) o1.getId()).compareTo(o2.getId());
//                    return idComparision == 0 ? o1.getName().compareTo(o2.getName()) : idComparision;
//                }
//            });
//        } else {
//            // Use efficient map when not debugging
//            liveThreadInfos = new HashMap<Thread, ThreadInfo>();
//        }
        liveThreadInfos = new HashMap<Thread, ThreadInfo>();

        pausedThreadInfos = new TreeSet<ThreadInfo>();
        blockedThreadInfos = new HashSet<ThreadInfo>();
        informSchedulerOfCurrentThread();
        currentHappenedEvents.clear();
    }

    private static void informSchedulerOfCurrentThread() {
        Thread currentThread = Thread.currentThread();
        liveThreadInfos.put(currentThread, new ThreadInfo(currentThread));
    }


    /**
     * Should be called before a new exploration is going to be performed.
     * 在一个新的exploration 开始前调用
     *
     * 1) Informs the scheduling strategy that is being used that a new
     * exploration will be performed. <br/>
     * 通知 scheduling strategy 一个新的 exploration 开始了
     * 2) Prepares for a schedule execution.
     * 开始新的 执行
     */
    public static void startingExploration(String name) {

        //Listeners.fireStartingExploration(name);
        schedulingStrategy.startingExploration();
    }


    /**
     * Called before a field is accessed, it first needs to get the lock
     * it is instrumented to the class
     * @param isRead
     * @param owner
     * @param name
     * @param desc
     */
    public static void beforeFieldAccess(boolean isRead, String owner, String name, String desc) {
//        if (exploring) {
//            beforeEvent(new FieldAccessEventDesc(isRead ? EventType.READ : EventType.WRITE, owner, name, desc), true);
//        }
    }

    public static void failureDetected(String errorMsg) {
        //Listeners.fireFailureDetected(errorMsg, schedulingStrategy.getChoicesMadeDuringThisSchedule());
    }


    /**
     * Executed at the end of a {@link Runnable}'s run method.
     */
    public static void endThread() {
        if (exploring) {
            schedulerStateLock.lock();
            try {
                Thread currentThread = Thread.currentThread();
                ThreadInfo currentThreadInfo = liveThreadInfos.get(currentThread);
                if(currentThreadInfo!=null)
                {
                    currentThreadInfo.setEventDesc(new ThreadLifeEventDesc(EventType.END));
                    int newRunCount = currentThreadInfo.decrementRunCount();
                    if (newRunCount == 0) {
                        Set<ThreadInfo> joiningThisThreadInfos = new HashSet<ThreadInfo>();
                        for (ThreadInfo blockedThreadInfo : blockedThreadInfos) {
                            if (blockedThreadInfo.getEventDesc().getEventType().equals(EventType.JOIN)) {
                                JoinEventDesc joinEventDesc = (JoinEventDesc) blockedThreadInfo.getEventDesc();
                                if (joinEventDesc.getJoinThread().equals(currentThread)) {
                                    joiningThisThreadInfos.add(blockedThreadInfo);
                                    blockedThreadInfo.getPausingSemaphore().release();
                                }
                            }
                        }
                        blockedThreadInfos.removeAll(joiningThisThreadInfos);
                        liveThreadInfos.remove(currentThread);
                    }
                }
            } finally {
                schedulerWakeupCondition.signal();
                schedulerStateLock.unlock();
            }
        }
    }
}
