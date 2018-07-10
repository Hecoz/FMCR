public class RVConfig {

    public static final RVConfig instance = new RVConfig();

    public String mode = "SC";   //default: sequential consistency memory model


    /**
     * In the class:RVSharedAccessEventsMethodTransformer   function:visitMethodInsn
     * add this function names and descriptions in the RVRuntime to instrument class
     */
    public final String LOG_THREAD_BEFORE_START = "logBeforeStart";
    public final String DESC_LOG_THREAD_START = "(ILjava/lang/Object;)V";

    public final String LOG_THREAD_JOIN = "logJoin";
    public final String DESC_LOG_THREAD_JOIN = "(ILjava/lang/Object;)V";

    public final String LOG_WAIT = "logWait";
    public final String DESC_LOG_WAIT = "(ILjava/lang/Object;)V";

    public final String LOG_NOTIFY = "logNotify";
    public final String LOG_NOTIFY_ALL = "logNotifyAll";
    public final String DESC_LOG_NOTIFY = "(ILjava/lang/Object;)V";

    public final String LOG_LOCK_INSTANCE = "logLock";
    public final String DESC_LOG_LOCK_INSTANCE = "(ILjava/lang/Object;)V";

    public final String LOG_UNLOCK_INSTANCE = "logUnlock";
    public final String DESC_LOG_UNLOCK_INSTANCE = "(ILjava/lang/Object;)V";

    public final String LOG_THREAD_SLEEP = "logSleep";
    public final String DESC_LOG_THREAD_SLEEP = "()V";




}
