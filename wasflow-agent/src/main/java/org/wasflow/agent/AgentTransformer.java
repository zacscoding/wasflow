package org.wasflow.agent;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.wasflow.agent.asm.CustomLoginASM;
import org.wasflow.agent.asm.HttpServiceASM;
import org.wasflow.agent.asm.IASM;
import org.wasflow.agent.asm.InitialContextASM;
import org.wasflow.agent.asm.JDBCConnectionASM;
import org.wasflow.agent.asm.JDBCDataSourceASM;
import org.wasflow.agent.asm.JDBCPreparedStatementASM;
import org.wasflow.agent.asm.JDBCResultSetASM;
import org.wasflow.agent.asm.JDBCResultSetMetaDataASM;
import org.wasflow.agent.asm.JDBCStatementASM;
import org.wasflow.agent.asm.ServletAsyncASM;
import org.wasflow.agent.asm.ServletOutputStreamASM;
import org.wasflow.agent.asm.TomcatASM;
import org.wasflow.agent.asm.WasflowClassWriter;
import org.wasflow.util.ASMUtil;
import org.wasflow.util.WriteClassFileUtil;

/**
 * @author zacconding
 * @Date 2018-09-08
 * @GitHub : https://github.com/zacscoding
 */
public class AgentTransformer implements ClassFileTransformer {

    protected static List<IASM> asms = new ArrayList<IASM>();
    public static ThreadLocal<ClassLoader> hookingCtx = new ThreadLocal<ClassLoader>();

    static {
        reload();
    }

    public static void reload() {
        List<IASM> temps = new ArrayList<IASM>();
        temps.add(new HttpServiceASM());
        temps.add(new InitialContextASM());
        temps.add(new JDBCResultSetASM());
        temps.add(new JDBCResultSetMetaDataASM());
        temps.add(new JDBCPreparedStatementASM());
        temps.add(new JDBCStatementASM());
        temps.add(new JDBCDataSourceASM());
        temps.add(new JDBCConnectionASM());
        temps.add(new TomcatASM());
        temps.add(new CustomLoginASM());
        temps.add(new ServletOutputStreamASM());

        temps.add(new ServletAsyncASM());

        //         temps.add(new JDBCDriverASM());
        /*  TEMP CODE :: 특정 클래스, 메소드를 찾기 위한 */
        //temps.add(new TempStackTraceASM());
        /* -- TEMP CODE     */
        asms = temps;
    }

    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        try {
            hookingCtx.set(loader);
            if (className == null) {
                return null;
            }

            // set class desc
            final ClassDesc classDesc = new ClassDesc();
            ClassReader cr = new ClassReader(classfileBuffer);
            cr.accept(new ClassVisitor(Opcodes.ASM5) {
                public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                    classDesc.set(version, access, name, signature, superName, interfaces);
                    super.visit(version, access, name, signature, superName, interfaces);
                }

                @Override
                public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                    classDesc.annotation += desc;
                    return super.visitAnnotation(desc, visible);
                }
            }, 0);

            // check interface
            if (ASMUtil.isInterface(classDesc.access)) {
                return null;
            }

            classDesc.classBeingRedefined = classBeingRedefined;
            ClassWriter cw = new WasflowClassWriter(ClassWriter.COMPUTE_FRAMES);
            ClassVisitor cv = cw;
            List<IASM> workAsms = asms;

            for (int i = workAsms.size() - 1; i >= 0; i--) {
                cv = workAsms.get(i).transform(cv, className, classDesc);
                if (cv != cw) {
                    cr = new ClassReader(classfileBuffer);
                    cr.accept(cv, ClassReader.EXPAND_FRAMES);
                    classfileBuffer = cw.toByteArray();
                    cv = cw = new WasflowClassWriter(ClassWriter.COMPUTE_FRAMES);

                    // temp develop :: 클래스 바이트를 파일로 쓴다
                    if ("true".equalsIgnoreCase(System.getProperty("wasflow.writeclass"))) {
                        WriteClassFileUtil.writeByteCode(classfileBuffer, className);
                    }
                }
            }

            return classfileBuffer;
        } catch (Throwable t) {
            // 1.6 이후 버전은 JSR/RET를 사용하지 않지만, 그 미만의 버전에서는 사용하여 에러가 발생한다.
            if (t.getMessage() != null && t.getMessage().startsWith("JSR/RET")) {
                LOGGER.error("Failed to transform in AgentTransformer JSR/RET error occur : " + className);
            } else {
                LOGGER.error("Failed to transform in AgentTransformer in " + className, t);
            }
        } finally {
            hookingCtx.set(null);
        }

        return null;
    }
}