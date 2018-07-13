package controller.Instrumentor;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import controller.MCRProperties;
import controller.exploration.Scheduler;
import engine.config.Configuration;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;


public class RVInstrumentor {

    /**
     * Constants
     */
    private static final String SLASH = "/";
    private static final String DOT = ".";
    private static final String SEMICOLON = ";"; //通过 ; 来分割 properties 中的字符串

    public static String logClass;    //根据指定的模型检查器选择运行时插桩程序
    private static final String JUC_DOTS = "java.util.concurrent";

    private static final String INSTRUMENTATION_PACKAGES_DEFAULT = "default";
    public static final String INSTR_EVENTS_RECEIVER = Scheduler.class.getName().replace(DOT, SLASH);

    /**
     *  packages and classes which needed to instrument or ignore
     */
    //Ignore
    private static final Set<String> pckgPrefixesToIgnore = new HashSet<String>();
    private static final Set<String> pckgsToIgnore = new HashSet<String>();
    private static final Set<String> classPrefixesToIgnore = new HashSet<String>();
    private static final Set<String> classesToIgnore = new HashSet<String>();
    //Allow
    private static final Set<String> pckgPrefixesToAllow = new HashSet<String>();
    private static final Set<String> pckgsToAllow = new HashSet<String>();
    private static final Set<String> classPrefixesToAllow = new HashSet<String>();
    private static final Set<String> classesToAllow = new HashSet<String>();

    /**
     * store the packages that needs to instrument or ignored
     */
    public static Set<String> packagesThatWereInstrumented = new HashSet<String>();
    public static Set<String> packagesThatWereNOTInstrumented = new HashSet<String>();



    //存储属性值
    private static void storePropertyValues(String values, Set<String> toSet) {
        if (values != null) {
            String[] split = values.split(SEMICOLON); //通过 ; 来分割 properties 中的字符串
            for (String val : split) {
                val = val.replace(DOT, SLASH).trim();// 将得到的路径中的 . 替换成 /
                if (!val.isEmpty()) {
                    toSet.add(val);
                }
            }
        }
    }

    private static boolean shouldInstrumentClass(String name) {       //判断是否要对当前运行的类进行插桩
        /*
         * @Alan
         * when using Java 8 for controller and controller-test
         * name could be null
         */
        if (name==null) {
            return false;
        }
        
        String pckgName = INSTRUMENTATION_PACKAGES_DEFAULT;
        int lastSlashIndex = name.lastIndexOf(SLASH);
        // Non-default package
        if (lastSlashIndex != -1) {
            pckgName = name.substring(0, lastSlashIndex);
        }

        // Phase 1 - check if explicitly allowed
        if (classesToAllow.contains(name)) {
            packagesThatWereInstrumented.add(pckgName);
            return true;
        }

        // Phase 2 - check if prefix is allowed
        for (String classPrefix : classPrefixesToAllow) {
            if (name.startsWith(classPrefix)) {
                packagesThatWereInstrumented.add(pckgName);
                return true;
            }
        }

        // Phase 3 - check if package is allowed
        if (pckgsToAllow.contains(pckgName)) {
            packagesThatWereInstrumented.add(pckgName);
            return true;
        }

        // Phase 4 - check if package is allowed via prefix matching
        for (String pckgPrefix : pckgPrefixesToAllow) {
            if (pckgName.startsWith(pckgPrefix)) {
                packagesThatWereInstrumented.add(pckgName);
                return true;
            }
        }

        // Phase 5 - check for any ignores
        if (classesToIgnore.contains(name)) {
            packagesThatWereNOTInstrumented.add(pckgName);
            return false;
        }
        if (pckgsToIgnore.contains(pckgName)) {
            packagesThatWereNOTInstrumented.add(pckgName);
            return false;
        }
        for (String classPrefix : classPrefixesToIgnore) {
            if (name.startsWith(classPrefix)) {
                packagesThatWereNOTInstrumented.add(pckgName);
                return false;
            }
        }
        for (String pckgPrefix : pckgPrefixesToIgnore) {
            //System.out.println(pckgPrefix);
            if (pckgName.startsWith(pckgPrefix)) {
                if (pckgName.startsWith("com/googlecode")) {
                    return true;
                }
                packagesThatWereNOTInstrumented.add(pckgName);
                return false;
            }
        }

        // Otherwise instrument by default
        packagesThatWereInstrumented.add(pckgName);
        return true;
    }

