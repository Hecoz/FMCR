package controller.exploration;

import controller.FMCRProperties;
import engine.trace.Trace;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;

import java.util.HashSet;

/**
 * MCR runner for JUnit4 tests.
 *
 */
public class JUnit4MCRRunner extends BlockJUnit4ClassRunner {


    /**
     * Constants
     */
    private static final String DOT = ".";

    private static final String INVALID_SYNTAX_MESSAGE = "Ignoring schedule because of invalid syntax: name = %s value = %s .\nCaused by: %s";
    private static final String EXPECT_DEADLOCK_MSG = "Expecting deadlock!";

    /**
     * Currently executing test method and notifier and schedule.
     */
    private FrameworkMethod currentTestMethod;
    private RunNotifier currentTestNotifier;

    /**
     * programe terminated when the first error detected
     */
    private static boolean stopOnFirstError;
    public static HashSet<String> npes = new HashSet<String>();


    //private static JUnit4WrappedRunNotifier wrappedNotifier;
    private static FrameworkMethod method;


    /**
     * Creates a BlockJUnit4ClassRunner to run {@code klass}
     *
     * @param klass
     * @throws InitializationError if the test class is malformed.
     */
    public JUnit4MCRRunner(Class<?> klass) throws InitializationError {
        super(klass);
    }

    @Override
    protected void runChild(FrameworkMethod method, RunNotifier notifier) {

        this.currentTestMethod = method;
        this.currentTestNotifier = notifier;

        Trace.appname = method.getMethod().getDeclaringClass().getName();

        //源代码中使用了IMUnit，但并未使用，本项目中不涉及这部分
        exploreTest(method,notifier);
        //super.runChild(method, notifier);
    }

    /**
     * called by runChild
     * @param method
     * @param notifier
     */
    private void exploreTest(FrameworkMethod method, RunNotifier notifier) {

        stopOnFirstError = true;
        //check stopOnFirstError property
        String stopOnFirstErrorString = FMCRProperties.getFmcrProperties().getProperty(FMCRProperties.STOP_ON_FIRST_ERROR_KEY); //true

        if (stopOnFirstErrorString.equalsIgnoreCase("false")) {
            stopOnFirstError = false;
        }

        JUnit4MCRRunner.method = method;    //当前测试方法
        String name = getTestClass().getName() + DOT + method.getName();  //当前测试类下面的测试方法

        //这里调用 startingExploration 初始化方法
        Scheduler.startingExploration(name);

    }
}
