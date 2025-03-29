package com.mememan.nexus.asm.annotations;

import com.mememan.nexus.Nexus;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation interface used to load classes that need early class-loading ahead of all other mod-loading stages.
 *
 * @see Nexus
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface LoadEarly {
}
