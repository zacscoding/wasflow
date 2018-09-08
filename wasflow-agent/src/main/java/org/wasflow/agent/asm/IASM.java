package org.wasflow.agent.asm;

import org.objectweb.asm.ClassVisitor;
import org.wasflow.agent.ClassDesc;

/**
 * @author zacconding
 * @Date 2018-09-08
 * @GitHub : https://github.com/zacscoding
 */
public interface IASM {

    ClassVisitor transform(ClassVisitor cv, String className, ClassDesc classDesc);
}