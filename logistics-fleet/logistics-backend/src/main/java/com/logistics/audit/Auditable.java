package com.logistics.audit;

import com.logistics.entity.enums.AuditAction;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Put on a service method; after it returns, AuditAspect writes an audit_logs row with the
 * returned DTO as new_value and its id as entity_id.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Auditable {
    AuditAction action();

    String module();

    String entity() default "";
}
