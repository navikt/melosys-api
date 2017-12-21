package no.nav.melosys.regler.motor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Tægge-interface for regelpakker.
 * 
 * Instansene trenger kun implementere en eller flere static metoder som annoteres med @Regel
 * 
 */
public interface Regelpakke {
    
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface Regel {}

}
