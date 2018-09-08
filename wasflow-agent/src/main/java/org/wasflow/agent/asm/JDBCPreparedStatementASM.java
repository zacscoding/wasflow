package org.wasflow.agent.asm;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.LocalVariablesSorter;
import org.wasflow.agent.ClassDesc;
import org.wasflow.agent.LOGGER;
import org.wasflow.agent.trace.TracePreparedStatement;
import org.wasflow.util.ASMUtil;
import org.wasflow.util.StringUtil;

/**
 * PreparedStatement 구현 클래스 변경
 *
 * @author zacconding
 * @Date 2018-09-08
 * @GitHub : https://github.com/zacscoding
 */
public class JDBCPreparedStatementASM implements IASM, Opcodes {

    public final HashSet<String> target = new HashSet<String>();
    public final HashSet<String> setterTarget = new HashSet<String>();

    public JDBCPreparedStatementASM() {
        // class targets
        target.add("oracle/jdbc/driver/OraclePreparedStatement");
        target.add("com/microsoft/sqlserver/jdbc/SQLServerPreparedStatement");
        target.add("org/postgresql/jdbc2/AbstractJdbc2Statement");
        target.add("org/postgresql/jdbc3/AbstractJdbc3Statement");
        target.add("org/postgresql/jdbc/PgPreparedStatement");

        // setXXX가 PreparedStatement를 이용하지 않는 경우
        setterTarget.add("org/postgresql/core/v3/SimpleParameterList");
    }

    @Override
    public ClassVisitor transform(ClassVisitor cv, String className, ClassDesc classDesc) {
        if (target.contains(className)) {
            LOGGER.info("!@ find target class name : " + className);
            return new PreparedStatementCV(cv, className);
        } else if (setterTarget.contains(className)) {
            LOGGER.info("!@ find target class name : " + className);
            return new PreparedSetterCV(cv, className);
        }

        return cv;
    }
}

/* ======================================== ClassVisitor ======================================== */
class PreparedStatementCV extends ClassVisitor implements Opcodes {

    private String className;

    public PreparedStatementCV(ClassVisitor cv, String className) {
        super(ASM5, cv);
        this.className = className;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
        if (mv == null) {
            return mv;
        }

        // SqlServer의 경우, setValue로 통일
        if ("setValue".equals(name)) {
            if ("(ILcom/microsoft/sqlserver/jdbc/JDBCType;Ljava/lang/Object;Lcom/microsoft/sqlserver/jdbc/JavaType;)V".equals(desc)) {
                return new PsSetCustomMV(access, desc, mv, 1, 3);
            } else if ("(Lcom/microsoft/sqlserver/jdbc/JDBCType;Ljava/lang/Object;Lcom/microsoft/sqlserver/jdbc/JavaType;Lcom/microsoft/sqlserver/jdbc/StreamSetterArgs;Ljava/util/Calendar;Ljava/lang/Integer;Lcom/microsoft/sqlserver/jdbc/SQLServerConnection;)V"
                .equals(desc)) {
                return new PsSetCustomMV(access, desc, mv, 6, 2);
            }
        } else if ("setStream".equals(name)) {
            if ("(ILcom/microsoft/sqlserver/jdbc/StreamType;Ljava/lang/Object;Lcom/microsoft/sqlserver/jdbc/JavaType;J)V".equals(desc)) {
                return new PsSetCustomMV(access, desc, mv, 1, 3);
            }
        }

        // Pstmt setXXX
        String setDesc = PsSetMV.getSetSignature(name);
        if (setDesc != null && desc.equals(setDesc)) {
            return new PsSetMV(access, desc, mv, name);
        }
        // Pstmt executeXXX
        else if (PsExecuteMV.contains(name)) {
            if (desc.startsWith("()")) {
                return new PsExecuteMV(access, desc, mv, name);
            } else if (desc.startsWith("(Ljava/lang/String;)")) {
                return new StExecuteMV(access, desc, mv, name);
            }
        }
        // 생성자
        else if ("<init>".equals(name)) {
            Type[] paramTypes = Type.getArgumentTypes(desc);
            if (paramTypes != null && paramTypes.length > 1 && "Ljava/lang/String;".equals(paramTypes[1].getDescriptor())) {
                return new PsInitMV(access, desc, mv);
            }
        } else if ("clearParameters".equals(name)) {
            return new PsClearParamasMV(access, desc, mv);
        } else if ("close".equals(name) && "()V".equals(desc)) {
            return new PsCloseMV(access, desc, mv);
        } else if ("addBatch".equals(name) && "()V".equals(desc)) {
            return new PsAddBatchMV(access, desc, mv);
        }

        return mv;
    }
}

