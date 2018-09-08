package org.wasflow.util;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 * @author zacconding
 * @Date 2018-09-08
 * @GitHub : https://github.com/zacscoding
 */
public class ASMUtil {

    /**
     * Type에 따른 XLOAD or XSTORE 처리 메소드
     *
     * @param mv     MethodVisitor
     * @param type   체크 할 Type
     * @param idx    LOAD or STORE 할 idx
     * @param isLoad LOAD 여부
     */
    public static void loadOrStore(MethodVisitor mv, Type type, int idx, boolean isLoad) {
        if (mv == null) {
            return;
        }

        switch (type.getSort()) {
            case Type.BOOLEAN:
            case Type.BYTE:
            case Type.SHORT:
            case Type.CHAR:
            case Type.INT:
                if (isLoad) {
                    mv.visitVarInsn(Opcodes.ILOAD, idx);
                } else {
                    mv.visitVarInsn(Opcodes.ISTORE, idx);
                }
                break;
            case Type.LONG:
                if (isLoad) {
                    mv.visitVarInsn(Opcodes.LLOAD, idx);
                } else {
                    mv.visitVarInsn(Opcodes.LSTORE, idx);
                }
                break;
            case Type.FLOAT:
                if (isLoad) {
                    mv.visitVarInsn(Opcodes.FLOAD, idx);
                } else {
                    mv.visitVarInsn(Opcodes.FSTORE, idx);
                }
                break;
            case Type.DOUBLE:
                if (isLoad) {
                    mv.visitVarInsn(Opcodes.DLOAD, idx);
                } else {
                    mv.visitVarInsn(Opcodes.DSTORE, idx);
                }
                break;
            default:
                if (isLoad) {
                    mv.visitVarInsn(Opcodes.ALOAD, idx);
                } else {
                    mv.visitVarInsn(Opcodes.ASTORE, idx);
                }
        }
    }

    /**
     * Desc를 String.valueOf() 의 paramDesc로 변경해주는 메소드
     * primitive type
     * 1) B , S , I => (I)Ljava/lang/String;
     * 2) 그 외 => (desc)Ljava/lang/String;
     *
     * Object or Array type
     * (Ljava/lang/Object;)Ljava/lang/String;
     */
    /*  TEMP CODE :: 배열도 체크해야 하는지? */
    public static String parseStringValueOfDesc(String desc) {
        // System.out.println("!@ parseStringValueOfDesc is called " + desc);
        String returnDesc = Type.getReturnType(desc).getDescriptor();
        // primative 타입
        if (returnDesc == null || returnDesc.length() == 0) {
            return "(Ljava/lang/Object;)Ljava/lang/String;";
        }
        if (returnDesc.length() == 1) {
            char descChar = returnDesc.charAt(0);
            switch (descChar) {
                case 'B':
                case 'S':
                case 'I':
                    return "(I)Ljava/lang/String;";
                default:
                    return "(" + returnDesc + ")Ljava/lang/String;";
            }

        }
        // String
        else if (returnDesc.endsWith("String;")) {
            return null;
        }
        // 그 외 Object
        else {
            return "(Ljava/lang/Object;)Ljava/lang/String;";
        }
    }

    public static String parseStringValueOfDesc(Type type) {
        if (type == null) {
            return null;
        }
        switch (type.getSort()) {
            case Type.BYTE:
            case Type.SHORT:
            case Type.INT:
                return "(I)Ljava/lang/String;";
            case Type.OBJECT:
            case Type.ARRAY:
                return "(Ljava/lang/Object;)Ljava/lang/String;";
            default:
                return "(" + type.getDescriptor() + ")Ljava/lang/String;";
        }
    }

    /**
     * 인터페이스 체크 메소드
     *
     * @param access Opcodes.ACC 관련 int 값
     *
     * @return true : 인터페이스 , false : 그외
     */
    public static boolean isInterface(int access) {
        return (access & Opcodes.ACC_INTERFACE) != 0;
    }
}

