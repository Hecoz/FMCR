package controller.scheduling.strategy;

import controller.FMCRProperties;
import controller.Instrumentor.RVGlobalStateForInstrumentation;
import controller.Instrumentor.RVRunTime;
import controller.scheduling.ChoiceType;
import controller.scheduling.ThreadInfo;
import controller.scheduling.events.EventType;
import engine.config.Configuration;
import engine.trace.Trace;
import engine.trace.TraceInfo;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FMCRStrategy extends SchedulingStrategy {

    protected Queue<List<String>> toExplore;

    public static List<Integer> choicesMade;

    public static List<String> schedulePrefix = new ArrayList<String>();

    public static Trace currentTrace;

    private boolean notYetExecutedFirstSchedule;

    private final static int NUM_THREADS = 10;

    public volatile static ExecutorService executor;

    protected ThreadInfo previousThreadInfo;

    public final static boolean fullTrace;

    static {
        fullTrace = Boolean.parseBoolean(FMCRProperties.getFmcrProperties()
                .getProperty(FMCRProperties.RV_CAUSAL_FULL_TRACE, "false"));
    }


    public static Trace getTrace() {
        return currentTrace;
    }

    /**
     * Called before a new exploration starts
     *  do some initial work for exploring
     */
    @Override
    public void startingExploration() {

        this.toExplore = new ConcurrentLinkedQueue<List<String>>();

        FMCRStrategy.choicesMade = new ArrayList<Integer>();
        FMCRStrategy.schedulePrefix = new ArrayList<String>();

        this.notYetExecutedFirstSchedule = true;
        RVRunTime.currentIndex = 0;
        executor = Executors.newFixedThreadPool(NUM_THREADS);
    }

    /**
     * called before a new schedule starts
     */
    @Override
    public void startingScheduleExecution() {

        List<String> prefix = this.toExplore.poll();
        System.out.println("current schedule:" + prefix);

        if (!FMCRStrategy.choicesMade.isEmpty()) {   // when not empty
            FMCRStrategy.choicesMade.clear();
            FMCRStrategy.schedulePrefix = new ArrayList<String>();
            for (String choice : prefix) {
                FMCRStrategy.schedulePrefix.add(choice);
            }
        }

        RVRunTime.currentIndex = 0;
        RVRunTime.failure_trace.clear();
        initTrace();

        previousThreadInfo = null;
    }


    int i  = 0 ;
    @Override
    public void completedScheduleExecution() {

        this.notYetExecutedFirstSchedule = false;

        Vector<String> prefix = new Vector<String>();
        for (String choice : FMCRStrategy.schedulePrefix) {
            prefix.add(choice);
        }

        System.out.print("<< Exploring trace executed along causal schedule  " + i + ": ");
        i++;
        System.err.println(choicesMade);
        System.out.print("\n");

        //executeMultiThread(trace, prefix);
        /*
         * after executing the program along the given prefix
         * then the model checker will analyze the trace generated
         * to computer more possible interleavings
         */
        //executeSingleThread(prefix);
    }

    @Override
    public boolean canExecuteMoreSchedules() {

        boolean result = (!this.toExplore.isEmpty())
                || this.notYetExecutedFirstSchedule;
        if (result) {
            return true;
        }
        return false;
//        while (StartExploring.executorsCount.getValue() > 0) {
//            try {
//                Thread.sleep(10);
//                // if (!this.toExplore.isEmpty()) {
//                // return true;
//                // }
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//        }
//        result = (!this.toExplore.isEmpty())
//                || this.notYetExecutedFirstSchedule;
//        return result;
    }

    /**
     * choose the next statement to execute
     */
    @Override
    public Object choose(SortedSet<?> objectChoices, ChoiceType choiceType) {
        /*
         * Initialize choice
         */
        int chosenIndex = 0;
        Object chosenObject = null;

        //System.out.println("👎👎👎："+MCRStrategy.schedulePrefix + ",RVRunTime.currentIndex:" + RVRunTime.currentIndex);
        //for the rest events, executed in random schedule
        if (FMCRStrategy.schedulePrefix.size() > RVRunTime.currentIndex) {
            //System.out.println("😅😅😅："+MCRStrategy.schedulePrefix + ",RVRunTime.currentIndex:" + RVRunTime.currentIndex);
            /*
             * Make the choice to be made according to schedule prefix
             */
            // chosenIndex = MCRStrategy.schedulePrefix
            // .get(this.currentIndex);
            //System.out.println("👎👎👎："+objectChoices + ",RVRunTime.currentIndex:" + RVRunTime.currentIndex);
            chosenIndex = getChosenThread(objectChoices, RVRunTime.currentIndex);
            //System.out.println("chosenIndex:" + chosenIndex);
            chosenObject = getChosenObject(chosenIndex, objectChoices);

            if (Configuration.DEBUG) {
                if (chosenObject != null)
                    System.out.println(RVRunTime.currentIndex + ":" + chosenObject.toString());
            }

            if (chosenObject == null) {

                //one case that can cause this is due to the wait event
                //wait has no corresponding schedule index, it has to be announced
                //chose the wait to execute, the wait is trying to acquire the semaphore
                for (Iterator<? extends Object> iterator = objectChoices.iterator(); iterator.hasNext();) {
                    ThreadInfo threadInfo = (ThreadInfo) iterator.next();
                    if(threadInfo.getEventDesc().getEventType() == EventType.WAIT){
                        return threadInfo;
                    }
                }

                //what if the chosenObject is still null??
                //it might not correct
                if (chosenObject == null) {
                    chosenIndex = 0;
                    while (true) {
                        chosenObject = getChosenObject(chosenIndex, objectChoices);

                        if(choiceType.equals(ChoiceType.THREAD_TO_FAIR)
                                && chosenObject.equals(previousThreadInfo))
                        {
                            //change to a different thread
                        }
                        else
                            break;
                        chosenIndex++;

                    }

                }
                FMCRStrategy.choicesMade.add(chosenIndex);

                this.previousThreadInfo = (ThreadInfo) chosenObject;
                return chosenObject;
            }

        }

        //it might be that the wanted thread is blocked, waiting to be added to the paused threads
        if (chosenObject == null) {
            chosenIndex = 0;
            while (true) {
                chosenObject = getChosenObject(chosenIndex, objectChoices);

                if(choiceType.equals(ChoiceType.THREAD_TO_FAIR)
                        && chosenObject.equals(previousThreadInfo))
                {
                    //change to a different thread
                }
                else
                    break;
                chosenIndex++;

            }

        }

        FMCRStrategy.choicesMade.add(chosenIndex);

        this.previousThreadInfo = (ThreadInfo) chosenObject;

        return chosenObject;
    }

    @Override
    public List<Integer> getChoicesMadeDuringThisSchedule() {
        return FMCRStrategy.choicesMade;
    }

    //problem here
    //in the first execution, the initialized trace will be used by the aser-engine project
    //however, in the first initialization, the trace hasn't been complete yet.
    private void initTrace() {

        RVRunTime.init();
        TraceInfo traceInfo = new TraceInfo(
                RVGlobalStateForInstrumentation.variableIdSigMap,
                new HashMap<Integer, String>(),
                RVGlobalStateForInstrumentation.stmtIdSigMap,
                RVRunTime.threadTidNameMap);
        traceInfo.setVolatileAddresses(RVGlobalStateForInstrumentation.instance.volatilevariables);
        currentTrace = new Trace(traceInfo);
    }

    /**
     * chose a thread object based on the index
     * return -1 if not found
     * @param objectChoices
     * @param index
     * @return
     */
    private int getChosenThread(SortedSet<? extends Object> objectChoices,int index) {

        // String name = this.schedulePreifixName.get(index);
        //String name = MCRStrategy.schedulePrefix.get(index);
        String name = FMCRStrategy.schedulePrefix.get(index).split("_")[0];
        long tid = -1;
        for (Map.Entry<Long, String> entry : RVRunTime.threadTidNameMap.entrySet()) {

            if (name.equals(entry.getValue())) {
                tid = entry.getKey();
                break;
            }
        }

        Iterator<? extends Object> iter = objectChoices.iterator();
        int currentIndex = -1;
        while (iter.hasNext()) {
            ++currentIndex;
            ThreadInfo ti = (ThreadInfo) iter.next();
            //System.out.println("😢😢😢ThreadInfo:"+ti + " ti.ID:" + ti.getThread().getId());
            if (ti.getThread().getId() == tid) {
                return currentIndex;
            }
        }
        return -1;
    }
}
