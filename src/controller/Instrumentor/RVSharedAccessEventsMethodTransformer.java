import jdk.internal.org.objectweb.asm.MethodVisitor;
import jdk.internal.org.objectweb.asm.Opcodes;
import jdk.internal.org.objectweb.asm.Type;
import jdk.internal.org.objectweb.asm.commons.AdviceAdapter;

public class RVSharedAccessEventsMethodTransformer extends AdviceAdapter implements Opcodes {

    /**
     * Constants
     */
    final static String CLASS_INTEGER = "java/lang/Integer";
    final static String CLASS_BOOLEAN = "java/lang/Boolean";
    final static String CLASS_CHAR = "java/lang/Character";
    final static String CLASS_SHORT = "java/lang/Short";
    final static String CLASS_BYTE = "java/lang/Byte";
    final static String CLASS_LONG = "java/lang/Long";
    final static String CLASS_FLOAT = "java/lang/Float";
    final static String CLASS_DOUBLE = "java/lang/Double";

    final static String METHOD_VALUEOF = "valueOf";
    final static String DESC_INTEGER_VALUEOF = "(I)Ljava/lang/Integer;";
    final static String DESC_BOOLEAN_VALUEOF = "(Z)Ljava/lang/Boolean;";
    final static String DESC_BYTE_VALUEOF = "(B)Ljava/lang/Byte;";
    final static String DESC_SHORT_VALUEOF = "(S)Ljava/lang/Short;";
    final static String DESC_CHAR_VALUEOF = "(C)Ljava/lang/Character;";
    final static String DESC_LONG_VALUEOF = "(J)Ljava/lang/Long;";
    final static String DESC_FLOAT_VALUEOF = "(F)Ljava/lang/Float;";
    final static String DESC_DOUBLE_VALUEOF = "(D)Ljava/lang/Double;";

    boolean isInit,isSynchronized,isStatic, possibleRunMethod;
    String className;
    String source;
    String methodName;
    String methodSignature;
    private int maxindex_cur;   //current max index of local variables
    private int line_cur;

    public RVSharedAccessEventsMethodTransformer(
            MethodVisitor mv,
            String source,
            int access,
            String desc,
            String cname,
            String mname,
            String msignature,
            boolean isInit,
            boolean isSynchronized,
            boolean isStatic,
            boolean possibleRunMethod) {

        super(Opcodes.ASM5, mv, access, mname, desc);


        this.source = source == null ? "Unknown" : source;
        this.className = cname;
        this.methodName = mname;
        this.methodSignature = msignature;
        this.isInit = isInit;
        this.isSynchronized = isSynchronized;
        this.isStatic = isStatic;
        this.possibleRunMethod = possibleRunMethod;

        this.maxindex_cur = Type.getArgumentsAndReturnSizes(desc) + 1;
    }
}
