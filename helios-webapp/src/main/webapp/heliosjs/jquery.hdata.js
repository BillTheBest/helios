/**
 *  Helios jQuery Data Manipulation Plugin
 *  Helios Development Group LLC, 2011
 *  @author whitehead (nwhitehead AT heliosdev DOT org)
*/
/**
 * A set of type names we consider "primitives"
 */
var primitives = {
	"string" : "",
	"number" : "",
	"boolean" : ""
};

/** the heval expression pattern for getting data from a data object */
var hevalGetData = new RegExp("<-\\$\\{(.*)\\}");
/** the heval expression pattern for setting data on a data object */
var hevalSetData = new RegExp("->\\$\\{(.*)\\}\\((.*)\\)");

(function( $ ){

	$.fn.hdata = function() {
		var methods = {
				recurseData: function() {
			
				},
				hdata: function(dataObject, expr) {
					if(expr==null || expr=="") return null;
					if(!expr.trim().hstartsWith("h~")) {
						return expr;
					}
					expr = expr.trim().substring(2);
					if(hevalGetData.test(expr)) {		
						return hdata(hevalGetData.exec(expr)[1], dataObject);		  				
					} else if(hevalSetData.test(expr)) {		
						var groups = hevalGetData.exec(expr);
						return hdata(groups[1], dataObject, groups[2]);		  				
					}			
				}
				
		};
		if (methods[method]) {
			return methods[ method ].apply(this, Array.prototype.slice.call(arguments, 1));
		} else if (typeof method === 'object' || ! method) {
			return methods.load.apply(this, arguments);
		} else {
			$.error('Method ' +  method + ' does not exist on jQuery.simplegmap');
		}    
	};

})( jQuery );

//====================================================
//    String Protoypes
//====================================================

/**
 * String.prototype.hstartsWith. Indicates if this string starts with the passed string
 * @param str the string prefix to test for
 * @return true if this string starts with <code>str</code>, false if it does not.
 */
String.prototype.hstartsWith = function(str) {
	return (this.indexOf(str) === 0);
};
/**
 * String.prototype.hendsWith. Indicates if this string ends with the passed string
 * @param str the string suffix to test for
 * @return true if this string ends with <code>str</code>, false if it does not.
 */
String.prototype.hendsWith = function(t) {
	return this.hreverse().hstartsWith(t);
};
/**
 * String.prototype.hrepeat. Returns a new string with this string's value
 * repeated <code>num</code> times.
 * @param num The number of times to repeat this string.
 * @param def A default value to return if <code>num</code> is less than one.
 * @return the repeated string.
 */
String.prototype.hrepeat = function(num, def) {
	if (num < 1)
		return def == null ? "" : def;
	return new Array(num + 1).join(this);
};
/**
 * String.prototype.hreverse. Returns this string reversed.
 * @return the reversed string.
 */
String.prototype.hreverse = function() {
	return this.split("").reverse().join("");
};
/**
 * String.prototype.htrimChar. Trims leading and trailing instances of the passed string
 * @param t The string to trim leading and trailing instances of
 * @return the trimed string
 */
String.prototype.htrimChar = function(t) {
	var s = this;
	if (s != "") {
		while (s.hstartsWith(t)) {
			s = s.substring(t.length);
		}
		while (s.hendsWith(t))
			s = s.substring(0, s.length - (t.length));
	}
	return s;
};

//====================================================


/**
 * Determines if the passed object is a primitive
 * @param obj The object to test
 * @return true if the passed object is a string, number, boolean or null.
 */
function isPrim(obj) {
	if(obj==null) return true;
	return (primitives[(typeof obj)] != null);
};

