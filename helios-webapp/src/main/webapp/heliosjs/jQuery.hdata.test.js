var tree = {a: "foo", b: { drums: "Ringo", guitar: "George" }, c:["John", "Paul"], deep: {foo: {bar: {snafu: {here: "AAA"}}}}};



console.info("========== HData Read Tests ==========");
assert("Ringo", function() { return hdata("b/drums", tree);});
assert("AAA", function() { return hdata("deep/foo/bar/snafu/here", tree);});
assert("foo", function() { return hdata("a", tree);});
assert("Paul", function() { return hdata("c/1", tree);});
assert("John", function() { return hdata("c/0", tree);});
assert(tree, function() { return hdata("", tree);});

console.info("========== HData Write Tests ==========");
var wtree = deepClone(tree);
assert("Fred", function() { return hdata("c[0]", wtree, "Fred");});
assert("John", function() { return hdata("c[0]", tree);});
assert("ZZZ", function() { return hdata("deep/foo/bar/snafu/here", wtree, "ZZZ");});
assert("AAA", function() { return hdata("deep/foo/bar/snafu/here", tree);});
assert("bar", function() { return hdata("a", wtree, "bar");});
assert("foo", function() { return hdata("a", tree);});
var empty = {};
$.each(recurseData(tree), function(k,v) {
    hdata(k, empty, v);
});
assert(true, function() { return navMapsSame(empty, tree);});
hdata("deep/foo/bar/snafu/here", tree, "GGG" );
assert("GGG", function() { return hdata("deep/foo/bar/snafu/here", tree);});
assert(false, function() { return navMapsSame(empty, tree);});