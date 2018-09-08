package org.wasflow.agent.asm;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.LocalVariablesSorter;
import org.wasflow.agent.ClassDesc;
import org.wasflow.agent.Configurer;
import org.wasflow.agent.LOGGER;
import org.wasflow.agent.trace.TraceMain;
import org.wasflow.util.StringUtil;

/**
 * 로그인 값을 제대로 추출하지 못할 때, 로그인 아이디를 판별하는 메소드에 적용해서 로그인 아이디를 추출하는 ASM
 *
 * @author zacconding
 * @Date 2018-09-08
 * @GitHub : https://github.com/zacscoding
 */
public class CustomLoginASM implements IASM, Opcodes {

    private int classNameHashCode;
    private String className;
    private String methodName;
    private String methodDesc;
    private boolean isApply = false;

    public CustomLoginASM() {
        isApply = initFields();
    }

    @Override
    public ClassVisitor transform(ClassVisitor cv, String className, ClassDesc classDesc) {
        if (isApply) {
            if (this.classNameHashCode == className.hashCode() && this.className.equals(className)) {
                LOGGER.info("!@ find target " + className);
                return new CustomLoginCV(cv, this.methodName, this.methodDesc);
            }
        }

        return cv;
    }

    /**
     * Configurer에 custom login 값이 존재하는지 체크한다.
     *
     * @return config에 custom login이 존재하는 지 여부
     */
    private boolean initFields() {
        Configurer conf = Configurer.getInstance();

        className = conf.login_invoker_class_name;
        if (StringUtil.isEmpty(className)) {
            return false;
        }

        this.methodName = conf.login_invoker_method_name;
        if (StringUtil.isEmpty(methodName)) {
            return false;
        }

        this.methodDesc = conf.login_invoker_method_desc;
        if (StringUtil.isEmpty(methodDesc)) {
            return false;
        }

        this.classNameHashCode = className.hashCode();

        return true;
    }
}

class CustomLoginCV extends ClassVisitor implements Opcodes {

    private String methodName;
    private String methodDesc;

    public CustomLoginCV(ClassVisitor cv, String methodName, String methodDesc) {
        super(ASM5, cv);
        this.methodName = methodName;
        this.methodDesc = methodDesc;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);

        if (mv == null) {
            return mv;
        }

        if (methodName.equals(name) && methodDesc.equals(desc)) {
            LOGGER.info("!@ find target method " + name + desc);
            return new CustomLoginMV(access, desc, mv);
        }

        return mv;
    }
}

class CustomLoginMV extends LocalVariablesSorter implements Opcodes {

    private String desc;
    private final String TRACEMAIN = StringUtil.parseClassName(TraceMain.class);

    public CustomLoginMV(int access, String desc, MethodVisitor mv) {
        super(ASM5, access, desc, mv);
    }

    @Override
    public void visitInsn(int opcode) {
        if ((opcode >= IRETURN && opcode <= RETURN)) {
            // top stack value copy(리턴 값)
            mv.visitInsn(DUP);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, TRACEMAIN, "setLoginId", "(Ljava/lang/Object;)V", false);
        }

        mv.visitInsn(opcode);
    }
}