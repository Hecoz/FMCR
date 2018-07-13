package controller.Instrumentor;

import controller.exploration.Scheduler;
import controller.scheduling.strategy.MCRStrategy;
import engine.graph.Queue;
import engine.trace.*;

import java.util.HashMap;
import java.util.Vector;

public class RVRunTime {

    public static HashMap<Long, String> threadTidNameMap;   //线程 ID 与对应名字的 map
	public static HashMap<Long, Integer> threadTidIndexMap;  //线程 ID 与对应的索引的 map
	final static String MAIN_NAME = "0";
	public static long globalEventID;
    public static int currentIndex = 0;
    
    public static Vector<String> failure_trace = new Vector<String>();

    private static HashMap<Integer, Object> staticObjects= new HashMap<Integer,Object>();

    private static Object getObject(int SID)
    {
        Object o = staticObjects.get(SID);
        if(o==null)
        {
            o = new Object();
            staticObjects.put(SID,o);
        }
        return o;
    }

	public static void init() {
		long tid = Thread.currentThread().getId();
		threadTidNameMap = new HashMap<Long, String>();
		threadTidNameMap.put(tid, MAIN_NAME);
		threadTidIndexMap = new HashMap<Long, Integer>();
		threadTidIndexMap.put(tid, 1);
		globalEventID = 0;
	}

