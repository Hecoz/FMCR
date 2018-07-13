package controller.Instrumentor;

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


    /**
     * In the class:RVSharedAccessEventsMethodTransformer   function:visitFieldInsn
     * add this function names and descriptions in the RVRuntime to instrument class
     */
    public final String BEFORE_FIELD_ACCESS = "beforeFieldAccess";
    public final String BOOL_3STRINGS_VOID = "(ZLjava/lang/String;Ljava/lang/String;Ljava/lang/String;)V";

    public final String AFTER_FIELD_ACCESS = "afterFieldAccess";

    public final String LOG_FIELD_ACCESS = "logFieldAcc";
    public final String DESC_LOG_FIELD_ACCESS = "(ILjava/lang/Object;ILjava/lang/Object;Z)V";

    public final String LOG_INIT_WRITE_ACCESS = "logInitialWrite";
    public final String DESC_LOG_INIT_WRITE_ACCESS = "(ILjava/lang/Object;ILjava/lang/Object;)V";



    public final String BEFORE_ARRAY_ACCESS = "beforeArrayAccess";
    public final String AFTER_ARRAY_ACCESS = "afterArrayAccess";
    public final String BOOL_VOID = "(Z)V";

    public final String LOG_ARRAY_ACCESS = "logArrayAcc";
    public final String DESC_LOG_ARRAY_ACCESS = "(ILjava/lang/Object;ILjava/lang/Object;Z)V";

    public final String LOG_UNLOCK_STATIC = "logStaticSyncUnlock";
    public final String DESC_LOG_UNLOCK_STATIC = "(II)V";

    public final String LOG_THREAD_BEGIN = "logThreadBegin";
    public final String DESC_LOG_THREAD_BEGIN = "()V";

    public final String LOG_THREAD_END = "logThreadEnd";
    public final String DESC_LOG_THREAD_END = "()V";

    public final String LOG_LOCK_STATIC = "logStaticSyncLock";
    public final String DESC_LOG_LOCK_STATIC = "(II)V";


}
