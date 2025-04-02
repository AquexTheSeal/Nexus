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
     * Determines the ordinal priority this annotation's owning {@code class} should be loaded by relative to other
     * registrar entries.
     * <br></br>
     * By default, registrar entries are loaded lexicographically provided they share the same priority value with any
     * other registrar class(es) AND that their {@link #dependencies()} are either empty or all loaded/initialized.
     *
     * @return The priority value this annotation's owning {@code class} should be loaded by. Defaults to 0. Higher
     * values are prioritized.
     */
    int priority() default 0;

    /**
     *
     *
     * @return
     */
    Class[] dependencies() default {};
}
