import jdk.internal.org.objectweb.asm.ClassReader;
import jdk.internal.org.objectweb.asm.ClassWriter;
import jdk.internal.org.objectweb.asm.Opcodes;
import jdk.internal.org.objectweb.asm.tree.*;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.HashSet;
import java.util.Set;

public class RVInstrumentor {

    /**
     * Constants
     */
    private static final String SLASH = "/";
    private static final String INSTRUMENTATION_PACKAGES_DEFAULT = "default";

    /**
     *  packages and classes which needed to instrument or ignore
     */
    private static final Set<String> pckgPrefixesToIgnore = new HashSet<String>();


    /**
     * 在main函数执行前，执行的函数
     *
     * @param options
     * @param ins
     */
    public static void premain(String options, Instrumentation ins) {


        //注册我自己的字节码转换器
        ins.addTransformer(new ClassFileTransformer() {
            @Override
            public byte[] transform(ClassLoader loader,
                                    String className,
                                    Class<?> classBeingRedefined,
                                    ProtectionDomain protectionDomain,
                                    byte[] classfileBuffer) throws IllegalClassFormatException {

                /**
                 * 首先判断当前的类是否需要被插桩
                 * If the package is included in the packages to instrument,
                 * or the class is included in the classes to instrument,
                 * instrument the class
                 */
                return new byte[0];
            }
        });
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


        return true;
    }

}
