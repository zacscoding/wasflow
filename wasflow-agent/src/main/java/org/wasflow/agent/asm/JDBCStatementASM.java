package org.wasflow.agent.asm;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.LocalVariablesSorter;
import org.wasflow.agent.ClassDesc;
import org.wasflow.agent.LOGGER;
import org.wasflow.agent.trace.TraceStatement;
import org.wasflow.util.StringUtil;

/**
 * Statement
 *
 * @author zacconding
 * @Date 2018-09-08
 * @GitHub : https://github.com/zacscoding
 */
public class JDBCStatementASM implements IASM, Opcodes {

    public final HashSet<String> target = new HashSet<String>();

    public JDBCStatementASM() {
        target.add("oracle/jdbc/driver/OracleStatement");
        target.add("com/microsoft/sqlserver/jdbc/SQLServerStatement");
        target.add("org/postgresql/jdbc/PgStatement");
    }

    @Override
    public ClassVisitor transform(ClassVisitor cv, String className, ClassDesc classDesc) {
        if (target.contains(className)) {
            LOGGER.info("!@ find target class name : " + className);
            return new StatementCV(cv, className);
        }
        return cv;
    }
}

/* ======================================== ClassVisitor ======================================== */
class StatementCV extends ClassVisitor implements Opcodes {

    private String className;

    public StatementCV(ClassVisitor cv, String className) {
        super(ASM5, cv);
        this.className = className;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
        if (mv == null) {
            return mv;
        }

        if (StExecuteMV.contains(name, desc)) {
            return new StExecuteMV(access, desc, mv, name);
        } else if ("addBatch".equals(name)) {
            return new StAddBatchMV(access, desc, mv);
        }

        return mv;
    }
}

/* ======================================== MethodVisitor ======================================== */
class StExecuteMV extends LocalVariablesSorter implements Opcodes {

    private final String TRACE_STATEMENT = StringUtil.parseClassName(TraceStatement.class);
    private static Map<String, String> target = new HashMap<String, String>();
    private String name;
    // 0 : execute, 1 : executeQuery, 2 : executeUpdate, 3 : executeBatch,
    private byte methodType;
    private int addedStack = 0;

    static {
        target.put("execute", "(Ljava/lang/String;)Z");
        target.put("executeQuery", "(Ljava/lang/String;)Ljava/sql/ResultSet;");
        target.put("executeUpdate", "MULTIPLE");
        target.put("executeBatch", "()[I");
    }

    public static boolean contains(String name, String desc) {
        if (target != null) {
            String targetDesc = target.get(name);
            if (targetDesc == null) {
                return false;
            }
            // executeUpdate의 경우 첫번째 매개변수로 String을 가진 경우
            if (name.equals("executeUpdate")) {
                // return desc.startsWith("(Ljava/lang/String;") && desc.endsWith(")I");
                return desc.startsWith("(Ljava/lang/String;");
            } else {
                return desc.equals(targetDesc);
            }
        }

        return false;
    }

    public StExecuteMV(int access, String desc, MethodVisitor mv, String name) {
        super(ASM5, access, desc, mv);
        this.name = name;
        if ("execute".equals(name)) {
            methodType = 0;
        } else if ("executeQuery".equals(name)) {
            methodType = 1;
        } else if ("executeUpdate".equals(name)) {
            methodType = 2;
        } else if ("executeBatch".equals(name)) {
            methodType = 3;
        }
    }

    @Override
    public void visitInsn(int opcode) {
        if ((opcode >= IRETURN && opcode <= RETURN)) {
            switch (methodType) {
                // 0 : execute
                case 0:
                    mv.visitInsn(DUP);
                    mv.visitVarInsn(Opcodes.ALOAD, 1);
                    mv.visitMethodInsn(Opcodes.INVOKESTATIC, TRACE_STATEMENT, name, "(ZLjava/lang/String;)V", false);
                    addedStack = 2;
                    break;
                // executeQuery
                case 1:
                    mv.visitInsn(DUP);
                    mv.visitVarInsn(Opcodes.ALOAD, 1);
                    mv.visitMethodInsn(Opcodes.INVOKESTATIC, TRACE_STATEMENT, name, "(Ljava/sql/ResultSet;Ljava/lang/String;)V", false);
                    addedStack = 1;
                    break;
                // executeUpdate
                case 2:
                    mv.visitInsn(DUP);
                    mv.visitVarInsn(Opcodes.ALOAD, 1);
                    mv.visitMethodInsn(Opcodes.INVOKESTATIC, TRACE_STATEMENT, name, "(ILjava/lang/String;)V", false);
                    addedStack = 2;
                    break;
                // executeBatch
                case 3:
                    mv.visitInsn(DUP);
                    mv.visitMethodInsn(Opcodes.INVOKESTATIC, TRACE_STATEMENT, name, "([I)V", false);
                    addedStack = 1;
                    break;

                default:
            }
        }
        mv.visitInsn(opcode);
    }

    @Override
    public void visitMaxs(int maxStack, int maxLocals) {
        mv.visitMaxs(maxStack + addedStack, maxLocals);
    }
}

class StAddBatchMV extends LocalVariablesSorter implements Opcodes {

    private static final String TRACE_STATEMENT = StringUtil.parseClassName(TraceStatement.class);

    public StAddBatchMV(int access, String desc, MethodVisitor mv) {
        super(ASM5, access, desc, mv);
    }

    @Override
    public void visitInsn(int opcode) {
        if ((opcode >= IRETURN && opcode <= RETURN)) {
            mv.visitVarInsn(Opcodes.ALOAD, 1);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, TRACE_STATEMENT, "addBatch", "(Ljava/lang/String;)V", false);
        }
        mv.visitInsn(opcode);
    }

    @Override
    public void visitMaxs(int maxStack, int maxLocals) {
        mv.visitMaxs(maxStack + 1, maxLocals);
    }
}