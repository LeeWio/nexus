package space.nebula.nexus.common.aspect;

import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;

import java.util.Map;

/**
 * Decorator for transferring MDC context (like traceId) to async threads.
 */
public class MdcTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable runnable) {
        // Copy the current MDC context
        Map<String, String> contextMap = MDC.getCopyOfContextMap();
        
        return () -> {
            try {
                // Apply the context to the new thread
                if (contextMap != null) {
                    MDC.setContextMap(contextMap);
                }
                runnable.run();
            } finally {
                // Clear context after task completion
                MDC.clear();
            }
        };
    }
}
