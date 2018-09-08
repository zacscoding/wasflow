package org.wasflow.agent.asm;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
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
 * ResultSet 구현 클래스 관련 ASM
 *
 * @author zacconding
 * @Date 2018-09-08
 * @GitHub : https://github.com/zacscoding
 */
public class JDBCResultSetASM implements IASM, Opcodes {

    public final HashSet<String> targets = new HashSet<String>();

    public JDBCResultSetASM() {
        // oracle
        targets.add("oracle/jdbc/driver/InsensitiveScrollableResultSet");
        targets.add("oracle/jdbc/driver/SensitiveScrollableResultSet");
        targets.add("oracle/jdbc/driver/GeneratedScrollableResultSet"); // ojdbc6
        targets.add("oracle/jdbc/driver/OracleResultSetImpl"); // ojdbc5

        // sqlserver
        targets.add("com/microsoft/sqlserver/jdbc/SQLServerResultSet");

        // postgresql
        targets.add("org/postgresql/jdbc2/AbstractJdbc2ResultSet");
        //pg driver 42+
        targets.add("org/postgresql/jdbc/PgResultSet");
    }

    @Override
    public ClassVisitor transform(ClassVisitor cv, String className, ClassDesc classDesc) {
        if (targets.contains(className)) {
            LOGGER.info("!@ find ResultSet target class name : " + className);
            return new ResultSetCV(cv);
        }

        /*  TEMP CODE :: */
        /*for (String ifName : classDesc.interfaces) {
            if ("java/sql/ResultSet".equals(ifName)) {
                LOGGER.info("!@ find ResultSet impl target class name : " + className);
                // return new ResultSetCV(cv);
            }
        }*/
        /*  TEMP CODE :: */

        return cv;
    }
}

/* ======================================== ClassVisitor ======================================== */
class ResultSetCV extends ClassVisitor implements Opcodes {

    public ResultSetCV(ClassVisitor cv) {
        super(ASM5, cv);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
        if (mv == null) {
            return mv;
        }

        // SqlServer의 ResultSet은 getValue를 통해 이루어짐
        if ("getValue".equals(name) && "(ILcom/microsoft/sqlserver/jdbc/JDBCType;Lcom/microsoft/sqlserver/jdbc/InputStreamGetterArgs;Ljava/util/Calendar;)Ljava/lang/Object;".equals(desc)) {
            // mv = new SqlServerResultSetMV(access, desc, mv, name);
            mv = new ResultSetValueMV(access, desc, mv, name, "(Ljava/lang/Object;I)V");
        }
        
        // getXXX 추출 mv
        String[] getDescs = ResultSetValueMV.getDescs(name);
        if (getDescs != null) {
            for (String targetDesc : getDescs) {
                if (targetDesc.equals(desc)) {
                    return new ResultSetValueMV(access, desc, mv, name);
                }
            }
        }
        if ("<init>".equals(name)) {
            return new ResultSetMV(access, desc, mv, 0);
        } else if ("next".equals(name) && "()Z".equals(desc)) {
            return new ResultSetMV(access, desc, mv, 1);
        } else if ("close".equals(name) && "()V".equals(desc)) {
            return new ResultSetMV(access, desc, mv, 2);
        }

        return mv;
    }
}

/* ======================================== MethodVisitor ======================================== */
class ResultSetMV extends LocalVariablesSorter implements Opcodes {

    private static final String TRACESQL = StringUtil.parseClassName(TraceResultSet.class);
    private static final String[] METHODS = new String[] {"init", "next", "close"};
    private static final String[] SIGNATURES = new String[] {"(Ljava/lang/Object;)V", "(Z)V", "(Ljava/lang/Object;)V"};
    private int methodType;

    // methodType == 0 : <init> , 1 : next , 2 : close
    public ResultSetMV(int access, String desc, MethodVisitor mv, int methodType) {
        super(ASM5, access, desc, mv);
        this.methodType = methodType;
    }

    @Override
    public void visitInsn(int opcode) {
        if ((opcode >= IRETURN && opcode <= RETURN)) {
            if (methodType >= 0 && methodType <= 2) {
                // next()
                if (methodType == 1) {
                    mv.visitInsn(Opcodes.DUP);
                } else {
                    // load this
                    mv.visitVarInsn(ALOAD, 0);
                }
                // invoke TraceSQL.method(signatue)
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, TRACESQL, METHODS[methodType], SIGNATURES[methodType], false);
            }
        }
        mv.visitInsn(opcode);
    }

    @Override
    public void visitMaxs(int maxStack, int maxLocals) {
        mv.visitMaxs(maxStack + 1, maxLocals);
    }
}

// public static void rsGetColumnValue(String value, int idx) {
class ResultSetValueMV extends LocalVariablesSorter implements Opcodes {

