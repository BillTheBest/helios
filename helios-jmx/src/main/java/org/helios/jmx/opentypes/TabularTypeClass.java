package org.helios.jmx.opentypes;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * <p>Title: TabularTypeClass</p>
 * <p>Description: Annotation to mark a class as an aggregated container for composite type instances in the form of a TabularType.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jmx.opentypes.TabularTypeClass</code></p>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface TabularTypeClass {
	/**
	 * The name of the type, defaulting to "" which means the class name
	 */
	public String name() default "";
	/**
	 * The description of the type, defaulting to "" which means a synthetic description
	 */
	public String description() default "";
	/**
	 * The class for which a CompositeType will be supplied to specify the row type of this tabular type
	 */
	public Class<?> compositeType();

}
