import jdk.internal.org.objectweb.asm.Label;
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

    @Override
    public void visitMaxs(int maxStack, int maxLocals) {

        mv.visitMaxs(maxStack + 5, maxindex_cur+2);//may change to ...
    }

    @Override
    public void visitLineNumber(int line, Label start) {
        line_cur = line;
        mv.visitLineNumber(line, start);
    }



    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String desc,boolean b) {

        String sig_loc = source + "|" + (className+"|"+methodSignature+"|"+line_cur).replace("/", ".");
        int ID  = RVGlobalStateForInstrumentation.instance.getLocationId(sig_loc);

        //System.out.println("owner:" + owner + "    name:" + name + "    desc:" + desc);

        switch (opcode){

            case INVOKEVIRTUAL:

                if(RVGlobalStateForInstrumentation.instance.isThreadClass(owner)) {

                    /**
                     * INVOKESTATIC  静态
                     * RVInstrumentor.logClass  类名
                     * RVConfig.instance.LOG_THREAD_BEFORE_START   方法名
                     * RVConfig.instance.DESC_LOG_THREAD_START     方法字节吗
                     */
                    // 线程 start 插桩
                    if(name.equals("start") && desc.equals("()V")) {

                        maxindex_cur++;
                        int index = maxindex_cur;
                        mv.visitInsn(DUP);
                        mv.visitVarInsn(ASTORE, index);
                        addBipushInsn(mv,ID);
                        mv.visitVarInsn(ALOAD, index);

                        mv.visitMethodInsn(INVOKESTATIC, RVInstrumentor.logClass,
                                RVConfig.instance.LOG_THREAD_BEFORE_START,
                                RVConfig.instance.DESC_LOG_THREAD_START,b);

                        mv.visitMethodInsn(opcode, owner, name, desc,b);
                    }else if(name.equals("join") &&desc.equals("()V")) {
                        //线程 join 插桩
                        int index = maxindex_cur;
                        mv.visitVarInsn(ASTORE, index);

                        addBipushInsn(mv,ID);
                        mv.visitVarInsn(ALOAD, index);
                        mv.visitMethodInsn(INVOKESTATIC, RVInstrumentor.logClass,
                                RVConfig.instance.LOG_THREAD_JOIN,
                                RVConfig.instance.DESC_LOG_THREAD_JOIN,b);

                    }else {
                        mv.visitMethodInsn(opcode, owner, name, desc,b);
                    }
                }else{

                    //线程等待
                    if(name.equals("wait") &&desc.equals("()V")) {

                        maxindex_cur++;
                        int index = maxindex_cur;
                        //mv.visitInsn(DUP);
                        mv.visitVarInsn(ASTORE, index);

                        addBipushInsn(mv,ID);
                        mv.visitVarInsn(ALOAD, index);
                        mv.visitMethodInsn(INVOKESTATIC, RVInstrumentor.logClass,
                                RVConfig.instance.LOG_WAIT,
                                RVConfig.instance.DESC_LOG_WAIT,b);

                    } else if (name.equals("notify") && desc.equals("()V")) {

                        maxindex_cur++;
                        int index = maxindex_cur;
                        mv.visitVarInsn(ASTORE, index);

                        addBipushInsn(mv, ID);
                        mv.visitVarInsn(ALOAD, index);
                        mv.visitMethodInsn(INVOKESTATIC, RVInstrumentor.logClass,
                                RVConfig.instance.LOG_NOTIFY,
                                RVConfig.instance.DESC_LOG_NOTIFY,b);

                    } else if (name.equals("notifyAll") && desc.equals("()V")) {

                        maxindex_cur++;
                        int index = maxindex_cur;
                        mv.visitVarInsn(ASTORE, index);

                        addBipushInsn(mv, ID);
                        mv.visitVarInsn(ALOAD, index);
                        mv.visitMethodInsn(INVOKESTATIC, RVInstrumentor.logClass,
                                RVConfig.instance.LOG_NOTIFY_ALL,
                                RVConfig.instance.DESC_LOG_NOTIFY,b);
                    }else if (name.equals("lock") && desc.equals("()V")) {

                        maxindex_cur++;
                        int index = maxindex_cur;
                        mv.visitVarInsn(ASTORE, index);

                        addBipushInsn(mv, ID);
                        mv.visitVarInsn(ALOAD, index);

                        mv.visitMethodInsn(INVOKESTATIC, RVInstrumentor.logClass,
                                RVConfig.instance.LOG_LOCK_INSTANCE,
                                RVConfig.instance.DESC_LOG_LOCK_INSTANCE,b);
                    }else if (name.equals("unlock") && desc.equals("()V")) {

                        maxindex_cur++;
                        int index = maxindex_cur;
                        mv.visitVarInsn(ASTORE, index);// objectref
                        addBipushInsn(mv, ID);
                        mv.visitVarInsn(ALOAD, index);

                        mv.visitMethodInsn(INVOKESTATIC, RVInstrumentor.logClass,
                                RVConfig.instance.LOG_UNLOCK_INSTANCE,
                                RVConfig.instance.DESC_LOG_UNLOCK_INSTANCE,b);
                    }else if (name.equals("lockInterruptibly") && desc.equals("()V")) {

                        maxindex_cur++;
                        int index = maxindex_cur;
                        mv.visitVarInsn(ASTORE, index);// objectref

                        addBipushInsn(mv, ID);
                        mv.visitVarInsn(ALOAD, index);

                        mv.visitMethodInsn(INVOKESTATIC, RVInstrumentor.logClass,
                                RVConfig.instance.LOG_LOCK_INSTANCE,
                                RVConfig.instance.DESC_LOG_LOCK_INSTANCE,b);
                    }else
                        mv.visitMethodInsn(opcode, owner, name, desc);
                }
                break;

            case INVOKESTATIC:

                if ((RVGlobalStateForInstrumentation.instance.isThreadClass(owner))
                        &&name.equals("sleep") && (desc.equals(("(J)V")) || desc.equals("(JI)V"))) {
                    mv.visitMethodInsn(INVOKESTATIC, RVInstrumentor.logClass,
                            RVConfig.instance.LOG_THREAD_SLEEP,
                            RVConfig.instance.DESC_LOG_THREAD_SLEEP,b);
                }

            case INVOKESPECIAL:

            case INVOKEINTERFACE:
                if (name.equals("lock") && desc.equals("()V")) {

                    maxindex_cur++;
                    int index = maxindex_cur;
                    mv.visitVarInsn(ASTORE, index);// objectref

                    addBipushInsn(mv, ID);
                    mv.visitVarInsn(ALOAD, index);

                    mv.visitMethodInsn(INVOKESTATIC, RVInstrumentor.logClass,
                            RVConfig.instance.LOG_LOCK_INSTANCE,
                            RVConfig.instance.DESC_LOG_LOCK_INSTANCE,b);
                }
                else if (name.equals("unlock") && desc.equals("()V")) {

                    maxindex_cur++;
                    int index = maxindex_cur;
                    mv.visitVarInsn(ASTORE, index);// objectref
                    addBipushInsn(mv, ID);
                    mv.visitVarInsn(ALOAD, index);

                    mv.visitMethodInsn(INVOKESTATIC, RVInstrumentor.logClass,
                            RVConfig.instance.LOG_UNLOCK_INSTANCE,
                            RVConfig.instance.DESC_LOG_UNLOCK_INSTANCE,b);
                }
                else {
                    mv.visitMethodInsn(opcode, owner, name, desc,b);
                }
                break;
            default:
                System.err.println("Unknown method invocation opcode "+opcode);
                System.exit(1);
        }


        super.visitMethodInsn(opcode, owner, name, desc, b);
    }

    private void addBipushInsn(MethodVisitor mv, int val) {
        switch (val) {
            case 0:
                mv.visitInsn(ICONST_0);
                break;
            case 1:
                mv.visitInsn(ICONST_1);
                break;
            case 2:
                mv.visitInsn(ICONST_2);
                break;
            case 3:
                mv.visitInsn(ICONST_3);
                break;
            case 4:
                mv.visitInsn(ICONST_4);
                break;
            case 5:
                mv.visitInsn(ICONST_5);
                break;
            default:
                mv.visitLdcInsn(new Integer(val));
                break;
        }
    }

}