    public static void premain(String agentArgs, Instrumentation inst) {

        /**
         * 01:设置相关的属性值
         */
        MCRProperties mcrProps = MCRProperties.getInstance();
        //IGNORE
        storePropertyValues(mcrProps.getProperty(MCRProperties.INSTRUMENTATION_PACKAGES_IGNORE_PREFIXES_KEY),pckgPrefixesToIgnore);
        storePropertyValues(mcrProps.getProperty(MCRProperties.INSTRUMENTATION_PACKAGES_IGNORE_KEY),pckgsToIgnore);
        storePropertyValues(mcrProps.getProperty(MCRProperties.INSTRUMENTATION_CLASSES_IGNORE_PREFIXES_KEY),classPrefixesToIgnore);
        storePropertyValues(mcrProps.getProperty(MCRProperties.INSTRUMENTATION_CLASSES_IGNORE_KEY),classesToIgnore);
        //ALLOW
        storePropertyValues(mcrProps.getProperty(MCRProperties.INSTRUMENTATION_PACKAGES_ALLOW_PREFIXES_KEY),pckgPrefixesToAllow);
        storePropertyValues(mcrProps.getProperty(MCRProperties.INSTRUMENTATION_PACKAGES_ALLOW_KEY),pckgsToAllow);
        storePropertyValues(mcrProps.getProperty(MCRProperties.INSTRUMENTATION_CLASSES_ALLOW_PREFIXES_KEY),classPrefixesToAllow);
        storePropertyValues(mcrProps.getProperty(MCRProperties.INSTRUMENTATION_CLASSES_ALLOW_KEY),classesToAllow);

        /**
         * 02:设置内存模型，默认是顺序一致性内存模型
         */
        String memory_model = System.getProperty("memory_model"); //返回为null
        if (memory_model != null && !memory_model.isEmpty()) {
            RVConfig.instance.mode = memory_model;
        }

        /**
         * 03:配置文件 在这里设置，
         */
        final boolean debug = Boolean.parseBoolean(System.getProperty("debug")); //debug :false
        final boolean static_opt = Boolean.parseBoolean(System.getProperty("static_opt"));  //static_opt :false
        Configuration.DEBUG = debug;
        Configuration.Optimize = static_opt;
        Configuration.setup();
        
        /**
         * 04:根据指定的模型检查器选择运行时插桩程序
         */
        logClass = "controller/Instrumentor/RVRunTime";

        inst.addTransformer(new ClassFileTransformer() {
            
            //when a class is loaded by the JVM, the function is invoked
            //每当JVM加载一个class文件时调用
            public byte[] transform(ClassLoader l, String className, Class<?> c, ProtectionDomain d, byte[] bytes) throws IllegalClassFormatException {
                try {

                    //首先判断类是否需要被插桩
                    if (shouldInstrumentClass(className)) {
                        System.err.println("Instrumented (这货需要被插桩🙃️) " + className);
                        
                        ClassReader classReader = new ClassReader(bytes); //bytes is the .class we are going to read
                        ClassWriter classWriter = new ExtendedClassWriter(classReader, ClassWriter.COMPUTE_FRAMES);//ClassWriter.COMPUTE_FRAMES 值为2

                        //RVSharedAccessEventsClassTransformer 插桩类 继承classVisitor
                        RVSharedAccessEventsClassTransformer rvsharedAccessEventsTransformer = new RVSharedAccessEventsClassTransformer(classWriter);
                        classReader.accept(rvsharedAccessEventsTransformer, ClassReader.EXPAND_FRAMES);

                        bytes = classWriter.toByteArray();

                        // - - - - - - - - - -  输出插桩后的class文件 - - - - - - - - - - - - - - - - - - -
                        File file = new File("./src/test/ClassesGenerated/" + className.replace("/",".") + ".class");
                        FileOutputStream fOutputStream;
                        try {
                            fOutputStream = new FileOutputStream(file);
                            fOutputStream.write(bytes);
                            fOutputStream.close();
                        } catch (IOException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                        // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
                        
                        /*
                         * If debugging is enabled, check and print out the
                         * instrumented bytecode
                         */
                        if (debug) 
                        {
                            System.out.println("Instrumented " + className);
                        }
                    }
                } catch (Throwable th) {
                    th.printStackTrace();
                    System.err.println(th.getMessage());
                }             
                return bytes;
            }
        }, true);

        /* Re-transform already loaded java.util.concurrent classes */
        try {
            List<Class<?>> classesToReTransform = new ArrayList<Class<?>>();
            for (Class<?> loadedClass : inst.getAllLoadedClasses()) {
                
                if (inst.isModifiableClass(loadedClass) && loadedClass.getPackage().getName().startsWith(JUC_DOTS)) {
                    classesToReTransform.add(loadedClass);
                }
            }
            inst.retransformClasses(classesToReTransform.toArray(new Class<?>[classesToReTransform.size()]));
        } catch (UnmodifiableClassException e) {
            e.printStackTrace();
            System.err.println("Unable to modify a pre-loaded java.util.concurrent class!");
            System.exit(2);
        }
    }
   
}
