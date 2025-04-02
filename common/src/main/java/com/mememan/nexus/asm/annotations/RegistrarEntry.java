package com.mememan.nexus.asm.annotations;

import com.mememan.nexus.platform.services.Registrar;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation used by loader-specific implementations of {@link Registrar} in order to discover and load registrar
 * classes annotated with this annotation.
 *
 * @see Registrar
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface RegistrarEntry {

    /**
     *
     *
     * @return
     */
    int priority() default 0;

    /**
     *
     *
     * @return
     */
    Class[] dependencies() default {};
}