// Prepared SetXXX 값을 구하기 위한 ClassVisitor
class PreparedSetterCV extends ClassVisitor implements Opcodes {

    private String className;

    public PreparedSetterCV(ClassVisitor cv, String className) {
        super(ASM5, cv);
        this.className = className;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
        if (mv == null) {
            return mv;
        }

        if ("bind".equals(name) && "(ILjava/lang/Object;I)V".equals(desc)) {
            return new PsSetCustomMV(access, desc, mv, 1, 2);
        }

        return mv;
    }
}


// Ps 초기화 관련
class PsInitMV extends LocalVariablesSorter implements Opcodes {

    private final static String TRACE_PREPARE = StringUtil.parseClassName(TracePreparedStatement.class);
    private String desc;

    public PsInitMV(int access, String desc, MethodVisitor mv) {
        super(ASM5, access, desc, mv);
        this.desc = desc;
    }

    @Override
    public void visitCode() {
        mv.visitVarInsn(ALOAD, 2);
        mv.visitMethodInsn(INVOKESTATIC, TRACE_PREPARE, "setQuery", "(Ljava/lang/String;)V", false);
        mv.visitCode();
    }

    @Override
    public void visitMaxs(int maxStack, int maxLocals) {
        mv.visitMaxs(maxStack + 1, maxLocals);
    }
}

class PsClearParamasMV extends LocalVariablesSorter implements Opcodes {

    private static final String TRACE_PREPARE = StringUtil.parseClassName(TracePreparedStatement.class);

    public PsClearParamasMV(int access, String desc, MethodVisitor mv) {
        super(ASM5, access, desc, mv);
    }

    @Override
    public void visitCode() {
        mv.visitMethodInsn(INVOKESTATIC, TRACE_PREPARE, "clearParameters", "()V", false);
        mv.visitCode();
    }
}

class PsCloseMV extends LocalVariablesSorter implements Opcodes {

    private static final String TRACE_PREPARE = StringUtil.parseClassName(TracePreparedStatement.class);

    public PsCloseMV(int access, String desc, MethodVisitor mv) {
        super(ASM5, access, desc, mv);
    }

    @Override
    public void visitInsn(int opcode) {
        if ((opcode >= IRETURN && opcode <= RETURN)) {
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, TRACE_PREPARE, "closeStmt", "(Ljava/lang/Object;)V", false);
        }
        mv.visitInsn(opcode);
    }
}

// PreparedStatement set value 추출
class PsSetMV extends LocalVariablesSorter implements Opcodes {

    private static final String TRACE_PREPARED = StringUtil.parseClassName(TracePreparedStatement.class);
    private String name, desc;
    private Type secondParam;
    private static Map<String, String> target = new HashMap<String, String>();

    static {
        target.put("setNull", "(II)V");
        target.put("setByte", "(IB)V");
        target.put("setBoolean", "(IZ)V");
        target.put("setShort", "(IS)V");
        target.put("setInt", "(II)V");
        target.put("setFloat", "(IF)V");
        target.put("setLong", "(IJ)V");
        target.put("setDouble", "(ID)V");
        target.put("setBigDecimal", "(ILjava/math/BigDecimal;)V");
        target.put("setString", "(ILjava/lang/String;)V");
        target.put("setDate", "(ILjava/util/Date;)V");
        target.put("setTime", "(ILjava/sql/Time;)V");
        target.put("setTimestamp", "(ILjava/sql/Timestamp;)V");
        target.put("setObject", "(ILjava/lang/Object;)V");
        target.put("setBlob", "(ILjava/sql/Blob;)V");
        // target.put("setClob", "(ILjava/sql/Clob;)V");
        // target.put("setURL", "(ILjava/net/URL;)V"); //
    }

