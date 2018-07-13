package controller.exploration;

import java.io.StringReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;

import controller.MCRProperties;
import edu.illinois.imunit.ExpectedDeadlock;
import engine.ExploreSeedInterleavings;
import engine.trace.Trace;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;

import edu.illinois.imunit.Schedule;
import edu.illinois.imunit.ScheduleError;
import edu.illinois.imunit.Schedules;
import edu.illinois.imunit.internal.parsing.Orderings;
import edu.illinois.imunit.internal.parsing.ParseException;
import edu.illinois.imunit.internal.parsing.ScheduleParser;
import edu.illinois.imunit.internal.parsing.TokenMgrError;

/**
 * MCR runner for JUnit4 tests.
 * 
 */
public class JUnit4MCRRunner extends BlockJUnit4ClassRunner {

    private static final String DOT = ".";

    private static final String INVALID_SYNTAX_MESSAGE = "Ignoring schedule because of invalid syntax: name = %s value = %s .\nCaused by: %s";
    private static final String EXPECT_DEADLOCK_MSG = "Expecting deadlock!";
    
    private static int used;

    /**
     * Currently executing test method and notifier and schedule.
     */
    private FrameworkMethod currentTestMethod;
    private RunNotifier currentTestNotifier;
    
    private boolean isDeadlockExpected = false;
    public static HashSet<String> npes = new HashSet<String>();
    
    public JUnit4MCRRunner(Class<?> klass) throws InitializationError {
        super(klass);
    }

    private static JUnit4WrappedRunNotifier wrappedNotifier;
    private static FrameworkMethod method;
    private static boolean stopOnFirstError;
    
    //start from here
    //the first to be executed in this class
    //and after instrumentation
    @Override
    protected void runChild(final FrameworkMethod method, RunNotifier notifier) {

        this.used = 0;
        this.currentTestMethod = method;        //当前测试的方法
        this.currentTestNotifier = notifier;    //当前使用的Notifier,当前使用的 notifier还是默认 junit 的 notifier
        
        Trace.appname = method.getMethod().getDeclaringClass().getName(); //当前类的绝对路径

        Map<String, Orderings> schedules = collectSchedules();
        //System.out.println("schedules🙂️🙂🙂:" + schedules);
        if (!schedules.isEmpty()) {

            for (Entry<String, Orderings> schedule : schedules.entrySet()) {
                Scheduler.setIMUnitSchedule(schedule.getKey(), schedule.getValue());
                exploreTest(method, notifier);                
            }
            Scheduler.clearIMUnitSchedule();
        }
        else
        {
            exploreTest(method, notifier);
        }
    }
    
    /**
     * called by runChild
     * @param method
     * @param notifier
     */
    private void exploreTest(FrameworkMethod method, RunNotifier notifier) {

        stopOnFirstError = true;
        String stopOnFirstErrorString = MCRProperties.getInstance().getProperty(MCRProperties.STOP_ON_FIRST_ERROR_KEY); //true

        if (stopOnFirstErrorString.equalsIgnoreCase("false")) {
            stopOnFirstError = false;
        }
        
        JUnit4MCRRunner.method = method;    //当前测试方法
        String name = getTestClass().getName() + DOT + method.getName();  //当前测试类下面的测试方法

        //这里调用 startingExploration 初始化方法
        Scheduler.startingExploration(name);

        wrappedNotifier = new JUnit4WrappedRunNotifier(notifier);
        wrappedNotifier.testExplorationStarted();
        
        Thread explorationThread = getNewExplorationThread();
        explorationThread.start();              //start the exploration

        //after the state space exploration finishes
        while (true) {
            try {
                // wait for either a normal finish or a deadlock to occur
                Scheduler.getTerminationNotifer().acquire();
                while (explorationThread.getState().equals(Thread.State.RUNNABLE)) {
                    Thread.yield();
                }
                // check for deadlock
                if (!isDeadlockExpected && (explorationThread.getState().equals(Thread.State.WAITING) || 
                        explorationThread.getState().equals(Thread.State.BLOCKED))) {

                    Scheduler.failureDetected("Deadlock detected in schedule");
                    Scheduler.completedScheduleExecution(); //call  the mcr method
                    wrappedNotifier.fireTestFailure(new Failure(describeChild(method), new RuntimeException("Deadlock detected in schedule")));
                    wrappedNotifier.setFailure(null); // workaround to prevent
                                                      // exploration thread from
                                                      // thinking that a
                                                      // previous failure means
                                                      // a failure in current
                                                      // schedule
                    // if we should continue exploring from deadlock
                    if (!stopOnFirstError) {
                        // leave currently deadlocked threads in place
                        explorationThread = getNewExplorationThread();
                        explorationThread.start();
                        continue;
                    }
                } else if (isDeadlockExpected && (explorationThread.getState().equals(Thread.State.WAITING) || 
                        explorationThread.getState().equals(Thread.State.BLOCKED))) {

                    Scheduler.completedScheduleExecution();
                    wrappedNotifier.setFailure(null);
                    explorationThread = getNewExplorationThread();
                    explorationThread.start();
                    continue;
                }
                break;
            } catch (InterruptedException e) {
                e.printStackTrace();
                System.exit(2);
            }
        }   //end while
        wrappedNotifier.testExplorationFinished();
        Scheduler.completedExploration();
        
        System.err.println("memory used: " + ExploreSeedInterleavings.memUsed + "bytes.");
    }
    
