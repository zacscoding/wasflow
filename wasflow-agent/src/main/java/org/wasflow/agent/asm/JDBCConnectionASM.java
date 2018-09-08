package org.wasflow.agent.asm;

import java.util.HashSet;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.LocalVariablesSorter;
import org.wasflow.agent.ClassDesc;
import org.wasflow.agent.LOGGER;
import org.wasflow.agent.trace.TraceSQL;
import org.wasflow.util.StringUtil;

/**
 * Connection의 rollback을 체크하는 ASM
 *
 * @author zacconding
 * @Date 2018-09-08
 * @GitHub : https://github.com/zacscoding
 */
public class JDBCConnectionASM implements IASM, Opcodes {

    public HashSet<String> targets = new HashSet<String>();

    public JDBCConnectionASM() {
        targets.add("oracle/jdbc/driver/PhysicalConnection");
        targets.add("com/microsoft/sqlserver/jdbc/SQLServerConnection");
        targets.add("org/postgresql/jdbc/PgConnection");
    }

    @Override
    public ClassVisitor transform(ClassVisitor cv, String className, ClassDesc classDesc) {
        if (targets.contains(className)) {
            LOGGER.info("!@ find target class name : " + className);
            return new JDBCConnectionCV(cv);
        }
        return cv;
    }
}


/* ======================================== ClassVisitor ======================================== */
class JDBCConnectionCV extends ClassVisitor implements Opcodes {

    public JDBCConnectionCV(ClassVisitor cv) {
        super(ASM5, cv);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
        if (mv == null) {
            return mv;
        }

        if ("rollback".equals(name)) {
            return new JDBCConnRollbackMV(access, desc, mv);
        }
        return mv;
    }
}

/* ======================================== MethodVisitor ======================================== */
class JDBCConnRollbackMV extends LocalVariablesSorter implements Opcodes {

    private static final String TRACEMAIN = StringUtil.parseClassName(TraceSQL.class);

    protected JDBCConnRollbackMV(int access, String desc, MethodVisitor mv) {
        super(ASM5, access, desc, mv);
    }

    @Override
    public void visitInsn(int opcode) {
        if ((opcode >= IRETURN && opcode <= RETURN)) {
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, TRACEMAIN, "rollback", "()V", false);
        }
        mv.visitInsn(opcode);
    }
}