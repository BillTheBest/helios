package org.helios.jmx.opentypes;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenType;

/**
 * <p>Title: CompositeTypeClass</p>
 * <p>Description: Annotation to mark a class as having an CompositeType</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jmx.opentypes.CompositeTypeClass</code></p>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface CompositeTypeClass {
	/**
	 * The name of the type, defaulting to "" which means the class name
	 */
	public String name() default "";
	/**
	 * The description of the type, defaulting to "" which means a synthetic description
	 */
	public String description() default "";
	/**
	 * The OpenType of the type to create
	 */
	public Class<? extends OpenType<?>> openType() default CompositeType.class;
	
	
}
