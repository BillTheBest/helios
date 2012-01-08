/**
 * Helios, OpenSource Monitoring
 * Brought to you by the Helios Development Group
 *
 * Copyright 2007, Helios Development Group and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org. 
 *
 */
package test.org.helios.aop.tesclass;

import org.helios.aop.ConstructorDefinition;
import org.helios.aop.DynaClassFactory;
import org.helios.aop.FieldDefinition;
import org.helios.aop.MethodDefinition;

/**
 * <p>Title: TestDynaClass</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>test.org.helios.aop.tesclass.TestDynaClass</code></p>
 */

public abstract class TestDynaClass {
	protected String content;
	
	@ConstructorDefinition(id="Simple")
	public TestDynaClass(String content) {
		super();
		this.content = content;
	}
	
	@MethodDefinition(
			bsrc="return new StringBuilder(content).reverse().toString();",
			fields={
					@FieldDefinition(name="foo", type=int.class, initializer="47")
			}
			
	)
	public abstract String rev();
	
	
	public static void main(String[] args) {
		log("TestDynaClass");
		try {
			TestDynaClass tdc = (TestDynaClass)DynaClassFactory.generateClassInstance("Foo", TestDynaClass.class, "Simple", "Hello Venus");
			log(tdc.rev());
		} catch (Exception e) {
			e.printStackTrace(System.err);
		}
	}
	
	public static void log(Object msg) {
		System.out.println(msg);
	}
}
