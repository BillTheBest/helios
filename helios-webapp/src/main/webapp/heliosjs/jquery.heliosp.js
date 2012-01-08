/**
 *  Helios jQuery Data Plugin
 *  Helios Development Group LLC, 2011
 *  @author whitehead (nwhitehead AT heliosdev DOT org)
*/

//   Call like this:  $('*').heliosp("load", {a: "foo",b: "bar"});

(function( $ ){
	$.fn.heliosp = function(method) {
		/** the cached jmx tree id */
		var _jmxTreeId = null;
		/** the cached jmx tree selector */
		var _jmxTreeSel = null;
		/** the cached jmx tree window */
		var _jmxTreeWindow = null;
		
		
		/** the cached jmx client */
		var _jmxClient = null;
		/** the plugin methods */
		var methods = {
				load: function(options) {
					return this.each(function() {
						$this = $(this);
						console.info("Processing [%s] with [%s]", this, options);
					});			
				},
				jmxTree: function() {
					if(_jmxTree==null) {
						_jmxTreeId = "jmx-tree-" + new Date().getTime();
						_jmxTreeSel = "#" + _jmxTreeId;
						$("<div id='" + treeId + "'></div>").dialog();

						$('#' + treeId).jstree("create",-1,false,
								{attr: {id: "helios-root", rel : "Root", q:""}, 
						        state: "open", data : {title: "HeliosRoot"}},false,true);

						$("#" + treeId).jstree({core : {animation : 10}, 						
						    plugins : [ "themeroller", "html_data", "ui", "crrm", "types"],
						    themes : { "theme" : "apple" }, 
						    types : $.heliosp._typeMappings
						});
						
						_jmxTree = $("#" + treeId).jstree("create", '#root', "first" , {
													attr: {id: "jmx-tree-root", rel: "Root", pop: false, ts: 0, treeid: "mbeanserver-tree"}, 
													state: "open",  
													data : {title: "HeliosRoot"}
												}, false, true);						
					}
				},
				jmxTreeWindow: function() {
					if(_jmxTreeWindow==null) {
						var uniqueClass = "jmx-tree-dialog-" + new Date().getTime();
						var treeId = uniqueClass;
						$(_jmxTreeSel).dialog({dialogClass: uniqueClass, title: "Available MBeanServers", position: "left", autoOpen: "true",
							close : function(evt, dlg) { 
								$(_jmxTreeSel).show(); 
							},
							open : function(evt, dlg) { 
								$(_jmxTreeSel).hide(); 
							}}).dialogExtend(
									{minimize: true}
							);
						$('.' + uniqueClass).attr("id", treeId);
						_jmxTreeWindow = $('#' + treeId)[0];
					}
					return _jmxTreeWindow;

				}
		};
		if (methods[method]) {
			return methods[ method ].apply(this, Array.prototype.slice.call(arguments, 1));
		} else if (typeof method === 'object' || ! method) {
			return methods.load.apply(this, arguments);
		} else {
			$.error('Method ' +  method + ' does not exist on jQuery.heliosp');
		}    		
	};
})( jQuery );


/**
1. Wrap Jolokia client.
2. Modify server to type all JSON returns.
3. Implement heliosp ops for "helios-ui" classed objects.
	Decorate: where has class "helios-ui" but has no decorated flag or data segment
	Populate: where has class "helios-ui" but populated flag is false
	Update: targeted, consults timestamp to see if an update is applicable
4. Complete the type definitions for the jmx-tree root
5. Apply Decorate/Populate/Update to jmx-tree root.
*/