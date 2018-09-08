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
 * DataSource 인터페이스 구현 클래스 => 어디에 적용할 지 아직 미정...
 *
 * @author zacconding
 * @Date 2018-09-08
 * @GitHub : https://github.com/zacscoding
 */
public class JDBCDataSourceASM implements IASM, Opcodes {

    private static final String INTERFACE_TARGET = "javax/sql/DataSource";
    public HashSet<String> targets = new HashSet<String>();

    private int hashCode;

    public JDBCDataSourceASM() {
        hashCode = INTERFACE_TARGET.hashCode();
        targets.add("org/springframework/jdbc/datasource/AbstractDriverBasedDataSource");
        targets.add("org/apache/tomcat/dbcp/dbcp/BasicDataSource");
        targets.add("org/apache/tomcat/jdbc/pool/DataSourceProxy");
        targets.add("org/apache/commons/dbcp2/BasicDataSource");
        targets.add("org/apache/commons/dbcp/PoolingDataSource");
    }

    @Override
    public ClassVisitor transform(ClassVisitor cv, String className, ClassDesc classDesc) {
        if (targets.contains(className)) {
            LOGGER.info("!@ find target : " + className);
            return new JDBCDataSourceCV(cv);
        } else {
            String[] interfaces = classDesc.interfaces;
            if (interfaces != null && interfaces.length > 0) {
                for (String interfaceName : interfaces) {
                    if (interfaceName.hashCode() == hashCode) {
                        if (INTERFACE_TARGET.equals(interfaceName)) {
                            LOGGER.info("!@ find target class name : " + className + ", interface : " + INTERFACE_TARGET);
                            return new JDBCDataSourceCV(cv);
                        }
                    }
                }
            }
        }

        return cv;
    }
}

class JDBCDataSourceCV extends ClassVisitor implements Opcodes {

    protected JDBCDataSourceCV(ClassVisitor cv) {
        super(ASM5, cv);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
        if (mv == null) {
            return mv;
        }

        if ("getConnection".equals(name) && (desc.startsWith("(Ljava/lang/String;Ljava/util/Properties;Ljava/lang/Class;") || desc.equals("()Ljava/sql/Connection;"))) {
            return new DsGetConnMV(access, desc, mv);
        }
        // else if("rollback".equals(name) && "()V".equals(desc)) {
        else if ("rollback".equals(name)) {
            return new DsRollbackMV(access, desc, mv);
        }

        return mv;
    }
}

class DsRollbackMV extends LocalVariablesSorter implements Opcodes {

    private static final String TRACEMAIN = StringUtil.parseClassName(TraceSQL.class);

    protected DsRollbackMV(int access, String desc, MethodVisitor mv) {
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

class DsGetConnMV extends LocalVariablesSorter implements Opcodes {

    private static final String TRACEMAIN = StringUtil.parseClassName(TraceSQL.class);
    private boolean hasParams;

    protected DsGetConnMV(int access, String desc, MethodVisitor mv) {
        super(ASM5, access, desc, mv);
        hasParams = desc.startsWith("()") == false;
    }

    @Override
    public void visitInsn(int opcode) {
        if ((opcode >= IRETURN && opcode <= RETURN)) {
            if (hasParams) {
                mv.visitInsn(DUP);
                mv.visitVarInsn(Opcodes.ALOAD, 0);
                mv.visitVarInsn(Opcodes.ALOAD, 1);
            } else {
                mv.visitInsn(DUP);
                mv.visitInsn(Opcodes.ACONST_NULL);
                mv.visitInsn(Opcodes.ACONST_NULL);
            }
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, TRACEMAIN, "startSqlTrace", "(Ljava/sql/Connection;Ljava/lang/String;Ljava/util/Properties;)V", false);
        }
        mv.visitInsn(opcode);
    }

    //    @Override
    //    public void visitCode() {
    //        if (hasParams) {
    //            mv.visitVarInsn(Opcodes.ALOAD, 1);
    //            mv.visitVarInsn(Opcodes.ALOAD, 2);
    //        } else {
    //            mv.visitInsn(Opcodes.ACONST_NULL);
    //            mv.visitInsn(Opcodes.ACONST_NULL);
    //        }
    //        mv.visitMethodInsn(Opcodes.INVOKESTATIC, TRACEMAIN, "getConnection","(Ljava/lang/String;Ljava/util/Properties;)V", false);
    //        mv.visitCode();
    //    }


    @Override
    public void visitMaxs(int maxStack, int maxLocals) {
        int addedStack = (hasParams) ? 2 : 0;
        mv.visitMaxs(maxStack + addedStack, maxLocals);
    }
}