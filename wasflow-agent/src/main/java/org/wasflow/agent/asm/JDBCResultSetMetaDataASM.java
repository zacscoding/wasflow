package org.wasflow.agent.asm;

import java.util.HashSet;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.LocalVariablesSorter;
import org.wasflow.agent.ClassDesc;
import org.wasflow.agent.LOGGER;
import org.wasflow.agent.trace.TraceResultSet;
import org.wasflow.util.StringUtil;

/**
 * ResultSetMetaData
 *
 * @author zacconding
 * @Date 2018-09-08
 * @GitHub : https://github.com/zacscoding
 */
public class JDBCResultSetMetaDataASM implements IASM, Opcodes {

    public final HashSet<String> targets = new HashSet<String>();

    public JDBCResultSetMetaDataASM() {
        targets.add("oracle/jdbc/driver/OracleResultSetMetaData");
        targets.add("com/microsoft/sqlserver/jdbc/SQLServerResultSetMetaData");
        targets.add("org/postgresql/jdbc/PgResultSetMetaData");
        targets.add("org/postgresql/jdbc2/AbstractJdbc2ResultSetMetaData");
        targets.add("org/postgresql/jdbc3/Jdbc3ResultSetMetaData");
    }

    @Override
    public ClassVisitor transform(ClassVisitor cv, String className, ClassDesc classDesc) {
        if (targets.contains(className)) {
            LOGGER.info("!@ find target cn : " + className);
            return new ResultSetMetaDataCV(cv, className);
        }
        return cv;
    }
}

/* ======================================== ClassVisitor ======================================== */
class ResultSetMetaDataCV extends ClassVisitor implements Opcodes {

    private String className;

    public ResultSetMetaDataCV(ClassVisitor cv, String className) {
        super(ASM5, cv);
        this.className = className;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
        if (mv == null) {
            return mv;
        }

        if ("getColumnLabel".equals(name) && "(I)Ljava/lang/String;".equals(desc)) {
            return new ResultSetMetaDataMV(access, desc, mv, 0);
        } else if ("getColumnCount".equals(name) && "()I".equals(desc)) {
            return new ResultSetMetaDataMV(access, desc, mv, 1);
        }

        return mv;
    }
}

/* ======================================== ResultSetMetaDataMV ======================================== */
class ResultSetMetaDataMV extends LocalVariablesSorter implements org.objectweb.asm.Opcodes {

    private static final String TRACESQL = StringUtil.parseClassName(TraceResultSet.class);
    private int paramIdx;
    // 0 : getColumnLabel (I)Ljava/lang/String;  /   1 : getColumnCount  ()I
    private int methodType;

    protected ResultSetMetaDataMV(int access, String desc, MethodVisitor mv, int methodType) {
        super(ASM5, access, desc, mv);
        this.methodType = methodType;
    }

    @Override
    public void visitCode() {
        // getColumnLabel : (I)Ljava/lang/String;
        if (methodType == 0) {
            // int param = 매개변수; << 지역변수 할당
            mv.visitVarInsn(ILOAD, 1);
            paramIdx = newLocal(Type.getType(int.class));
            mv.visitVarInsn(Opcodes.ISTORE, paramIdx);
        }
        mv.visitCode();
    }

    @Override
    public void visitInsn(int opcode) {
        if ((opcode >= IRETURN && opcode <= RETURN)) {
            mv.visitInsn(Opcodes.DUP); // Return object
            // getColumnLabel
            if (methodType == 0) {
                mv.visitVarInsn(Opcodes.ILOAD, paramIdx);
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, TRACESQL, "setColumnLabel", "(Ljava/lang/String;I)V", false);
            }
            // getColunmCount
            else if (methodType == 1) {
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, TRACESQL, "setColumnCount", "(I)V", false);
            }
        }
        mv.visitInsn(opcode);
    }

    @Override
    public void visitMaxs(int maxStack, int maxLocals) {
        int addedLocals = 0;
        if (methodType == 0) {
            addedLocals = 1;
        }
        mv.visitMaxs(maxStack, maxLocals + addedLocals);
    }
}