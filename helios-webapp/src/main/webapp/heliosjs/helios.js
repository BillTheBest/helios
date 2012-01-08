var ATTR_DECODE = {"HostId" : "host", "VMId" : "vm", "DefaultDomain" : "domain"};
var ATTR_OP = {"HostId" : "search", "VMId" : "search", "DefaultDomain" : "search", "MBean" : "list"};
var ATTR_CHILD_DECODE = {"Root" : "HostId", "HostId" : "VMId", "VMId" : "DefaultDomain", "DefaultDomain" : "MBean"};
var NODE_REFRESH_DELAY = 15000;

$(function(){
	console.info("Initializing Console");
	initConsole();
});


function initConsole() {
	console.info("Initializing MBeanServerTree");
	//$.getScript("heliosjs/heliosp.js");
	var heliosTypes = $.heliosp.mappings();
	/*
	$("#mbeanserver-tree").jstree("create",-1,false,{attr: {id: "helios-root", rel : "Root", q:""}, state: "open", data : {title: "HeliosRoot"}},false,true);
	$("#mbeanserver-tree-open").hide();
	var dialogId = "" + (new Date().getTime()) + "-mbeanserver-dialog";
	$('#mbeanserver-tree').dialog({dialogClass: "mbeanserver-dialog", title: "Available MBeanServers", position: "left", autoOpen: "true",
				close : function(evt, dlg) { 
					$("#mbeanserver-tree-open").show(); 
				},
				open : function(evt, dlg) { 
					$("#mbeanserver-tree-open").hide(); 
				}
		}).dialogExtend({
		minimize: true,
		events : { 
			"restore" : function(evt, dlg) {
				$('#mbeanserver-dialog').draggable({
							cursor: "move",
							containment: '#mbeanserverContainerX'
						});
				$('#mbeanserver-dialog').resizable({
							cursor: "resize",
							containment: '#mbeanserverContainerX'
						});		
			}
		}
	});
	$('#mbeanserver-tree').css("overflow", "scroll");
	$('.mbeanserver-dialog').attr("id", "mbeanserver-dialog");
	$('#mbeanserver-dialog').appendTo('#mbeanserverContainerX');
	$( "#mbeanserver-dialog" ).bind( "dialogclose", function(event, ui) {
		$("#mbeanserver-tree-open").show(); 
	});	
	$( "#mbeanserver-dialog" ).bind( "dialogopen", function(event, ui) {
		$("#mbeanserver-tree-open").hide(); 
	});	
	$( "#mbeanserver-tree-open" ).bind( "click", function(event, ui) {
		$('#mbeanserver-tree').show();
	});
	$('#mbeanserver-dialog').css('overflow', 'hidden');
	
	
	//$('#mbeanserver-dialog').remove().prependTo('#tabs');
	
	//$('#mbeanserver-dialog').resizable({containment: "mbeanserverContainer"});
	//$('#mbeanserver-dialog').dialog("open");
	
	
	*/
	
	
	console.log("Initializing JMXTree...");
	$.heliosp("jmxTreeWindow");
	/*
	var mbeans = jolokia.search("org.helios.jmx.mbeanservers:*");
	console.log("Found [%d] MBeanServers", mbeans.length);
	for(mbean in mbeans) {
		console.log("Reading MBean [%s]", mbeans[mbean]);
		var resp = jolokia.request({ type: "read", mbean: mbeans[mbean] });			
		var nextParent = popServer(resp.value, "HostId", "#helios-root", "closed");
	}
	*/
}
	


function getNodeData(e) {
	console.log("Populating Node [%s]", e.id);
	var sel = '#' + e.id;
	var node = $(sel);
	if(node==null) {
		console.error("Pop Request Node [%s] was null", e.id);
		return false; 
	}		
			
	var pop = ($(sel).attr("pop")==='true');
	var ts = parseInt($(sel).attr("ts"));
	var attr = $(sel).attr("attr");
	var key = $(sel).attr("key");
	var rel = $(sel).attr("rel");
	var q = $(sel).attr("q");
	var query = "org.helios.jmx.mbeanservers:" + q + ",*"; 				
	var opt = rel=="DefaultDomain" ? 0 : null;				
	var elapsed = (new Date().getTime()-ts);
	console.log("Pop: [%s], ts: [%d], attr: [%s], name: [%s], elapsed [%d], query: [%s], opt: " + opt, pop, ts, attr, key, elapsed, query); 
	if(!pop || elapsed > NODE_REFRESH_DELAY) {
		$("#mbeanserver-tree").jstree("remove", $('li[parent=' + e.id + ']'));
		var mbeans = jolokia.search(query);
		for(mbean in mbeans) {
			console.log("Reading MBean [%s]", mbeans[mbean]);
			var resp = jolokia.request({ type: "read", mbean: mbeans[mbean] });
			var server = opt==null ? resp.value : resp.value.Domains;
			if(opt==null) {
				popServer(resp.value, ATTR_CHILD_DECODE[rel], sel, "closed");
			} else {
				for(domain in resp.value.Domains) {
					console.log("Adding:" +  server[domain]); 
					popServer(resp.value.Domains, ATTR_CHILD_DECODE[rel], sel, "closed", domain);
				}
			}			
									
		}
	}
	return true; 
}
function displayNode(e) {
	console.log("Displaying Node [%s]", e);
}

function popServer(server, type, parent, state, index) {
	console.log("Server Type [%s]", $.type(server));
	var key = index==null ? server[type] : server[index];
	var id = type + "-" + key;
	var sel = "#" + id;
	console.log("popServer [%s] [%s] [%s] [%s]", key, type, parent, state); 
	if($(id).length==0) {					
		console.log("Adding [%s]", id);
		var q = $(parent).attr("q");
		if(q=="") {
			q += (ATTR_DECODE[type] + "=" + key);
		} else {
			q += ("," + ATTR_DECODE[type] + "=" + key); 
		}
		var now = new Date().getTime();
		$("#mbeanserver-tree").jstree("create",parent, "inside" ,
			{attr: {id: id, rel: type, attr: ATTR_DECODE[type], q: q ,pop: false, ts: 0, key: key, parent : parent.substring(1)}, 
			state: state, 
			data : {title: key}}, false, true);
		$(parent).attr({"ts" : now, "pop" : true});					
	}
	return sel;
}

String.prototype.startsWith = function(str){
    return (this.indexOf(str) === 0);
}
