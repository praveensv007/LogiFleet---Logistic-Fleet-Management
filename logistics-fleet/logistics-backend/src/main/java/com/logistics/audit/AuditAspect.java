package com.logistics.audit;

import com.logistics.service.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

    private final AuditService auditService;

    @AfterReturning(pointcut = "@annotation(auditable)", returning = "result")
    public void afterReturning(JoinPoint joinPoint, Auditable auditable, Object result) {
        try {
            String entity = auditable.entity().isEmpty() ? auditable.module() : auditable.entity();
            Long entityId = extractId(result);
            if (entityId == null) {
                // e.g. update(Long id, ...) returning void: use the first Long argument
                for (Object arg : joinPoint.getArgs()) {
                    if (arg instanceof Long l) {
                        entityId = l;
                        break;
                    }
                }
            }
            auditService.log(auditable.action(), auditable.module(), entity, entityId, null, result);
        } catch (Exception e) {
            log.warn("Audit logging failed: {}", e.getMessage());
        }
    }

    private Long extractId(Object result) {
        if (result == null) {
            return null;
        }
        for (String name : new String[]{"getId", "id"}) {
            try {
                Method m = result.getClass().getMethod(name);
                Object v = m.invoke(result);
                if (v instanceof Long l) {
                    return l;
                }
            } catch (ReflectiveOperationException ignored) {
                // try next accessor style
            }
        }
        return null;
    }
}
