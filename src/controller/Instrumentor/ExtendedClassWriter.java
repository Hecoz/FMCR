import jdk.internal.org.objectweb.asm.ClassReader;
import jdk.internal.org.objectweb.asm.ClassWriter;
import jdk.internal.org.objectweb.asm.Opcodes;
import jdk.internal.org.objectweb.asm.Type;

import java.io.IOException;
import java.io.InputStream;

public class ExtendedClassWriter extends ClassWriter {

    public ExtendedClassWriter(ClassReader classReader, int i) {
        super(classReader, i);
    }


    @Override
    protected String getCommonSuperClass(String type1, String type2) {

        ClassLoader classLoader = getClass().getClassLoader();

        if (classLoader == null){

            classLoader = ClassLoader.getSystemClassLoader();
        }
        ClassInfo c, d;
        try {
            c = new ClassInfo(type1, classLoader);
            d = new ClassInfo(type2, classLoader);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }

        //isAssignableFrom 判定此class对象所表示的类或接口与指定的class参数所表示的类或接口是否相同
        if (c.isAssignableFrom(d)) {
            return type1;
        }

        if (d.isAssignableFrom(c)) {
            return type2;
        }

        if (c.isInterface() || d.isInterface()) {
            return "java/lang/Object";
        } else {
            do {
                c = c.getSuperclass();
            } while (!c.isAssignableFrom(d));
            return c.getType().getInternalName();
        }
    }
}

/**
 *  类属性
 */
class ClassInfo{

    private Type type;                  //类型
    private ClassLoader classLoader;
    public int access;                         //权限 ACC_PUBLIC+ACC_ABSTRACT+ACC_INTERFACE
    public String superClass;                  //父类
    public String[] interfaces;                //所实现的接口

    public ClassInfo(final String type, final ClassLoader classLoader) {

        this.type = Type.getObjectType(type);
        this.classLoader = classLoader;

        String s = type.replace('.', '/') + ".class";

        InputStream is = null;
        ClassReader cr;

        try {
            is = classLoader.getResourceAsStream(s);
            cr = new ClassReader(is);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (Exception e) {
                }
            }
        }

        // optimized version
        int h = cr.header;
        ClassInfo.this.access = cr.readUnsignedShort(h);
        char[] buf = new char[2048];
        // String name = cr.readClass( cr.header + 2, buf);

        int v = cr.getItem(cr.readUnsignedShort(h + 4));
        ClassInfo.this.superClass = v == 0 ? null : cr.readUTF8(v, buf);
        ClassInfo.this.interfaces = new String[cr.readUnsignedShort(h + 6)];
        h += 8;
        for (int i = 0; i < interfaces.length; ++i) {
            interfaces[i] = cr.readClass(h, buf);
            h += 2;
        }
    }

    public String getName(){

        return type.getInternalName();
    }

    public Type getType() {
        return type;
    }

    public int getModifiers() {
        return access;
    }

    public ClassInfo getSuperclass() {
        if (superClass == null) {
            return null;
        }
        return new ClassInfo(superClass, classLoader);
    }

    public ClassInfo[] getInterfaces() {
        if (interfaces == null) {
            return new ClassInfo[0];
        }
        ClassInfo[] result = new ClassInfo[interfaces.length];
        for (int i = 0; i < result.length; ++i) {
            result[i] = new ClassInfo(interfaces[i], classLoader);
        }
        return result;
    }

    public boolean isInterface() {
        return (getModifiers() & Opcodes.ACC_INTERFACE) > 0;
    }

    private boolean implementsInterface(final ClassInfo that) {
        for (ClassInfo c = this; c != null; c = c.getSuperclass()) {
            ClassInfo[] tis = c.getInterfaces();
            for (int i = 0; i < tis.length; ++i) {
                ClassInfo ti = tis[i];
                if (ti.type.equals(that.type) || ti.implementsInterface(that)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isSubclassOf(final ClassInfo that) {
        for (ClassInfo c = this; c != null; c = c.getSuperclass()) {
            if (c.getSuperclass() != null && c.getSuperclass().type.equals(that.type)) {
                return true;
            }
        }
        return false;
    }

    public boolean isAssignableFrom(final ClassInfo that) {
        if (this == that) {
            return true;
        }

        if (that.isSubclassOf(this)) {
            return true;
        }

        if (that.implementsInterface(this)) {
            return true;
        }

        if (that.isInterface() && getType().getDescriptor().equals("Ljava/lang/Object;")) {
            return true;
        }

        return false;
    }
}