    public static String getSetSignature(String name) {
        return target.get(name);
    }

    public PsSetMV(int access, String desc, MethodVisitor mv, String name) {
        super(ASM5, access, desc, mv);
        this.name = name;
        this.desc = desc;
        secondParam = Type.getArgumentTypes(desc)[1];
    }

    @Override
    public void visitCode() {
        mv.visitVarInsn(ILOAD, 1);
        ASMUtil.loadOrStore(mv, secondParam, 2, true);
        mv.visitMethodInsn(INVOKESTATIC, TRACE_PREPARED, name, desc, false);
        mv.visitCode();
    }
}

// SqlServer의 경우 PreparedStatement의 setValue, setStream으로 추출 & postgresql의 경우 bind(ILjava/lang/Object;I)V로 호출
class PsSetCustomMV extends LocalVariablesSorter implements Opcodes {

    private static final String TRACE_PREPARED = StringUtil.parseClassName(TracePreparedStatement.class);

    private int paramIdx;
    private int objIdx;

    public PsSetCustomMV(int access, String desc, MethodVisitor mv, int paramIdx, int objIdx) {
        super(ASM5, access, desc, mv);
        this.paramIdx = paramIdx;
        this.objIdx = objIdx;
    }

    @Override
    public void visitCode() {
        mv.visitVarInsn(ILOAD, paramIdx);
        mv.visitVarInsn(ALOAD, objIdx);

        mv.visitMethodInsn(INVOKESTATIC, TRACE_PREPARED, "setObjectByCustom", "(ILjava/lang/Object;)V", false);
        mv.visitCode();
    }
}

class PsExecuteMV extends LocalVariablesSorter implements Opcodes {

    private static final String TRACE_PREPARED = StringUtil.parseClassName(TracePreparedStatement.class);
    private static Set<String> target = new HashSet<String>();
    private Type returnType;
    private String name;
    private int addedStack;

    static {
        target.add("execute");
        target.add("executeQuery");
        target.add("executeUpdate");
        target.add("executeBatch");
    }

    public static boolean contains(String name) {
        return target != null && target.contains(name);
    }

    public PsExecuteMV(int access, String desc, MethodVisitor mv, String name) {
        super(ASM5, access, desc, mv);
        this.returnType = Type.getReturnType(desc);
        this.name = name;
    }

    @Override
    public void visitInsn(int opcode) {
        if ((opcode >= IRETURN && opcode <= RETURN)) {
            mv.visitInsn(DUP);
            switch (returnType.getSort()) {
                // boolean execute()
                case Type.BOOLEAN:
                    mv.visitVarInsn(ALOAD, 0);
                    mv.visitMethodInsn(Opcodes.INVOKESTATIC, TRACE_PREPARED, name, "(ZLjava/sql/PreparedStatement;)V", false);
                    addedStack = 2;
                    break;
                // int executeUpdate()
                case Type.INT:
                    mv.visitMethodInsn(INVOKESTATIC, TRACE_PREPARED, name, "(I)V", false);
                    addedStack = 1;
                    break;
                // int[] executeBatch()
                case Type.ARRAY:
                    mv.visitMethodInsn(INVOKESTATIC, TRACE_PREPARED, name, "([I)V", false);
                    addedStack = 1;
                    break;
                // ResultSet executeQuery()
                default:
                    mv.visitMethodInsn(INVOKESTATIC, TRACE_PREPARED, name, "(Ljava/sql/ResultSet;)V", false);
            }
        }
        mv.visitInsn(opcode);
    }

    @Override
    public void visitMaxs(int maxStack, int maxLocals) {
        mv.visitMaxs(maxStack + addedStack, maxLocals);
    }
}

class PsAddBatchMV extends LocalVariablesSorter implements Opcodes {

    private static final String TRACE_PREPARED = StringUtil.parseClassName(TracePreparedStatement.class);

    public PsAddBatchMV(int access, String desc, MethodVisitor mv) {
        super(ASM5, access, desc, mv);
    }

    @Override
    public void visitCode() {
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, TRACE_PREPARED, "addBatch", "()V", false);
    }
}