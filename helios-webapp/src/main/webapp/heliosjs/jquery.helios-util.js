console.clear();
(function($) {
	String.prototype.reverse = function() {
	    return this.split("").reverse().join("");
	}
	String.prototype.endsWith = function(t) {
	    return this.reverse().startsWith(t);
	}
	String.prototype.trimChar = function(t) {
	    var s = this;
	    while(s.startsWith(t)) { s = s.substring(t.length); }
	    while(s.endsWith(t)) s = s.substring(0, s.length-(t.length));
	    if(s.length==0) return null; 
	    return s;
	}	
	  $.heliosutil = {
			    author:"Nicholas Whitehead",
			    version:"1.0",
			    gsegpattern: new RegExp("<-\\$\\{(.*)\\}"),
			    ssegpattern: new RegExp("->\\$\\{(.*)\\}\\((.*)\\)"),
			    heval:function(expression){
		  			if(expression==null || expression=="") return null;
		  			if(!expression.trim().startsWith("~")) {
		  				return expression;
		  			}
		  			expression = expression.trim().substring(1);
		  			if($.heliosutil.gsegpattern.test(expression)) {
		  				//console.log("Detected DataSeg Expression")
		  				return $.heliosutil._datasegment(this, $.heliosutil.gsegpattern.exec(expression)[1]);		  				
		  			} else if($.heliosutil.ssegpattern.test(expression)) {
		  				//console.log("Detected DataSeg Expression")
		  				var groups = $.heliosutil.gsegpattern.exec(expression);
		  				$.heliosutil._datasegment(this, groups[1], groups[2]);		  				
		  			}

	  			},
	  			_datasegment: function(set, dexp, dataValue) {
	  				//console.log("Data Segment Request for [%s]", dexp);
	  				if(dexp==null || set==null) return null;
	  				dexp = dexp.trimChar('/');
	  				if(dexp==null) return null;
	  				var segments = dexp.split("/");
	  				var setter = dataValue!=null;
	  				
	  				
		  			var arr  = {};
		  			var cntr = 0;
		  			
		  			$.each(set, function(k, v) {		  				
		  				var id = v.id;
		  				if(id==null||id.trim()=="") id = ("" + cntr++);
		  				var target = $(v).data(segments[0]);
		  				if(target==null) return;
		  				$.each(segments, function(k, v){
		  					// need to test each segment for existence all the way down.
		  					// if(dataValue!=null) then create any missing segments.
		  					if(k>0) { 
		  						if(setter) {
		  							target[v] = dataValue;
		  						} else {
		  							target = target[v];
		  						}
		  					}
		  				});
		  				if(dataValue==null) { arr[id] = target; }
		  			});
		  			return $.heliosutil._crush(arr);	
	  			},
	  			/**
	  			 * Splits the passed segments by '/' and uses each value to navigate into a map to get or set a value.
	  			 * @segments A navigation directive using '/' delimited keys. e.g. foo/bar/snafu which would target {foo:{bar:{snafu:""}}}  or {foo:{bar:{snafu:{}}}} 
	  			 * @target the map to navigate. If null, will create one
	  			 * @dataValue the value to set in the passed map. If null, function is a getter. 
	  			 */
	  			hdata: function(segments, target, dataValue) {
	  				var setter = dataValue!=null;
	  				if(!setter && target==null) return null;
	  				var segs = segs.trimChar('/').split('/').slice(-1);
	  				var nameseg = segments[segments.length-1];
	  				if(target==null) target = {};
	  				var loc = null;
	  				for(index in segs) {
	  					var seg = segs[index];
	  					if(loc==null) {
	  						loc = target[seg];
	  						if(loc==null) {
	  							if(setter) {
	  								loc[seg] = {};
	  							} else {
	  								return null;
	  							}
	  						}
	  					}
	  				}
	  				if(setter) loc[nameseg] = dataValue;
	  				return loc[nameseg];	  				
	  			},
	  			deepClone: function(from, to) {
	  			    if (from == null || typeof from != "object") return from;
	  			    if (from.constructor != Object && from.constructor != Array) return from;
	  			    if (from.constructor == Date || from.constructor == RegExp || from.constructor == Function ||
	  			        from.constructor == String || from.constructor == Number || from.constructor == Boolean)
	  			        return new from.constructor(from);

	  			    to = to || new from.constructor();

	  			    for (var name in from)
	  			    {
	  			        to[name] = typeof to[name] == "undefined" ? this.extend(from[name], null) : to[name];
	  			    }

	  			    return to;
	  			},	  			
	  			_size: function(arr) {
	  				var cntr = 0;
	  				$.each(arr, function() {cntr++;});
	  				return cntr;
	  			},
	  			_crush: function(arr) {
	  				if(arr==null) return null;
	  				var sz = $.heliosutil._size(arr);
	  				if(sz==0) return null;
	  				if(sz > 1) return arr;
	  				var singleRet = null;
	  				$.each(arr, function(k, v){singleRet = v;});
	  				return singleRet;
	  			}
	  			
	  }
	  $.fn.heval=$.heliosutil.heval;
})(jQuery);
$('#mbeanserver-tree').data({foo: {bar : {snafu : "Bingo !"} }})
$('#mbeanserver-tree').heval("*<-${foo/bar/snafu}");
//console.dir($('div').heval("<-${foo}"));



