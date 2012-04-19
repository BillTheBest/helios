
package org.helios.jmx.opentypes;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>Title: OpenTypeAttribute</p>
 * <p>Description: Annotation to mark an attribute as having an OpenType</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jmx.opentypes.OpenTypeAttribute</code></p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OpenTypeAttribute {
	
	
	/**
	 * The name of the item, defaulting to "" which means the property name
	 */
	public String name() default "";
	/**
	 * The description of the item, defaulting to "" which means a synthetic description
	 */
	public String description() default "";
	/**
	 * The open type mapping class of the item, defaulting to {@link DefaultedType} 
	 * which means the builder will use the return type
	 */
	public Class<?> openType() default DefaultedType.class;
	/**
	 * Indicates if this attribute is a CompositeType index
	 */
	public boolean index() default false;
	
	
	/**
	 * <p>Title: DefaultedType</p>
	 * <p>Description: Represents the defaulted type for the {@link OpenTypeAttribute#openType} value.</p> 
	 * <p>Company: ICE Futures US</p>
	 * @author Whitehead (nicholas.whitehead@theice.com)
	 * @version $LastChangedRevision$
	 * <p><code>com.ice.helpers.jmx.opentype.OpenTypeAttribute.DefaultedType</code></p>
	 */
	public static final class DefaultedType {}
	

}