    //Considering that after a read/write operation
    //we can update the store in other threads
    //this will make a mistake
    public static void updateFieldAcc(int ID, final Object o, int SID,
            final Object v, final boolean write, long tid) {


        //Scheduler.beforeFieldAccess(!write, "owner", "name", "desc");

        Trace trace = MCRStrategy.getTrace();
        
        

        // Use <= instead of < because currentIndex is increased after this
        // function call
        if ( MCRStrategy.schedulePrefix.size() <= currentIndex++|| MCRStrategy.fullTrace) {
            // Already reached the end of prefix

            //Alan
            StackTraceElement frame = Thread.currentThread().getStackTrace()[2];
            String fileName = frame.getFileName();
            int line = frame.getLineNumber();
            String label = fileName+":"+Integer.toString(line);
            
            globalEventID++;
            if (isPrim(v)) {
                if (write) {
                    WriteNode writeNode = new WriteNode(globalEventID, tid,
                              ID, o == null ? "." + SID
                            : System.identityHashCode(o) + "." + SID, v + "",
                            AbstractNode.TYPE.WRITE,
                            label);
                    trace.addRawNode(writeNode);
                    // db.saveEventToDB(tid, ID,
                    // o==null?"."+SID:hashcode_o+"."+SID,
                    // isPrim(v)?v+"":System.identityHashCode(v)+"_",
                    // write?db.tracetypetable[2]: db.tracetypetable[1]);
                } else {              
                    ReadNode readNode = new ReadNode(globalEventID, tid,
                             ID, o == null ? "." + SID
                            : System.identityHashCode(o) + "." + SID, v + "",
                            AbstractNode.TYPE.READ,
                            label);
                    trace.addRawNode(readNode);
                }
            } else {
                if (write) {
                    WriteNode writeNode = new WriteNode(globalEventID, tid,
                             ID, o == null ? "_."
                            + SID : System.identityHashCode(o) + "_." + SID,
                            System.identityHashCode(v) + "_",
                            AbstractNode.TYPE.WRITE,
                            label);
                    trace.addRawNode(writeNode);
                    // db.saveEventToDB(tid, ID,
                    // o==null?"_."+SID:hashcode_o+"_."+SID,
                    // isPrim(v)?v+"":System.identityHashCode(v)+"_",
                    // write?db.tracetypetable[2]: db.tracetypetable[1]);
                } else {
                    
                 
                    
                    ReadNode readNode = new ReadNode(globalEventID, tid,
                            ID, o == null ? "_."
                            + SID : System.identityHashCode(o) + "_." + SID,
                            System.identityHashCode(v) + "_",
                            AbstractNode.TYPE.READ,
                            label);
                    trace.addRawNode(readNode);
                }
            }

        } else {
            // Not added to trace but update initial memory write.
            if (write) {
                if (isPrim(v)) {
                    
                    trace.updateInitWriteValueToAddress(o == null ? "." + SID
                            : System.identityHashCode(o) + "." + SID, v + "");
                } else {
                    trace.updateInitWriteValueToAddress(o == null ? "_." + SID
                            : System.identityHashCode(o) + "_." + SID,
                            System.identityHashCode(v) + "_");
                }
            }
        }
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

		Trace trace = MCRStrategy.getTrace();
		
		StackTraceElement frame = Thread.currentThread().getStackTrace()[2];
        String fileName = frame.getFileName();
        int line = frame.getLineNumber();
        String label = fileName+":"+Integer.toString(line);
        
        String threadName = Thread.currentThread().getName().toString();
        String type = null;
        if (write) {
            type="write";
        }
        else
            type = "read";
        
        RVRunTime.failure_trace.add(threadName + "_" + label + ":" + type);
	
//        RVRunTime.failure_trace.add(label);
		
		// Use <= instead of < because currentIndex is increased after this
		// function call
		if ( MCRStrategy.schedulePrefix.size() <= currentIndex++|| MCRStrategy.fullTrace) 
		{
//		    StackTraceElement frame = Thread.currentThread().getStackTrace()[2];
//	        String fileName = frame.getFileName();
//	        int line = frame.getLineNumber();
//	        String label = fileName+":"+Integer.toString(line);
		    
			globalEventID++;
			if (isPrim(v)) 
			{
				if (write) 
				{
					WriteNode writeNode = new WriteNode(globalEventID, Thread.currentThread().getId(), ID, 
					        o == null ? "." + SID: System.identityHashCode(o) + "." + SID, 
					        v + "",AbstractNode.TYPE.WRITE,label);
					trace.addRawNode(writeNode);

				} else {	    
					ReadNode readNode = new ReadNode(globalEventID, 
					        Thread.currentThread().getId(), ID, o == null ? "." + SID
							: System.identityHashCode(o) + "." + SID, v + "",
							AbstractNode.TYPE.READ,
							label);
					trace.addRawNode(readNode);
//					if (o==null)
//					    System.out.println(readNode.toString());
				}
			} 
			else {
				if (write) {
					WriteNode writeNode = new WriteNode(globalEventID, 
					        Thread.currentThread().getId(), 
					        ID, 
					        o == null ? "_."+ SID : System.identityHashCode(o) + "_." + SID,
							System.identityHashCode(v) + "_",
							AbstractNode.TYPE.WRITE,
							label);
					trace.addRawNode(writeNode);
					// db.saveEventToDB(tid, ID,
					// o==null?"_."+SID:hashcode_o+"_."+SID,
					// isPrim(v)?v+"":System.identityHashCode(v)+"_",
					// write?db.tracetypetable[2]: db.tracetypetable[1]);
				} else {
					ReadNode readNode = new ReadNode(
					        globalEventID,              //index of this event in the trace
					        Thread.currentThread().getId(),
					        ID,               //id of the variable
					        o == null ? "_."+ SID : System.identityHashCode(o) + "_." + SID,   //addr
							System.identityHashCode(v) + "_",           //value
							AbstractNode.TYPE.READ,
							label);
					trace.addRawNode(readNode);
//					System.out.println(readNode.toString());
				}
			}

		} 
		else {
			// Not added to trace but update initial memory write.
			if (write) {
				if (isPrim(v)) {
				    
					trace.updateInitWriteValueToAddress(o == null ? "." + SID
							: System.identityHashCode(o) + "." + SID, v + "");
				} else {
					trace.updateInitWriteValueToAddress(o == null ? "_." + SID
							: System.identityHashCode(o) + "_." + SID,
							System.identityHashCode(v) + "_");
				}
			}
		}
	}

