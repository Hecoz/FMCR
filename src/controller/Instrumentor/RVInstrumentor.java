import jdk.internal.org.objectweb.asm.ClassReader;
import jdk.internal.org.objectweb.asm.ClassWriter;
import jdk.internal.org.objectweb.asm.Opcodes;
import jdk.internal.org.objectweb.asm.tree.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RVInstrumentor {

    /**
     * Constants
     */
    private static final String SLASH = "/";
    private static final String INSTRUMENTATION_PACKAGES_DEFAULT = "default";
    private static final String DOT = ".";
    private static final String SEMICOLON = ";";    //通过 ; 来分割 properties 中的字符串
    private static final String JUC_DOTS = "java.util.concurrent";

    public static String logClass;    //根据指定的模型检查器选择运行时插桩程序

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


    /**
     * 在main函数执行前，执行的函数
     *
     * @param options
     * @param ins
     */
    public static void premain(String options, Instrumentation inst) {

        /**
         *  01: get FMCRProperties
         */
        FMCRProperties fmcrProperties = FMCRProperties.getFmcrProperties();
        //IGNORE
        storePropertyValues(fmcrProperties.getProperty(FMCRProperties.INSTRUMENTATION_PACKAGES_IGNORE_PREFIXES_KEY),pckgPrefixesToIgnore);
        storePropertyValues(fmcrProperties.getProperty(FMCRProperties.INSTRUMENTATION_PACKAGES_IGNORE_KEY),pckgsToIgnore);
        storePropertyValues(fmcrProperties.getProperty(FMCRProperties.INSTRUMENTATION_CLASSES_IGNORE_PREFIXES_KEY),classPrefixesToIgnore);
        storePropertyValues(fmcrProperties.getProperty(FMCRProperties.INSTRUMENTATION_CLASSES_IGNORE_KEY),classesToIgnore);
        //ALLOW
        storePropertyValues(fmcrProperties.getProperty(FMCRProperties.INSTRUMENTATION_PACKAGES_ALLOW_PREFIXES_KEY),pckgPrefixesToAllow);
        storePropertyValues(fmcrProperties.getProperty(FMCRProperties.INSTRUMENTATION_PACKAGES_ALLOW_KEY),pckgsToAllow);
        storePropertyValues(fmcrProperties.getProperty(FMCRProperties.INSTRUMENTATION_CLASSES_IGNORE_PREFIXES_KEY),classPrefixesToAllow);
        storePropertyValues(fmcrProperties.getProperty(FMCRProperties.INSTRUMENTATION_CLASSES_ALLOW_KEY),classesToAllow);

        /**
         *  02: set the memory model
         */
        String memory_model = System.getProperty("memory_model");
        if(memory_model != null && !memory_model.isEmpty()){

            RVConfig.instance.mode = memory_model;
        }

        /**
         *  03: setup the configuration here, Configuration is located in engine.config
         */
        final boolean debug = Boolean.parseBoolean(System.getProperty("debug")); //debug :false
        final boolean static_opt = Boolean.parseBoolean(System.getProperty("static_opt"));  //static_opt :false
        Configuration.DEBUG = debug;
        Configuration.Optimize = static_opt;
        Configuration.setup();


        /**
         * 04: choose the runtime instrumentation based on the model checker specified
         */
        logClass = "controller/Instrumentor/RVRunTime";

        //注册我自己的字节码转换器
        inst.addTransformer(new ClassFileTransformer() {
            @Override
            public byte[] transform(ClassLoader loader,
                                    String className,
                                    Class<?> classBeingRedefined,
                                    ProtectionDomain protectionDomain,
                                    byte[] classfileBuffer) throws IllegalClassFormatException {
                try {

                    /**
                     * 首先判断当前的类是否需要被插桩
                     * If the package is included in the packages to instrument,
                     * or the class is included in the classes to instrument,
                     * instrument the class
                     */
                    if(shouldInstrumentClass(className)){

                        //System.out.println("Instrument:" + className);
                        ClassReader classReader = new ClassReader(classfileBuffer); //bytes is the .class we are going to read
                        ClassWriter classWriter = new ExtendedClassWriter(classReader, ClassWriter.COMPUTE_FRAMES);//ClassWriter.COMPUTE_FRAMES 值为2

                        //RVSharedAccessEventsClassTransformer Adapter 插桩类 继承classVisitor
                        RVSharedAccessEventsClassTransformer rvsharedAccessEventsTransformer = new RVSharedAccessEventsClassTransformer(classWriter);

                        classReader.accept(rvsharedAccessEventsTransformer, ClassReader.EXPAND_FRAMES);

                        classfileBuffer = classWriter.toByteArray();


                        // - - - - - - - - - -  输出插桩后的class文件 - - - - - - - - - - - - - - - - - - -
                        File file = new File("./src/test/ASM-Test/Class" + className + ".class");
                        FileOutputStream fOutputStream;
                        try {
                            fOutputStream = new FileOutputStream(file);
                            fOutputStream.write(classfileBuffer);
                            fOutputStream.close();
                        } catch (IOException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                        // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
                    }
                } catch (Throwable th) {
                    th.printStackTrace();
                    System.err.println(th.getMessage());
                }
                return classfileBuffer;
            }
        },true);

        /** Re-transform already loaded java.util.concurrent classes */
//        try {
//            List<Class<?>> classesToReTransform = new ArrayList<Class<?>>();
//            for (Class<?> loadedClass : inst.getAllLoadedClasses()) {
//
//                if (inst.isModifiableClass(loadedClass) && loadedClass.getPackage().getName().startsWith(JUC_DOTS)) {
//                    classesToReTransform.add(loadedClass);
//                }
//            }
//            inst.retransformClasses(classesToReTransform.toArray(new Class<?>[classesToReTransform.size()]));
//        } catch (UnmodifiableClassException e) {
//            e.printStackTrace();
//            System.err.println("Unable to modify a pre-loaded java.util.concurrent class!");
//            System.exit(2);
//        }
    }

    /**
     *  通过类名，判断当前类是否需要被插桩
     *
     *  @param className
     */
    private static boolean shouldInstrumentClass(String className){

        if(className == null)
            return false;

        String pckgName = INSTRUMENTATION_PACKAGES_DEFAULT;
        int lastSlashIndex = className.lastIndexOf(SLASH);

        //get the package name
        if(lastSlashIndex != -1){

            pckgName = className.substring(0,lastSlashIndex);
        }

        // Phase 1 - check if explicitly allowed
        if(classesToAllow.contains(className)){

            packagesThatWereInstrumented.add(pckgName);
            return true;
        }
        // Phase 2 - check if prefix is allowed
        for(String classPrefix : classPrefixesToAllow){

            if(className.startsWith(classPrefix)){

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
        if (classesToIgnore.contains(className)) {
            packagesThatWereNOTInstrumented.add(pckgName);
            return false;
        }
        if (pckgsToIgnore.contains(pckgName)) {
            packagesThatWereNOTInstrumented.add(pckgName);
            return false;
        }
        for (String classPrefix : classPrefixesToIgnore) {
            if (className.startsWith(classPrefix)) {
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

    /**
     *  存储属性值
     */
    private static void storePropertyValues(String values,Set<String> toSet){

        if(values != null){

            String[] split = values.split(SEMICOLON); //通过 ; 来分割 properties 中的字符串

            for (String val : split) {

                val = val.replace(DOT, SLASH).trim();// 将得到的路径中的 . 替换成 /
                if (!val.isEmpty()) {

                    toSet.add(val);
                }
            }
        }
    }
}
