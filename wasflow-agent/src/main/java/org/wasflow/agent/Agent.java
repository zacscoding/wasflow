package org.wasflow.agent;

import java.lang.instrument.Instrumentation;

/**
 * Agent premain
 *
 * @author zacconding
 * @Date 2018-09-08
 * @GitHub : https://github.com/zacscoding
 */
public class Agent {

    private static Instrumentation instrumentation;

    public static Instrumentation getInstrumentation() {
        return instrumentation;
    }

    public static void premain(String agentArgs, Instrumentation inst) {
        if (Agent.instrumentation != null) {
            return;
        }

        try {
            LOGGER.info("start to premain...");
            Agent.instrumentation = inst;
            Agent.instrumentation.addTransformer(new AgentTransformer());
        } catch (Throwable t) {
            LOGGER.error("Failed to premain in Agent", t);
        }
    }
}