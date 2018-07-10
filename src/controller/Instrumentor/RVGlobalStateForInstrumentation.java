import jdk.internal.org.objectweb.asm.ClassReader;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;

public class RVGlobalStateForInstrumentation {

    /**
     * Constants
     */
    public static final String RUNNABLE_CLASS_NAME = "java/lang/Runnable";
    public static final String OBJECT_CLASS_NAME = "java/lang/Object";
    public static final String THREAD_CLASS_NAME = "java/lang/Thread";

    public static RVGlobalStateForInstrumentation instance = new RVGlobalStateForInstrumentation();

    //visitField save variables' name and id
    public ConcurrentHashMap<String,Integer> variableIdMap = new ConcurrentHashMap<String,Integer>();
    public static HashMap<Integer, String> variableIdSigMap = new HashMap<Integer, String>();

    //visitField save volatile Variables
    public HashSet<String> volatileVariables = new HashSet<String>();
    //unknown
    public ConcurrentHashMap<String,Integer> stmtSigIdMap = new ConcurrentHashMap<String,Integer>();
    public static HashMap<Integer, String> stmtIdSigMap = new HashMap<Integer, String>();


    /**
     * This call returns the ID of a variable from a map structure
     * @param sig
     * @return
     */
    public int getVariableId(String sig) {

        if(variableIdMap.get(sig)==null) {

            synchronized (variableIdMap) {

                if (variableIdMap.get(sig) == null) {

                    int size = variableIdMap.size() + 1;
                    variableIdMap.put(sig, size);
                    variableIdSigMap.put(size, sig);
                }
            }
        }
        int sid = variableIdMap.get(sig);

        return sid;
    }

    /**
     *  This add volatile variable to the volatileVariables set
     * @param sig
     */
    public void addVolatileVariable(String sig){

        if (!volatileVariables.contains(sig)) {
            synchronized (volatileVariables) {

                if (!volatileVariables.contains(sig)) {
                    volatileVariables.add(sig);
                }
            }
        }
    }


    public int getLocationId(String sig)
    {
        if(stmtSigIdMap.get(sig)==null)
        {
            synchronized (stmtSigIdMap) {
                if(stmtSigIdMap.get(sig)==null) {
                    int size = stmtSigIdMap.size() + 1;
                    stmtSigIdMap.put(sig, size);
                    stmtIdSigMap.put(size,sig);
                }
            }
        }

        return stmtSigIdMap.get(sig);
    }

    /**
     *  判断当前类是否继承了Thread,是返回TRUE,不是返回FALSE
     * @param cname
     * @return
     */
    public boolean isThreadClass(String cname) {

        while (!cname.equals(OBJECT_CLASS_NAME)) {

            if (cname.equals(THREAD_CLASS_NAME))
                return true;

            try {
                ClassReader cr = new ClassReader(cname);
                cname = cr.getSuperName();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                // e.printStackTrace();
                // //if class can not find
                // System.out.println("Class "+cname+" can not find!");
                return false;
            }
        }
        return false;
    }


    /**
     * 判断当前类是否实现了Runnable接口，是返回TRUE,不是返回FALSE
     *
     * @param cname
     * @return
     */
    public boolean isRunnableClass(String cname) {

        while (!cname.equals(OBJECT_CLASS_NAME)) {

            try {
                ClassReader cr = new ClassReader(cname);

                String[] interfaces =  cr.getInterfaces();
                for(int i=0;i<interfaces.length;i++)
                    if(interfaces[i].equals(RUNNABLE_CLASS_NAME))
                        return true;

                cname = cr.getSuperName();

            } catch (IOException e) {
                // TODO Auto-generated catch block
                // e.printStackTrace();
                // //if class can not find
                // System.out.println("Class "+cname+" can not find!");
                return false;
            }
        }
        return false;
    }
}