	public static void logInitialWrite(int ID, final Object o, int SID, final Object v) 
	{

//	    while (MCRStrategy.getTrace() == null);
	    
//	    System.out.println("====================\n\n");
//	    StackTraceElement []aElements = Thread.currentThread().getStackTrace();
//	    for (int i = 0; i < aElements.length; i++) {
//	        System.err.println(aElements[i].toString());
//        }
	    

	    Trace trace = MCRStrategy.getTrace();
	    if (trace ==null) {
	        //when the tested class contains some initializations
	        //the trace is null when mcr is run from the terminal
            return;
        }
//	    while(trace == null){
//	        try {
//                Thread.sleep(100);
//            } catch (InterruptedException e) {
//                // TODO Auto-generated catch block
//                e.printStackTrace();
//            }
//	        Thread.yield();
//	        trace = MCRStrategy.getTrace();
//	    }
	    if (isPrim(v)) {
            
            trace.updateInitWriteValueToAddress(o == null ? "." + SID
                    : System.identityHashCode(o) + "." + SID, v + "");
        } else {
//            String addr = o == null ? "_." + SID : System.identityHashCode(o) + "_." + SID;
//            String value = System.identityHashCode(v) + "_";
            trace.updateInitWriteValueToAddress(o == null ? "_." + SID
                    : System.identityHashCode(o) + "_." + SID,
                    System.identityHashCode(v) + "_");
        }
	}

	public static void logBranch(int ID) {
	}

	public static void logThreadBegin()
	{
	    Scheduler.beginThread();
	}
    public static void logThreadEnd()
    {
        Scheduler.endThread();
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

	    Scheduler.beforeForking((Thread) o);

		long tid = Thread.currentThread().getId();
		Thread t = (Thread) o;

		long tid_t = t.getId();
        String name = null;
		if(threadTidNameMap != null){
            name = threadTidNameMap.get(tid);
        }

		// it's possible that name is NULL, because this thread is started from
		// library: e.g., AWT-EventQueue-0
		if (name == null) {
			name = Thread.currentThread().getName();
			threadTidIndexMap.put(tid, 1);
			threadTidNameMap.put(tid, name);
		}

		int index = threadTidIndexMap.get(tid);

		if (name.equals(MAIN_NAME))
			name = "" + index;
		else
			name = name + "." + index;

		threadTidNameMap.put(tid_t, name);
		threadTidIndexMap.put(tid_t, 1);

		index++;
		threadTidIndexMap.put(tid, index);

		// db.saveEventToDB(tid, ID, ""+tid_t, "", db.tracetypetable[7]);
		StackTraceElement frame = Thread.currentThread().getStackTrace()[2];
        String fileName = frame.getFileName();
        int line = frame.getLineNumber();
        String label = fileName+":"+Integer.toString(line);
        
        String threadName = Thread.currentThread().getName().toString();
        
        RVRunTime.failure_trace.add(threadName + "_" + label + ":start");
		

        if (MCRStrategy.schedulePrefix.size() <= currentIndex++ || MCRStrategy.fullTrace) {
            Trace trace = MCRStrategy.getTrace();
            globalEventID++;
            StartNode startNode = new StartNode(globalEventID, tid, ID, ""
                    + tid_t, AbstractNode.TYPE.START);
            trace.addRawNode(startNode);
        }
	}

	public static void logAfterStart(int ID, final Object o) {
		Scheduler.afterForking((Thread) o);

		long tid = Thread.currentThread().getId();
		Thread t = (Thread) o;
		long tid_t = t.getId();
		
		StackTraceElement frame = Thread.currentThread().getStackTrace()[2];
        String fileName = frame.getFileName();
        int line = frame.getLineNumber();
        String label = fileName+":"+Integer.toString(line);
        
        String threadName = Thread.currentThread().getName().toString();
        
        RVRunTime.failure_trace.add(threadName + "_" + label + ":after_start");
		
		if (MCRStrategy.schedulePrefix.size() <= currentIndex++ || MCRStrategy.fullTrace) {
			Trace trace = MCRStrategy.getTrace();
			globalEventID++;
			StartNode startNode = new StartNode(globalEventID, tid, ID, ""
					+ tid_t, AbstractNode.TYPE.START);
			trace.addRawNode(startNode);
		}
	}