    private static Map<String, String[]> target = new HashMap<String, String[]>();
    private static final String TRACESQL = StringUtil.parseClassName(TraceResultSet.class);
    private String invokeMethodName;
    private String invokeMethodDesc;
    private Type returnType;
    private int paramSize;
    private int addedStack;

    static {
        // 표준 JDBC
        // TraceResultSet의 setXXX 메소드 관련
        target.put("getObject", new String[] {"(I)Ljava/lang/Object;"});
        target.put("getString", new String[] {"(I)Ljava/lang/String;"});
        target.put("getNString", new String[] {"(I)Ljava/lang/String;"});
        target.put("getBoolean", new String[] {"(I)Z"});
        target.put("getByte", new String[] {"(I)B"});
        target.put("getBytes", new String[] {"(I)[B"});
        target.put("getShort", new String[] {"(I)S"});
        target.put("getInt", new String[] {"(I)I"});
        target.put("getLong", new String[] {"(I)J"});
        target.put("getFloat", new String[] {"(I)F"});
        target.put("getDouble", new String[] {"(I)D"});
        target.put("getBigDecimal", new String[] {"(I)Ljava/math/BigDecimal;", "(II)Ljava/math/BigDecimal;"});
        target.put("getDate", new String[] {"(I)Ljava/util/Date;"});
        target.put("getTimestamp", new String[] {"(I)Ljava/sql/Timestamp;"});
        target.put("getBlob", new String[] {"(I)Ljava/sql/Blob;"});
        target.put("getClob", new String[] {"(I)Ljava/sql/Clob;"});

        // SqlServer
        //        target.put("getValue", new String[]{
        //            "(ILcom/microsoft/sqlserver/jdbc/JDBCType;Lcom/microsoft/sqlserver/jdbc/InputStreamGetterArgs;Ljava/util/Calendar;)Ljava/lang/Object;"});
    }

    public static String[] getDescs(String name) {
        return target.get(name);
    }

    public ResultSetValueMV(int access, String desc, MethodVisitor mv, String methodName) {
        super(ASM5, access, desc, mv);
        // ResultSet.getString (I)Ljava/lang/String;
        // => TraceResultSet.setString (ILjava/lang/String;)V 로 파싱
        returnType = Type.getReturnType(desc);
        Type[] types = Type.getArgumentTypes(desc);
        paramSize = types.length;

        this.invokeMethodName = "s" + methodName.substring(1);
        invokeMethodDesc = "(" + returnType.getDescriptor();
        for (Type type : types) {
            invokeMethodDesc += type.getDescriptor();
        }
        invokeMethodDesc += ")V";
    }

    public ResultSetValueMV(int access, String desc, MethodVisitor mv, String methodName, String invokeMethodDesc) {
        super(ASM5, access, desc, mv);
        returnType = Type.getReturnType(desc);
        this.invokeMethodName = "s" + methodName.substring(1);
        this.invokeMethodDesc = invokeMethodDesc;
        paramSize = Type.getArgumentTypes(invokeMethodDesc).length - 1;
    }


    @Override
    public void visitInsn(int opcode) {
        if ((opcode >= IRETURN && opcode <= RETURN)) {
            switch (returnType.getSort()) {
                case Type.LONG:
                case Type.DOUBLE:
                    // long, double 은 2개의 stack을 차지 => DUP2
                    mv.visitInsn(Opcodes.DUP2);
                    addedStack += 2;
                    break;
                default:
                    addedStack += 1;
                    mv.visitInsn(Opcodes.DUP);
            }

            for (int i = 0; i < paramSize; i++) {
                addedStack++;
                mv.visitVarInsn(Opcodes.ILOAD, i + 1);
            }

            mv.visitMethodInsn(INVOKESTATIC, TRACESQL, invokeMethodName, invokeMethodDesc, false);
        }
        mv.visitInsn(opcode);
    }

    @Override
    public void visitMaxs(int maxStack, int maxLocals) {
        mv.visitMaxs(maxStack + addedStack, maxLocals);
    }
}


class SqlServerResultSetMV extends LocalVariablesSorter implements Opcodes {

    private static final String TRACESQL = StringUtil.parseClassName(TraceResultSet.class);
    private String methodName;

    public SqlServerResultSetMV(int access, String desc, MethodVisitor mv, String methodName) {
        super(ASM5, access, desc, mv);
        this.methodName = methodName;
    }

    @Override
    public void visitInsn(int opcode) {
        if ((opcode >= IRETURN && opcode <= RETURN)) {
            mv.visitInsn(Opcodes.DUP);
            mv.visitInsn(0);

        }

        mv.visitInsn(opcode);
    }


}