    /**
     * called by exploreTest in this class
     * @return a thread
     */
    private Thread getNewExplorationThread() {
        
        return new Thread() {
            public void run() {               
                while (Scheduler.canExecuteMoreSchedules()) {
                    
                    Scheduler.startingScheduleExecution();

                    JUnit4MCRRunner.super.runChild(method, wrappedNotifier);  //after choosen all the objects
                    //判断是否有错
                    if (wrappedNotifier.isTestFailed()) {
                        wrappedNotifier.getFailure().getException().printStackTrace();
                        Scheduler.failureDetected(wrappedNotifier.getFailure().getMessage());
                        if (stopOnFirstError) {
                            break;
                        }
                    }
                    // If expected deadlock but it isn't deadlocking, fail the test
                    //判断是否有死锁
                    if (isDeadlockExpected) {
                        Scheduler.failureDetected(EXPECT_DEADLOCK_MSG);
                        Scheduler.completedScheduleExecution();
                        wrappedNotifier.fireTestFailure(new Failure(describeChild(method), new RuntimeException(EXPECT_DEADLOCK_MSG)));
                        if (stopOnFirstError) {
                            break;
                        }
                    }
                    //计算下一次调度
                    Scheduler.completedScheduleExecution();    //one schedule completed
                }
                //all schedules have been finished
                // notify runner that exploration has completed
                Scheduler.getTerminationNotifer().release();
            }
        };
    }


    /**
     * Helper method for collecting all the names and partial orders for the given test method.
     */
    private Map<String, Orderings> collectSchedules() {
        
        Map<String, Orderings> schedules = new HashMap<String, Orderings>();
        Schedules schsAnno = this.currentTestMethod.getAnnotation(Schedules.class);
        isDeadlockExpected = (this.currentTestMethod.getAnnotation(ExpectedDeadlock.class) != null);
        
        if (schsAnno != null) {
            for (Schedule schAnno : schsAnno.value()) {
                collectSchedule(schAnno, schedules);
            }
        }
        //why write it twice here??
        //because there are Schedule and Schedules,it doesn't matter,this do not use the function
//        Schedule schAnno = currentTestMethod.getAnnotation(Schedule.class);
//        if (schAnno != null) {
//            collectSchedule(schAnno, schedules);
//        }
        return schedules;
    }

    /**
     * Helper method for collecting the name and partial orders from each {@link Schedule} annotation.
     *
     * @param schAnno
     * @param schedules
     *
     */
    private void collectSchedule(Schedule schAnno, Map<String, Orderings> schedules) {
        String schName = schAnno.name();
        schName = schName != null && schName.length() > 0 ? schName : schAnno.value();
        try {
            schedules.put(schName, new ScheduleParser(new StringReader(schAnno.value())).Orderings());
        } catch (ParseException e) {
            this.currentTestNotifier.fireTestFailure(new Failure(describeChild(this.currentTestMethod), new ScheduleError(schName, String.format(INVALID_SYNTAX_MESSAGE, schName, schAnno.value(), e))));
        } catch (TokenMgrError e) {
            this.currentTestNotifier.fireTestFailure(new Failure(describeChild(this.currentTestMethod), new ScheduleError(schName, String.format(INVALID_SYNTAX_MESSAGE, schName, schAnno.value(), e))));
        }
    }

}
