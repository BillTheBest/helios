/**
 * ====================================================================
 * jQuery.hdata.js
 * A plugin for reading and manipulating data objects.
 * @author whitehead (nwhitehead AT heliosdev DOT org)
 * ====================================================================
 */

/**
 * A set of type names we consider "primitives"
 */
var primitives = {
	"string" : "",
	"number" : "",
	"boolean" : ""
};
/**
 * Determines if the passed object is a primitive
 * @param obj The object to test
 * @return true if the passed object is a string, number, boolean or null.
 */
function isPrim(obj) {
	if(obj==null) return true;
	return (primitives[(typeof obj)] != null);
};
/**
 * <p>
 * Recursively navigates the structure of an object and processes each primitive
 * field found. Generates a contextual navigational map of the primitives found
 * in the object that has a structure of <b><code>key:value</code></b> where
 * the key is a hierarchical string that indicates the relative position of the
 * primitive and the value is the value of the field. e.g.
 * <ul>
 * <li>foo/bar: "Hello World"</li>
 * <li>foo/dev/num: 27</li>
 * <li>foo/arr[3]: "Jupiter"</li>
 * </ul>
 * </p>
 * <p>
 * For each primitive found, the value of the primitive can be formatted by an
 * optional <b><code>callback</code></b> which is passed the same argument
 * signature as this method. If no callback is passed, the primitive value is
 * read as is.
 * </p>
 * <p>
 * Parameters marked as <b>Recursively used only.</b> should not have values
 * passed when initially calling the method.
 * 
 * @param tree The object to recurse.
 * @param filter A regex to filter in the nav map entries we want to use. A null will match all entries.            
 * @param callback An optional callback used to format the value of the primitive fields.
 * @param slot The navigational map which accumulates the primitive field references. <b>Recursively used only.</b>
 * @param original A reference to the original top level object being recursed.<b>Recursively used only.</b>
 * @param context The contextual stack that accumulates the navigational key.<b>Recursively used only.</b>
 * @return A contextual navigational map of the primitives found in the passed object.
 */
