//package org..wasflow.agent.asm;
//
//import org..wasflow.agent.ClassDesc;
//import org..wasflow.agent.LOGGER;
//import java.util.HashSet;
//import java.util.Set;
//import org.objectweb.asm.ClassVisitor;
//import org.objectweb.asm.MethodVisitor;
//import org.objectweb.asm.Opcodes;
//import org.objectweb.asm.commons.LocalVariablesSorter;
//
///**
// * @author zaccoding
// */
//public class TempStackTraceASM implements IASM, Opcodes {
//
//    // private Set<String> target = new HashSet<String>();
//
//    public TempStackTraceASM() {
//    }
//
//    @Override
//    public ClassVisitor transform(ClassVisitor cv, String className, ClassDesc classDesc) {
//        String lowerCase = className.toLowerCase();
//        if (lowerCase.contains("servlet") || lowerCase.contains("apache")) {
//            return new TempStackTraceCV(cv, className);
//        }
//
//        return cv;
//    }
//}
//
//class TempStackTraceCV extends ClassVisitor implements Opcodes {
//
//    private String className;
//
//    public TempStackTraceCV(ClassVisitor cv, String className) {
//        super(ASM5, cv);
//        this.className = className;
//    }
//
//    @Override
//    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
//        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
//        if (mv == null) {
//            return mv;
//        }
//
//        if (name.startsWith("write") || name.startsWith("print")) {
//            LOGGER.temp("!@find target method" + className);
//            return new TempStackTraceMV(access, desc, mv, className, name);
//        }
//
//        return mv;
//    }
//
//
//}
//
//class TempStackTraceMV extends LocalVariablesSorter implements Opcodes {
//
//    private String methodName;
//    private String desc;
//    private String fullName;
//
//    protected TempStackTraceMV(int access, String desc, MethodVisitor mv, String className, String methodName) {
//        super(ASM5, access, desc, mv);
//        this.fullName = className + "::" + methodName + desc;
//        this.methodName = methodName;
//        this.desc = desc;
//    }
//
//    @Override
//    public void visitCode() {
//        mv.visitLdcInsn(fullName);
//        mv.visitMethodInsn(Opcodes.INVOKESTATIC, LOGGER.class.getName().replace('.', '/'), "temp", "(Ljava/lang/String;)V", false);
//        mv.visitCode();
//    }
//}
//