	public static void logSleep()
	{
	       try {
	            Scheduler.performSleep();
	        } catch (Exception e) {
	            e.printStackTrace();
	        }
	}
	public static void logJoin(int ID, final Object o) {
		// db.saveEventToDB(Thread.currentThread().getId(), ID, "" + ((Thread)
		// o).getId(), "", db.tracetypetable[8]);
	    
	    StackTraceElement frame = Thread.currentThread().getStackTrace()[2];
        String fileName = frame.getFileName();
        int line = frame.getLineNumber();
        String label = fileName+":"+Integer.toString(line);
        
        String threadName = Thread.currentThread().getName().toString();
        
        

		try {
			Scheduler.performJoin((Thread) o);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		RVRunTime.failure_trace.add(threadName + "_" + label + ":thread_join");

		if (MCRStrategy.schedulePrefix.size() <= currentIndex++ || MCRStrategy.fullTrace) {
			Trace trace = MCRStrategy.getTrace();
			globalEventID++;
			JoinNode joinNode = new JoinNode(globalEventID, Thread
					.currentThread().getId(), ID, "" + ((Thread) o).getId(),
					AbstractNode.TYPE.JOIN);
			trace.addRawNode(joinNode);
		}
	}

	//the original version
	public static void logWait(int ID, final Object o) {

		if (MCRStrategy.schedulePrefix.size() <= currentIndex || MCRStrategy.fullTrace) {
			Trace trace = MCRStrategy.getTrace();
			globalEventID++;
			WaitNode waitNode = new WaitNode(globalEventID, Thread
					.currentThread().getId(), ID, ""
					+ System.identityHashCode(o), AbstractNode.TYPE.WAIT);
			trace.addRawNode(waitNode);
		}
		
		StackTraceElement frame = Thread.currentThread().getStackTrace()[2];
        String fileName = frame.getFileName();
        int line = frame.getLineNumber();
        String label = fileName+":"+Integer.toString(line);
        
        String threadName = Thread.currentThread().getName().toString();
        
        RVRunTime.failure_trace.add(threadName + "_" + label + ":wait");

		int wait = 0;
		try {
			wait = Scheduler.performOnlyWait(o);       //wait for notify
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
//		currentIndex++;
//		System.err.println("wait be notified");
		
//		System.err.println(" after perform only wait");

		/*
		 * there is a problem here
		 * what if another lock event gets this lock first???
		 * Because this lock is a invisible lock when it first executed
		 */
//		System.err.println(RVRunTime.currentIndex);
		Scheduler.performLock(o, wait);  //acquire the release signal
//		System.err.println(RVRunTime.currentIndex);
		
//		if (Configuration.DEBUG) {
//            System.err.println("Log the lock after the wait!");
//        }
		
		if (MCRStrategy.schedulePrefix.size() <= currentIndex++ || MCRStrategy.fullTrace) {
			Trace trace = MCRStrategy.getTrace();
			globalEventID++;
			LockNode lockNode = new LockNode(globalEventID, Thread
					.currentThread().getId(), ID, ""
					+ System.identityHashCode(o), AbstractNode.TYPE.LOCK);
			trace.addRawNode(lockNode);
		}
	}

	public static void logNotify(int ID, final Object o) {
	    
	    
		long notifiedThreadId = Scheduler.performNotify(o);
		
		StackTraceElement frame = Thread.currentThread().getStackTrace()[2];
        String fileName = frame.getFileName();
        int line = frame.getLineNumber();
        String label = fileName+":"+Integer.toString(line);
        
        String threadName = Thread.currentThread().getName().toString();
        
        RVRunTime.failure_trace.add(threadName + "_" + label + ":notify");
//		System.err.println("notify " + RVRunTime.currentIndex);

		if (MCRStrategy.schedulePrefix.size() <= currentIndex++ || MCRStrategy.fullTrace) {
			Trace trace = MCRStrategy.getTrace();
			globalEventID++;
			NotifyNode notifyNode = new NotifyNode(globalEventID, Thread
					.currentThread().getId(), ID, ""
					+ System.identityHashCode(o), notifiedThreadId, AbstractNode.TYPE.NOTIFY);
			trace.addRawNode(notifyNode);
		}
	}

	public static void logNotifyAll(int ID, final Object o) {

	    long notifiedThreadId = Scheduler.performNotifyAll(o);
	    
	    addEventToTrace();

		if (MCRStrategy.schedulePrefix.size() <= currentIndex++ || MCRStrategy.fullTrace) {
			Trace trace = MCRStrategy.getTrace();
			globalEventID++;
			//TODO: Check how to do for notifyAll
			NotifyNode notifyNode = new NotifyNode(globalEventID, 
			        Thread.currentThread().getId(), 
			        ID, 
			        ""+ System.identityHashCode(o), 
			        notifiedThreadId, 
			        AbstractNode.TYPE.NOTIFY);
			trace.addRawNode(notifyNode);
		}
	}
 
	public static void logStaticSyncLock(int ID, int SID) {

        Scheduler.performLock(getObject(SID));
        
        StackTraceElement frame = Thread.currentThread().getStackTrace()[2];
        String fileName = frame.getFileName();
        int line = frame.getLineNumber();
        String label = fileName+":"+Integer.toString(line);
        
        String threadName = Thread.currentThread().getName().toString();
        
        RVRunTime.failure_trace.add(threadName + "_" + label + ":StaticSyncLock");

		if (MCRStrategy.schedulePrefix.size() <= currentIndex++ || MCRStrategy.fullTrace) {
			Trace trace = MCRStrategy.getTrace();
			globalEventID++;
			LockNode lockNode = new LockNode(globalEventID, Thread
					.currentThread().getId(), ID, "" + SID,
					AbstractNode.TYPE.LOCK);
			trace.addRawNode(lockNode);
		}
	}

	public static void logStaticSyncUnlock(int ID, int SID) {

        Scheduler.performUnlock(getObject(SID));
        
        StackTraceElement frame = Thread.currentThread().getStackTrace()[2];
        String fileName = frame.getFileName();
        int line = frame.getLineNumber();
        String label = fileName+":"+Integer.toString(line);
        
        String threadName = Thread.currentThread().getName().toString();
        
        RVRunTime.failure_trace.add(threadName + "_" + label + ":StaticSyncUnlock");
        
		if (MCRStrategy.schedulePrefix.size() <= currentIndex++ || MCRStrategy.fullTrace) {
			Trace trace = MCRStrategy.getTrace();
			globalEventID++;
			UnlockNode unlockNode = new UnlockNode(globalEventID, Thread
					.currentThread().getId(), ID, "" + SID,
					AbstractNode.TYPE.UNLOCK);
			trace.addRawNode(unlockNode);
		}
	}
	
	/**
	 * log the lock events
	 * @param ID
	 * @param lock
	 */
	
	public static void logLock(int ID, final Object lock)
	{
	    //why this blocks the thread if the thread can acquire the lock???
	    Scheduler.performLock(lock);

	    StackTraceElement frame = Thread.currentThread().getStackTrace()[2];
        String fileName = frame.getFileName();
        int line = frame.getLineNumber();
        String label = fileName+":"+Integer.toString(line);
        
        String threadName = Thread.currentThread().getName().toString();
        
        RVRunTime.failure_trace.add(threadName + "_" + label + ":Lock");
	    
//	    if (Configuration.DEBUG) {
//            System.err.println("Log the lock by thread: "+ Thread.currentThread().getId());
//        }

        if (MCRStrategy.schedulePrefix.size() <= currentIndex++ || MCRStrategy.fullTrace) {
            Trace trace = MCRStrategy.getTrace();
            globalEventID++;
            LockNode lockNode = new LockNode(globalEventID, Thread
                    .currentThread().getId(), ID, ""
                    + System.identityHashCode(lock), AbstractNode.TYPE.LOCK);
            trace.addRawNode(lockNode);
        }
	}
	public static void logUnlock(int ID, final Object lock)
	{
	      Scheduler.performUnlock(lock);
	      
	      StackTraceElement frame = Thread.currentThread().getStackTrace()[2];
	        String fileName = frame.getFileName();
	        int line = frame.getLineNumber();
	        String label = fileName+":"+Integer.toString(line);
	        
	        String threadName = Thread.currentThread().getName().toString();
	        
	        RVRunTime.failure_trace.add(threadName + "_" + label + ":UnLock");
	       
//	       if (Configuration.DEBUG) {
//            System.err.println("log unlock by thread: "+ Thread.currentThread().getId());
//	       }

        if (MCRStrategy.schedulePrefix.size() <= currentIndex++ || MCRStrategy.fullTrace) {
            Trace trace = MCRStrategy.getTrace();
            globalEventID++;
            UnlockNode unlockNode = new UnlockNode(globalEventID, Thread
                    .currentThread().getId(), ID, ""
                    + System.identityHashCode(lock), AbstractNode.TYPE.UNLOCK);
            trace.addRawNode(unlockNode);
        }
	}

	public static void logArrayAcc(int ID, final Object o, int index,
			final Object v, final boolean write) {

	       //Scheduler.beforeArrayAccess(!write);
		Trace trace = MCRStrategy.getTrace();
        String label = addEventToTrace();
       
		if (MCRStrategy.schedulePrefix.size() <= currentIndex++ || MCRStrategy.fullTrace) {
			// Already reached the end of prefix
		    
			globalEventID++;
			if (isPrim(v)) {
				if (write) {
					WriteNode writeNode = new WriteNode(globalEventID, Thread
							.currentThread().getId(), ID,
							System.identityHashCode(o) + "_" + index, v + "",
							AbstractNode.TYPE.WRITE,
							label);
					trace.addRawNode(writeNode);
				} else {
					ReadNode readNode = new ReadNode(globalEventID, Thread
							.currentThread().getId(), ID,
							System.identityHashCode(o) + "_" + index, v + "",
							AbstractNode.TYPE.READ,
							label);
					trace.addRawNode(readNode);
				}
			} else {
				if (write) {
					WriteNode writeNode = new WriteNode(globalEventID, Thread
							.currentThread().getId(), ID,
							System.identityHashCode(o) + "_" + index,
							System.identityHashCode(v) + "_",
							AbstractNode.TYPE.WRITE,
							label);
					trace.addRawNode(writeNode);
				} else {
					ReadNode readNode = new ReadNode(globalEventID, Thread
							.currentThread().getId(), ID,
							System.identityHashCode(o) + "_" + index,
							System.identityHashCode(v) + "_",
							AbstractNode.TYPE.READ,
							label);
					trace.addRawNode(readNode);
				}
			}

		} else {
			// Not added to trace but update initial memory write.
			if (write) {
				if (isPrim(v)) {
					trace.updateInitWriteValueToAddress(
							System.identityHashCode(o) + "_" + index, v + "");
				} else {
					trace.updateInitWriteValueToAddress(
							System.identityHashCode(o) + "_" + index,
							System.identityHashCode(v) + "_");
				}
			}
		}

		// db.saveEventToDB(tid, ID, System.identityHashCode(o)+"_"+index,
		// isPrim(v)?v+"":System.identityHashCode(v)+"_",
		// write?db.tracetypetable[2]: db.tracetypetable[1]);

	}

	//判断对象o是否是以下的类型之一 Integer  Long  Byte  Boolean  Float  Double  Short  Character
	private static boolean isPrim(Object o) {
		if (o instanceof Integer || o instanceof Long || o instanceof Byte
				|| o instanceof Boolean || o instanceof Float
				|| o instanceof Double || o instanceof Short
				|| o instanceof Character)
			return true;

		return false;
	}	
	
	//add the event to the trace, so that we can print the trace when a failure happens
	public static String addEventToTrace() {
	    
	    StackTraceElement frame = Thread.currentThread().getStackTrace()[2];
	    String fileName = frame.getFileName();
	    int line = frame.getLineNumber();
	    String label = fileName+":"+Integer.toString(line);
	    
	    String threadName = Thread.currentThread().getName().toString();
	    
	    RVRunTime.failure_trace.add(threadName + "_" + label);
	    
	    return label;
    }
}
