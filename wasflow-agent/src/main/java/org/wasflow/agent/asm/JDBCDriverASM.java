package org.wasflow.agent.asm;

import java.util.HashSet;
import java.util.Set;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.LocalVariablesSorter;
import org.wasflow.agent.ClassDesc;
import org.wasflow.agent.LOGGER;
import org.wasflow.agent.trace.TraceSQL;
import org.wasflow.util.StringUtil;

/**
 * java.sql.Driver 인터페이스를 구현한 클래스 변경
 *
 * @author zacconding
 * @Date 2018-09-08
 * @GitHub : https://github.com/zacscoding
 */
public class JDBCDriverASM implements IASM, Opcodes {

    public Set<String> targets = new HashSet<String>();

    private static final String ONE_TARGET = "java/sql/Driver";
    private static final int TARGET_HASH_CODE = ONE_TARGET.hashCode();

    @Override
    public ClassVisitor transform(ClassVisitor cv, String className, ClassDesc classDesc) {
        String[] interfaces = classDesc.interfaces;

        if (interfaces != null && interfaces.length > 0) {
            for (String interfaceName : interfaces) {
                if (interfaceName.hashCode() == TARGET_HASH_CODE) {
                    if (ONE_TARGET.equals(interfaceName)) {
                        LOGGER.info("!@ find target class name : " + className + ", interface : " + interfaceName);
                        return new JDBCDriverCV(cv);
                    }
                }
            }
        }

        return cv;
    }
}

/* ======================================== ClassVisitor ======================================== */
class JDBCDriverCV extends ClassVisitor implements Opcodes {

    public JDBCDriverCV(ClassVisitor cv) {
        super(ASM5, cv);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
        if (mv == null) {
            return mv;
        }

        if ("connect".equals(name) && "(Ljava/lang/String;Ljava/util/Properties;)Ljava/sql/Connection;".equals(desc)) {
            return new JDBCDriverMV(access, desc, mv, true);
        } else if ("close".equals(name) && "()V".equals(desc)) {
            return new JDBCDriverMV(access, desc, mv, false);
        }

        return mv;
    }
}

/* ======================================== MethodVisitor ======================================== */
class JDBCDriverMV extends LocalVariablesSorter implements Opcodes {

    private static final String TRACE_SQL = StringUtil.parseClassName(TraceSQL.class);
    private boolean isConnect;

    protected JDBCDriverMV(int access, String desc, MethodVisitor mv, boolean isConnect) {
        super(ASM5, access, desc, mv);
        this.isConnect = isConnect;
    }

    @Override
    public void visitCode() {
        if (!isConnect) {
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, TRACE_SQL, "closeConnection", "(Ljava/sql/Connection;)V", false);
        }
    }

    @Override
    public void visitInsn(int opcode) {
        if ((opcode >= IRETURN && opcode <= RETURN)) {
            if (isConnect) {
                mv.visitInsn(DUP);
                mv.visitVarInsn(Opcodes.ALOAD, 1);
                mv.visitVarInsn(Opcodes.ALOAD, 2);
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, TRACE_SQL, "putConnection", "(Ljava/sql/Connection;Ljava/lang/String;Ljava/util/Properties;)V", false);
            }
        }

        mv.visitInsn(opcode);
    }

    @Override
    public void visitMaxs(int maxStack, int maxLocals) {
        mv.visitMaxs(maxStack + 2, maxLocals);
    }
}