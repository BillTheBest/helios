console.clear();
String.prototype.startsWith = function(str){
    return (this.indexOf(str) === 0);
}
String.prototype.repeat = function( num, def ) {
    if(num<1) return def==null ? "" : def;
    return new Array( num + 1 ).join( this );
}
String.prototype.reverse = function() {
    return this.split("").reverse().join("");
}
String.prototype.endsWith = function(t) {
    return this.reverse().startsWith(t);
}
String.prototype.trimChar = function(t) {
    var s = this;
    if(s!="") {
        while(s.startsWith(t)) { s = s.substring(t.length); }
        while(s.endsWith(t)) s = s.substring(0, s.length-(t.length));
    }
    return s;
}	
var assert = function(value, expression, throwsex) {
    var error = null;
    var th = throwsex==null ? false : throwsex;
    try {
        var val = expression();
    } catch (e) {
        error = e;
        if(th) throw e;
    }
    var r = new RegExp("return (.*);", "m");
    var texp = r.exec(expression.toString())[1] + "==[" + value + "]";
    if(value!=val) { console.error("Assertion [%s] Failed. Value was [%s] but expected [%s]. Exception:[%s]", texp, val==null ? "null" : val, value, error==null ? "none" : error); } else {
        console.info("Assertion OK");
    }
}

// Andre Buchda, http://www.linkedin.com/in/andrebuchda
function deepClone(obj) {
    var c = {};
 
    for (var i in obj) {
        var prop = obj[i];
 
        if (typeof prop == 'object') {
           if (prop instanceof Array) {
               c[i] = [];
 
               for (var j = 0; j < prop.length; j++) {
                   c[i].push(prop[j]);
               }
           } else {
               c[i] = deepClone(prop);
           }
        } else {
           c[i] = prop;
        }
    }
 
    return c;
}

function hdata(segments, target, dataValue) {
	var setter = dataValue!=null;
	if(!setter && target==null) return null;
        var allsegs = segments.trimChar('/').split('/');
        if(segments.length==0) {
            return target;
        }
        var nameseg = allsegs[allsegs.length-1];
	var segs = allsegs.slice(0, -1);
        console.log("Segs:[%s], Name:[%s]", segs, nameseg);
	if(target==null) target = {};
	var loc = null;
 	if(segs.length<1) {
		if(setter) {
			return target[nameseg] = dataValue;
		} else {
			return target[nameseg];
		}
 	}
	for(index in segs) {
		var seg = segs[index];
        console.debug("Looking at index [%s][%d]", seg, index);
		if(loc==null) { 
			loc = target[seg]; 
        } else { 
        	loc = loc[seg]; 
        }		
		if(loc==null) {
			if(setter) {
				target[seg] = {};
				loc = target[seg];				
			} else {
				return null;
			}
            console.warn("Miss:" + "-".repeat(parseInt(index)+1) + ">[" + seg + "]"); 
		} else {
            console.info("Hit[" + index + "]" + ("-".repeat(index+1)) + ">[" + seg + "]-->[%s]", loc); 
        }
	}

    if($.isArray(loc)) {
        try {
            if(isNaN(nameseg)) {
                throw "Invalid number [" + nameseg + "]";
            }
            nameseg = parseInt(nameseg);
        } catch (e) {
            throw "Location was an array but name [" + nameseg + "] was not a number.";
        }
    }
	if(setter) loc[nameseg] = dataValue;
	return loc[nameseg];	  				
};
var primitives = {"string":"", "number":"", "boolean":"" };
function recurse(tree, callback, slot, original, context) {
	var _callback = callback!=null ? callback : function(x) { return x; };
	var _original = original==null ? tree : original;
    var _slot = slot==null ? {} : slot;
    var ctx = context==null ? [] : context;
    if (primitives[(typeof tree)] != null) {    
    	console.info("Slotting:[%s]", ctx);
        _slot[ctx.join("/")] = _callback(tree, callback, _slot, _original, ctx);
        return;
    } 
    for (var i in tree) {
    		ctx.push(i);
    		try {
    			var prop = tree[i];
    			if (primitives[(typeof prop)] != null) {
    				console.info("Slotting:[%s]", ctx);
    				_slot[ctx.join("/")] = _callback(prop, callback, _slot, _original, ctx);
    			} else if (prop instanceof Array) {
                        var le = prop.length;
	        	for (var j = 0; j < le;  j++) {
                                console.info("In Arr [%s]-->[%s]-->[%s]", i, j, prop[j]);
                                if(primitives[(typeof prop[j])] != null) {
                                    ctx.push(j); 
                                    try {recurse(prop[j], callback, _slot, _original, ctx);} catch(e) {} finally {ctx.pop()}
                                } else {
                                    try {recurse(prop[j], callback, _slot, _original, ctx);} catch(e) {}
                                }
	        		
	        	}
    			} else {
	        		try {
	        			recurse(prop, callback, _slot, _original, ctx);
	        		} finally {
	        		}			
    			}
    		} finally {
	     		ctx.pop();
    		}
    }
    return _slot;	
}

var tree = {a: "foo", b: { drums: "Ringo", guitar: "George" }, c:["John", "Paul"], deep: {foo: {bar: {snafu: {here: "AAA"}}}}};
console.info("========== Heval Read Tests ==========");
assert("Ringo", function() { return hdata("b/drums", tree);});
assert("AAA", function() { return hdata("deep/foo/bar/snafu/here", tree);});
assert("foo", function() { return hdata("a", tree);});
assert("Paul", function() { return hdata("c/1", tree);});
assert("John", function() { return hdata("c/0", tree);});
assert(tree, function() { return hdata("", tree);});

console.info("========== Heval Write Tests ==========");
var wtree = deepClone(tree);
assert("Fred", function() { return hdata("c/0", wtree, "Fred");});
assert("John", function() { return hdata("c/0", tree);});
assert("ZZZ", function() { return hdata("deep/foo/bar/snafu/here", wtree, "ZZZ");});
assert("AAA", function() { return hdata("deep/foo/bar/snafu/here", tree);});
assert("bar", function() { return hdata("a", wtree, "bar");});
assert("foo", function() { return hdata("a", tree);});


var ctree = {a: "foo", c:["John", "Paul"], b: { drums: "Ringo", guitar: "George" }, deep: {foo: {bar: {snafu: {here: "AAA"}}}}};

console.info("==============  Recurse Test ===============");
console.dir(recurse(ctree, function(x){
    return x;
} ));
var empty = {};
$.each(recurse(ctree), function(k,v) {
    hdata(k, empty, v);
});
console.dir(empty);