function recurseData(tree, filter, callback, slot, original, context) {
	var _callback = callback != null ? callback : function(x) {
		return x;
	};
	var _filter = filter==null ? filter : new RegExp(filter); 
	var _original = original == null ? tree : original;
	var _slot = slot == null ? {} : slot;
	var ctx = context == null ? [] : context;
	if (isPrim(tree)) {
		var navKey = ctx.join("/").replace(/\/\[/g, "[");
		var newValue = tree;
		if(_filter != null) {
			var testPattern = navKey + ":" + "" + tree;
			var matches = _filter.test(testPattern);			
			if(!matches) {
				return;
			}
		};
		newValue = _callback(tree, navKey, _original);
		_slot[navKey] = newValue;		
		return;
	}
	for ( var i in tree) {
		ctx.push(i);
		try {
			var prop = tree[i];
			if (isPrim(prop)) {
				var navKey = ctx.join("/").replace(/\/\[/g, "[");
				var newValue = prop;
				if(_filter != null) {
					var testPattern = navKey + ":" + prop;
					var matches = _filter.test(testPattern);					
					if(!matches) {
						continue;
					}
				}
				newValue = _callback(prop, navKey, _original);
				_slot[navKey] = newValue;	
			} else if (prop instanceof Array) {
				var le = prop.length;
				for ( var j = 0; j < le; j++) {
					ctx.push('[' + j + ']');
					recurseData(prop[j], _filter, _callback, _slot,_original, ctx);
					ctx.pop();					
				}
			} else {
				recurseData(prop, _filter, _callback, _slot, _original, ctx);
			}
		} finally {
			ctx.pop();
		}
	}
	return _slot;
};

/**
 * Accepts a navigational key ( see <b><code>recurseData</code><b> ) and a
 * target object, returning the value of the field found at the passed
 * navigational point. If a <b></code>dataValue</code></b> is passed as the
 * third optional argument, the method is a setter, setting the value of the
 * navigational point indicated by the key.
 * 
 * @param nKey The navigational key
 * @param target The target object to read/write.
 * @param dataValue The optional data value to set.
 * @param index An optional, recursion only array index
 * @param nKeys The original navigational key from the top of the recursion stack            
 * @return the value of the field pointed to by the <b></code>nKey</code></b>. 
 * This will be the new value if a <b></code>dataValue</code></b> was supplied.
 */

function setTarget(segment, target, dataValue, index, nKeys) {
	// ====== variables =======
	var setter = (dataValue != null);
	var nameSegArray = segment.match(/(.*)\[(\d+)\]/);
	var nameSegName = nameSegArray==null ? null : nameSegArray[1];
	var nameSegIndex = nameSegArray==null ? null : nameSegArray[2];
	var passIndex = null;
	// if index is not null, and dataValue is not null, advance the reference to the array index
	if(index!=null && dataValue == null) {
		// array will have already been created
		if(nameSegArray!=null) {
			
		} else {
			target = target[index];
		}
	}
	if(nameSegArray!=null) {  	// the segment is an array reference e.g /foo[2]/
		// ==== set array reference ====
		// we're not setting an array item
		if(target[index==null ? nameSegName : index]==null) {  // check to see if reference already exists
			// create the array at this reference
			var arr = [];
			if(dataValue!=null) {
				if(index!=null) {
					//console.error("==============DataValue on Index: D:[%s]I:[%s]S:[%s]", dataValue, index, segment);					
				} else {
					arr[nameSegIndex] = dataValue;
				}
				passIndex = -1;  // we're setting the value so don't pollute the passIndex
			}
			target[nameSegName]= arr; 
		} else {
			if(dataValue!=null) {
				// check to see if the nameseg exists
				if(target[nameSegName]==null) {
					if(index==null) {
						target[nameSegName] = [];
					} else {
						if(target[index][nameSegName]==null) {
							target[index][nameSegName] = [];
						}
					}					
				}
				// we have a dataValue so set it on the index of the array
				if(index==null) {
					target[nameSegName][nameSegIndex] = dataValue;
				} else {
					target[index][nameSegName][nameSegIndex] = dataValue;
				}
				passIndex = -1;  // we're setting the value so don't pollute the passIndex
			} else {
				//console.log("???");
			}
		}
		// advance the reference to the created array
		target = target[nameSegName];
		if(nameSegIndex!=null) {
			if(target[nameSegIndex]!=null) {
				target = target[nameSegIndex];
				passIndex = -1;
			}
		}
		// set the passIndex so the next call here sets the correct array entry
		passIndex = passIndex==-1 ? null : nameSegIndex;
	} else { 					// the segment is a flat reference  e.g. /foo/
		// ==== set flat reference ====
		// we're not setting an array item
		if(index==null) {
			if(dataValue!=null) {
				target[segment] = dataValue;
			} else {
				if(target[segment]==null) {
					target[segment] = {};
				}
			}
		} else {
			if(dataValue!=null) {
				if(target[index]==null) {
					target[index] = {};
				}
				target[index][segment] = dataValue;
			} else {
				console.info("??: Index:[%d] Segment:[%s] in Directive [%s]", index, segment, nKeys);
			}
		}
		// advance the reference to the created object
		target = target[segment];		
	}
	return [target, passIndex];
}

/**
 * Accepts a navigational key ( see <b><code>recurseData</code><b> ) and a
 * target object, returning the value of the field found at the passed
 * navigational point. If a <b></code>dataValue</code></b> is passed as the
 * third optional argument, the method is a setter, setting the value of the
 * navigational point indicated by the key.
 * 
 * @param nKey The navigational key
 * @param target The target object to read/write.
 * @param dataValue The optional data value to set.
 * @param index The recursively passed index if the last segment was an array
 * @param original The original nKey from the top of the recursive stack
 * @return the value of the field pointed to by the <b></code>nKey</code></b>.
 *         This will be the new value if a <b></code>dataValue</code></b>
 *         was supplied.
 */
function hdata(nKey, target, dataValue, index, original) {
	try {
		if(target==null && dataValue==null) {
			throw "hdata call for [" + nKey + "] had no target or dataValue";
		}
		var _original = original==null ? nKey : original;
		var allsegs = ("" + nKey).htrimChar('/').split('/');
		if (nKey.length == 0) {
			return target;
		}
		var setTResult = setTarget(allsegs[0], target, allsegs.length==1 ? dataValue : null, index, _original);
		target = setTResult[0];
		var passIndex = setTResult[1];
		return hdata(allsegs.slice(1).join("/").htrimChar('/'), target, dataValue, passIndex, _original);
	} catch (e) {
		var err = "Failed to process directive [" + nKey + "]" + (original!=null ? (" in [" + original + "]") : "") + "[" + e.message + ":" + e.lineNo + "]";
		console.log(e);
		throw e;
	}
	
}



/**
 * Compares the nav maps of two objects and compares them for equality.
 * @param A The first object
 * @param B the second object
 * @return true if the nav maps are the same
 */
function navMapsSame(A, B) {
	var astr = "";
	var objA = recurseData(A);
	var objB = recurseData(B);
	for(k in objA) { astr+= (k + objA[k]); }
	var bstr = "";
	for(k in objB) { bstr+= (k + objB[k]); }
	return bstr==astr;	
}

/**
 * Generates a navmap from one object and applies it to another
 * @param to The target of the navMap apply
 * @param from the source of the navmap
 */
function applyNavMap(to, from) {
	var navMap = recurseData(from);
	for(i in navMap) {
		hdata(i, to, navMap[i]);
	}
}

/**
 * Creates a deep clone of the passed object with no shared references.
 * @param obj The objec to clone
 * @return the cloned object
 */
function deepClone(obj) {
	if(obj==null) return null;
	var c = {};
	applyNavMap(c, obj);
	return c;
};



/**
 * String.prototype.hstartsWith. Indicates if this string starts with the passed
 * string
 * 
 * @param str
 *            the string prefix to test for
 * @return true if this string starts with <code>str</code>, false if it does
 *         not.
 */
String.prototype.hstartsWith = function(str) {
	return (this.indexOf(str) === 0);
};
/**
 * String.prototype.hendsWith. Indicates if this string ends with the passed
 * string
 * 
 * @param str
 *            the string suffix to test for
 * @return true if this string ends with <code>str</code>, false if it does
 *         not.
 */
String.prototype.hendsWith = function(t) {
	return this.hreverse().hstartsWith(t);
};
/**
 * String.prototype.hrepeat. Returns a new string with this string's value
 * repeated <code>num</code> times.
 * 
 * @param num
 *            The number of times to repeat this string.
 * @param def
 *            A default value to return if <code>num</code> is less than one.
 * @return the repeated string.
 */
String.prototype.hrepeat = function(num, def) {
	if (num < 1)
		return def == null ? "" : def;
	return new Array(num + 1).join(this);
};
/**
 * String.prototype.hreverse. Returns this string reversed.
 * 
 * @return the reversed string.
 */
String.prototype.hreverse = function() {
	return this.split("").reverse().join("");
};
/**
 * String.prototype.htrimChar. Trims leading and trailing instances of the
 * passed string
 * 
 * @param t
 *            The string to trim leading and trailing instances of
 * @return the trimed string
 */
String.prototype.htrimChar = function(t) {
	var s = this;
	if(t==null) t=" ";
	if (s != "") {
		while (s.hstartsWith(t)) {
			s = s.substring(t.length);
		}
		while (s.hendsWith(t))
			s = s.substring(0, s.length - (t.length));
	}
	return s;
};

function assert(value, expression, throwsex) {
	var error = null;
	var th = throwsex == null ? false : throwsex;
	try {
		var val = expression();
	} catch (e) {
		error = e;
		if (th)
			throw e;
	}
	var r = new RegExp("return (.*);", "m");
	var texp = r.exec(expression.toString())[1] + "==[" + value + "]";
	if (value != val) {
		console.log("Assertion [%s] Failed. Value was [%s] but expected [%s]. Exception:[%s]",
						texp, val == null ? "null" : val, value,
						error == null ? "none" : error);
	} else {
		console.log("Assertion [%s] OK", texp);
	}
};

/**
 * 
 * @param dataObject
 * @param filter
 * @return
 */
function resolveData(dataObject, filter) {	
	var clone = {};
	recurseData(dataObject, filter, function(_dataValue, navKey, _dataObject){
		var newValue = heval(_dataObject, _dataValue);
		hdata(navKey, clone, newValue);
		return newValue;
	});
	return clone;
}


/** the heval expression pattern for getting data from a data object */
var hevalGetData = new RegExp("<-\\$\\{(.*)\\}");
/** the heval expression pattern for setting data on a data object */
var hevalSetData = new RegExp("->\\$\\{(.*)\\}\\((.*)\\)");

function heval(dataObject, expr) {
	if(expr==null || expr=="") return null;	
	if("string"!=(typeof expr)) {
		return expr;
	}
	if(!expr.hstartsWith("h~")) {
		return expr;
	}
	expr = expr.substring(2);
	if(hevalGetData.test(expr)) {		
		return hdata(hevalGetData.exec(expr)[1].htrimChar("'"), dataObject);		  				
	} else if(hevalSetData.test(expr)) {		
		var groups = hevalGetData.exec(expr);
		return hdata(groups[1], dataObject, groups[2]);		  				
	}
	
};


