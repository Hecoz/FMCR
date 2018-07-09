import jdk.internal.org.objectweb.asm.ClassReader;
import jdk.internal.org.objectweb.asm.ClassWriter;
import jdk.internal.org.objectweb.asm.Opcodes;
import jdk.internal.org.objectweb.asm.tree.*;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

public class RVInstrumentor {


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

}