//Utilities pending:
//@${X}   extract tokens from local data segment
//X:->Y	set the data segment named X to the value of eval(Y)

/*
var a = "foo";
var b = {};
var c = [];
var d = function(foo) {};
var arrs = {"a": a, "b": b, "c" : c, "d" : d};
jQuery.each(arrs, function(k,v) {
    console.log("Var [%s] isArray:[%s]", k, jQuery.isArray(v));
    console.log("Var [%s] isPlainObject:[%s]", k, jQuery.isPlainObject(v));
    console.log("Var [%s] isFunction:[%s]", k, jQuery.isFunction(v));
    console.log("===============================");
});



function deepClone(from, to) {
    if (from == null || typeof from != "object") return from;
    if (from.constructor != Object && from.constructor != Array) return from;
    if (from.constructor == Date || from.constructor == RegExp || from.constructor == Function ||
        from.constructor == String || from.constructor == Number || from.constructor == Boolean)
        return new from.constructor(from);

    to = to || new from.constructor();

    for (var name in from)
    {
        to[name] = typeof to[name] == "undefined" ? this.extend(from[name], null) : to[name];
    }

    return to;
}

==========================

console.clear();
	String.prototype.reverse = function() {
	    return this.split("").reverse().join("");
	}
	String.prototype.endsWith = function(t) {
	    return this.reverse().startsWith(t);
	}
	String.prototype.trimChar = function(t) {
	    var s = this;
	    while(s.startsWith(t)) { s = s.substring(t.length); }
	    while(s.endsWith(t)) s = s.substring(0, s.length-(t.length));
	    if(s.length==0) return null; 
	    return s;
	}	
        var assert = function(message, value, expression) {
            var error = null;
            try {
                var val = expression();
            } catch (e) {
                console.log(e);
                error = e;
            }
            
            if(value!=val) { console.error("Assertion [%s] Failed. Value was [%s] but expected [%s]. Exception:[%s]", message, val==null ? "null" : val, value, error==null ? "none" : error); } else {
                console.info("Assertion [%s] OK", message);
            }
        }
function hdata(segments, target, dataValue) {
	var setter = dataValue!=null;
	if(!setter && target==null) return null;
	var segs = segments.trimChar('/').split('/');
	var nameseg = segs[segs.length-1];
        console.log("Segs:[%s], Name:[%s]", segs, nameseg);
	if(target==null) target = {};
	var loc = null;
        if(segs.length==1) {
            if(setter) target[segs[0]] = dataValue;
            else return target[segs[0]];
        }
	for(index in segs) {
		var seg = segs[index];
		if(loc==null) {
			loc = target[seg];
			if(loc==null) {
				if(setter) {
					loc[seg] = {};
				} else {
					return null;
				}
			}
		}
	}
        if($.isArray(loc)) {
            try {
                nameseg = parseInt(nameseg);
            } catch (e) {
                console.error(e);
                throw "Location was an array but name [" + nameseg + "] was not a number.";
            }
        }
	if(setter) loc[nameseg] = dataValue;
	return loc[nameseg];	  				
};

var tree = {a: "foo", b: { drums: "Ringo", guitar: "George" }, c:["John", "Paul"]};
assert("'b/drums'", "Ringo", function() { return hdata("b/drums", tree);});
assert("'a'", "foo", function() { return hdata("a", tree);});
assert("'c/1'", "Paul", function() { return hdata("c/Q", tree);});


*/