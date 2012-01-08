


(function($) {
	$.heliosp = function( method ) {
		if ( methods[method] ) {
			return methods[ method ].apply( this, Array.prototype.slice.call( arguments, 1 ));
		} else if ( typeof method === 'object' || ! method ) {
		    return methods.init.apply( this, arguments );
		} else {
			$.error( 'Method ' +  method + ' does not exist on jQuery.heliosp' );
		}    		 
	};
	var methods = {
			setting: function( name, value ) { if(value==null) return this.settings[name]; else this.settings[name]=value; },
			mapping: function( name, value ) { 
				if(value==null) return $.heliosp._typeMappings["types"][name]; 
				else $.heliosp._typeMappings[name]=value; 
			},
			mappings: function(values) { 
					if(values==null) {
						var arr = {};
						$.extend(arr, $.heliosp._typeMappings);
						return arr;						
					}
					else $.extend($.heliosp._typeMappings, values);  
			},			
			loadTypes: function() {
				try {
					jQuery.each(heliosTypes, function(k, v){
						console.log("Loading:%s %s", k, v.type_name);
						var larr = {};
						$.extend(larr, this.typeDefaults, v);
						$.heliosp._typeMappings["types"][v.type_name] = larr;						
					});
				} catch (e) {
					console.error("Failed to load types:%s", e);
				}
			},
			bindContainer: function(id ) {
				// Generate a bind container to attach to a UI widget
				
				/*
				$("#mbeanserver-tree").jstree("create", '#root', "first" , {
					attr: {id: "jmx-tree-root", rel: "Root", pop: false, ts: 0, treeid: "mbeanserver-tree"}, 
					state: "open",  
					data : {title: "HeliosRoot"}
				}, false, true);
				*/
				
				
			},
			mbeanServerTree: function(options)  { 
				this.options = {
						url:false,
			    	    data:"",
			    	    callback:function(){}
				};
				jQuery.extend (this.options, options);				
			},
			// Creates and caches the MBeanServer Tree Window
			jmxTreeWindow: function(  )  {
				if($.heliosp._jmxTreeWindow==null) {
					$("#mbeanserver-tree").jstree("create",-1,false,{attr: {id: "helios-root", rel : "Root", q:""}, state: "open", data : {title: "HeliosRoot"}},false,true);
					$("#mbeanserver-tree").jstree({
						core : {animation : 10},
						//plugins : [ "themeroller", "html_data", "ui", "crrm", "types"],
						plugins : [ "themeroller", "html_data", "ui", "crrm", "types"],
						themes : { "theme" : "apple" }, 
						types : $.heliosp._typeMappings
					});
					var uniqueClass = "helios-mbeanserver-dialog-" + new Date().getTime();
					$("#mbeanserver-tree").dialog({dialogClass: uniqueClass, title: "Available MBeanServers", position: "left", autoOpen: "true",
						close : function(evt, dlg) { 
							$("#mbeanserver-tree-open").show(); 
						},
						open : function(evt, dlg) { 
							$("#mbeanserver-tree-open").hide(); 
						}}).dialogExtend(
								{minimize: true}
						);
					$('.' + uniqueClass).attr("id", "helios-mbeanserver-dialog");
					$.heliosp._jmxTreeWindow = $('#helios-mbeanserver-dialog')[0];
					//$('#helios-mbeanserver-dialog a.ui-dialog-titlebar-close').remove();
					$('#helios-mbeanserver-dialog').css('overflow', 'hidden');
				} else {
					$('#helios-mbeanserver-dialog').toggle();
				}
				$("#mbeanserver-tree").jstree("create", '#root', "first" , {
						attr: {id: "jmx-tree-root", rel: "Root", pop: false, ts: 0, treeid: "mbeanserver-tree"}, 
						state: "open",  
						data : {title: "HeliosRoot"}
					}, false, true);
					//$(parent).attr({"ts" : now, "pop" : true});					
				
			},
			jmxFindServers: function(typeName, pattern) {
				var type = methods.mapping(typeName);
				if(type==null) {
					throw "The type name [" + typeName + "] is not a valid type";
				}
				var propName = type["prop_name"];
				methods._initJmxClient();
				if(pattern==null) {
					pattern = "*";
				}
				var arr = methods._jmxSearch(propName + "=" + pattern + ",*");
				var results = {};
				results[pattern] = arr;
				return results;
				
			},		
			_jmxClientOptions: function() {
				var options = {};
				jQuery.each($.heliosp._settings, function(k, v) {
					if(k.indexOf("request-") === 0) {
						options[k.replace("request-", "")] = v;
					}
				});
				return options;
			},
			_initJmxClient: function() {
				if($.heliosp._jmxClient==null) {
					var options = methods._jmxClientOptions();
					console.log("Jolokia Options=================");
					console.dir(options);
					$.heliosp._jmxClient = new Jolokia(options);
				}				
			},
			_jmxSearch: function(objectNameProps) {
				methods._initJmxClient();
				if(objectNameProps==null) {
					objectNameProps = "*";
				}
				
				$.heliosp._jmxClient.search($.heliosp._settings["jmx-domain"] + ":" + objectNameProps);
			},
			init: function() {
				methods.loadTypes();
			}
	}; // ==== End of methods ====
	
	// ==== State ====
	$.heliosp._jmxTreeWindow = null;
	$.heliosp._jmxClient = null;
	
	// ==== Settings ====
	
	$.heliosp._settings = {
			"refresh-delay" 			: 15000,
			"pooled-connector" 			: true,
			"request-timeout" 			: 5000,
			"request-maxDepth"			: 20,
			"request-maxCollectionSize"	: 500,
			"request-maxObjects"		: 1000,
			"request-ignoreErrors"		: true,
			"request-method"				: "post",
			"request-async"				: true,
			"request-url"				: "/jolokia/",
			"jmx-domain"				: "org.helios.jmx.mbeanservers"
	};
	
	$.heliosp._typeDefaults = {
			"max_children"	: -1,
			"max_depth"		: -1			
	};
	$.heliosp._typeMappings = {
			"types" : {
			}
	};
	// ==== End of settings ====
	
	// ==== Display Container Definitions ====
	$.heliosp._displayContainers = {
			"helios-jstree-node" : {
				"Root" : {
		
				}
			}
	};
	
	
	// ==== Public methods ====
	$.heliosp.setting = function(name, value) { return $.heliosp("setting", name, value); };
	$.heliosp.mapping = function(name, value) { return $.heliosp("mapping", name, value); };
	$.heliosp.mappings = function(values) { return $.heliosp("mappings", values); };
	$.heliosp.jmxTreeWindow = function() { return $.heliosp("jmxTreeWindow"); };
	
	// ==== End of Public methods ====

	
})( jQuery );




var heliosTypes = HELIOS_TYPES();
$.heliosp("init");
