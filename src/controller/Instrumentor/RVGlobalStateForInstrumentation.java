package controller.Instrumentor;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.objectweb.asm.ClassReader;


public class RVGlobalStateForInstrumentation {

	public static final String RUNNABLE_CLASS_NAME = "java/lang/Runnable";
	public static final String OBJECT_CLASS_NAME = "java/lang/Object";
	public static final String THREAD_CLASS_NAME = "java/lang/Thread";
	public static final String REENTRANTLOCK_NAME = "java/util/concurrent/locks/ReentrantLock";


	public static RVGlobalStateForInstrumentation instance = new RVGlobalStateForInstrumentation();
	public static HashMap<Integer, String> variableIdSigMap = new HashMap<Integer, String>();
	public static HashMap<Integer, String> stmtIdSigMap = new HashMap<Integer, String>();
	public HashSet<String> volatilevariables = new HashSet<String>();

	public ConcurrentHashMap<String,Integer> variableIdMap = new ConcurrentHashMap<String,Integer>();
	public HashMap<Integer,String> arrayIdMap = new HashMap<Integer,String>();

	public HashSet<String> volatileVariables = new HashSet<String>();
	public ConcurrentHashMap<String,Integer> stmtSigIdMap = new ConcurrentHashMap<String,Integer>();

	public void saveObjectToFile(Object o, String filename)
	{
		// save the object to file
		FileOutputStream fos = null;
		ObjectOutputStream out = null;
		try {
		  fos = new FileOutputStream(filename);
		  out = new ObjectOutputStream(fos);
		  out.writeObject(o);

		  out.close();
		} catch (Exception ex) {
		  ex.printStackTrace();
		}


	}

	public void addVolatileVariable(String sig)
	{
		if (!volatileVariables.contains(sig)) {
			synchronized (volatileVariables) {
				if (!volatileVariables.contains(sig)) {
					volatileVariables.add(sig);
					//unsavedVolatileVariables.put(sig, true);
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
	public int getArrayLocationId(String sig)
	{
		int id = getLocationId(sig);

		arrayIdMap.put(id,sig);

		return id;
	}
	public String getArrayLocationSig(int id)
	{
		return arrayIdMap.get(id);
	}

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

    public boolean isThreadClass(String cname) {
        while (!cname.equals(OBJECT_CLASS_NAME)) {
            if (cname.equals(THREAD_CLASS_NAME))
                return true;
            try {
                ClassReader cr = new ClassReader(cname);
                cname = cr.getSuperName();
            } catch (IOException e) {

                return false;
            }
        }
        return false;
    }
	public boolean isRunnableClass(String cname) {
		while (!cname.equals(OBJECT_CLASS_NAME)) {

			try {
				ClassReader cr = new ClassReader(cname);
				
				String[] interfaces =  cr.getInterfaces();
				for(int i=0;i<interfaces.length;i++)
				    if(interfaces[i].equals(RUNNABLE_CLASS_NAME))return true;
				
				cname = cr.getSuperName();
				
			} catch (IOException e) {

				return false;
			}
		}
		return false;
	}
}
