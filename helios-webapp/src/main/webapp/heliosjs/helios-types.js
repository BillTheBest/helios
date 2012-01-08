console.log("Initializing Helios Types Load");

function HELIOS_TYPES() {
	console.log("Loading Helios Types");
	return {
	RootType : {
		"type_name"		: "root",
		"select_node"	: function() {
			var selectedNode = $('#mbeanserver-tree').jstree.get_selected('#mbeanserver-tree')[0];
			console.log("Select Root Node");
			console.dir(selectedNode);
		},
		"valid_children": "host"		
	},
	HostType : {
		"type_name"		: "host",
		"valid_children": "vm",
		"select_node"	: function() { displayNode("foo"); return true; },
		"open_node"	: function(e) { 
			return getNodeData(e[0]);
		}
	},
	VMType : {
		"type_name"		: "vm",
		"valid_children": "domain",
		"select_node"	: true,
		"open_node"	: function(e) { return getNodeData(e[0]);}
	},
	DefaultDomainType : {
		"type_name"		: "domain",
		"valid_children": "subdomain",
		"select_node"	: true,
		"open_node"	: function(e) { return getNodeData(e[0]);}
	},
	SubDomainType : {
		"type_name"		: "subdomain",
		"valid_children": "mbean",
		"select_node"	: true,
		"open_node"	: function(e) { return getNodeData(e[0]);}
	},
	MBeanType : {
		"type_name"		: "mbean",
		"valid_children": "attr",
		"select_node"	: true,
		"open_node"	: function(e) { return getNodeData(e[0]);}
	},
	AttributeType : {
		"type_name"		: "attr",
		"valid_children": "subattr",
		"select_node"	: true,
		"open_node"	: function(e) { return getNodeData(e[0]);}		
	},
	SubAttributeType : {
		"type_name"		: "subattr",
		"valid_children": "subattr",
		"select_node"	: true,
		"open_node"	: function(e) { return getNodeData(e[0]);}
	}}
};

function getDataBind(name) {
	var def = deepClone(dataBindings["helios-ui"]);
	var named = deepClone(dataBindings[name]);
	return $.extend(def, named);	
}

// DisplayContainer --> Operation --> JMXType
var dataBindings = {
	// Default data binding attributes
	"helios-ui": {
		// the decorated flag
		"d"			: false,
		// the populated flag
		"p"			: false,
		// the timestamp of the last bind event
		"ts"		: 0,
		// the names of the attributes in the incoming payload to bind as attributes
		"bind-attr"	: []
	},
	// The jmx tree itself
	"helios-ui-jstree-tree" : {
		// the incoming payload type this node accepts
		"types"		: ["root"]
		// there is no bind op since no data is required to initialize this node
	},
	// the tree helios root node
	"helios-ui-jstree-root" : {
		"types"		: ["host"],
		// the bind operation to fire against the helios plugin 
		"bindop"	: {
			// the op type
			"type"		: "exec",
			// the target mbean object name
			"mbean"		: "org.helios.jmx.mbeanservers:service=SearchService",
			// the target operation name
			"operation" : "getSpec",
			// the arguments to pass to the operation
			//  [ X, Y, Z[] ] where X is the type we're looking for, Y is an optional regex filter on X and Z is an array of modifiers in the form a=b. 
			"arguments"	: [ "host", "", []]  // <- retrieves the list of hosts for the helios instance,  that is, we want hosts, with no filter and an empty modifier array.
		}
		// there is no bind-attr here since no actual data is held by the root
	},
	"helios-ui-jstree-host" : {
		"types"		: ["vm"],
		// take these attributes from the incoming payload and bind as an attribute to the node
		"bind-attr"	: ["host"],
		"bindop"	: {
			"type"		: "exec", 
			"mbean"		: "org.helios.jmx.mbeanservers:service=SearchService",
			"operation" : "getSpec",
			"arguments"	: [ "vm", "", ["@host=${host}"]] // <- retrieves the list of vms for the host
			// The syntax is @X where @ indicates a tokenized expression and X is a tokenized expression where values with the format ${Y} are substituted from 
			// a data attribute named Y.			           	    
		}
	},
	"helios-ui-jstree-vm" : {
		"types"		: ["domain"],
		"bind-attr"	: ["host", "vm", "proxy"],
		"bindop"	: {							// <- retrieves the list of domains for the vm
			"type"		: "exec", 
			"mbean"		: "org.helios.jmx.mbeanservers:service=SearchService",
			"operation" : "getSpec",
			"arguments"	: [ "domain", "", ["@host=${host}", "@vm=${vm}"]] 
		}
	},
	"helios-ui-jstree-domain" : {
		"types"		: ["subdomain"],
		"bind-attr"	: ["host", "vm", "domain", "proxy:->.parents('.helios-ui-jstree-vm').data('proxy')"],
		// the syntax X:->Y means the expression Y should be evaluated and the result bound into an attribute X
		"bindop"	: {							// <- retrieves the list of subdomains for the domain
			"type"		: "exec", 
			"mbean"		: "org.helios.jmx.mbeanservers:service=SearchService",
			"operation" : "getSpec",
			"arguments"	: [ "subdomain", "", ["@host=${host}", "@vm=${vm}", "@domain=${domain}"]],  
			"target"	: {"url" : "@${proxy}"}
		}
	},
	"helios-ui-jstree-subdomain" : {
		"types"		: ["mbean"],
		"bind-attr"	: ["subdomain", "proxy:->.parents('.helios-ui-jstree-vm').data('proxy')"],
		"bindop"	: {							// <- retrieves the list of mbeans for the subdomain
			"type"	: "search", 
			"mbean"	: "@${subdomain}:*", 
			"target"	: {"url" : "@${proxy}"}
		}
	},
	"helios-ui-jstree-mbean" : {   // ###  Need to capture if each attr is expected to have subattrs.
		"types"		: ["attr"],
		"bind-attr"	: ["mbean", "proxy:->.parents('.helios-ui-jstree-vm').data('proxy')"],
		"bindop"	: {							// <- retrieves the list of mbeans for the subdomain
			"type"	: "list", 
			"path"	: "@${mbean}",
			"target"	: {"url" : "@${proxy}"}
		}
	},
	"helios-ui-jstree-attr" : {
		"types"		: ["subattr"],
		"bind-attr"	: ["attr", "mbean:->.parents('.helios-ui-jstree-mbean').data('mbean')", "proxy:->.parents('.helios-ui-jstree-vm').data('proxy')"],
		"bindop"	: {							// <- retrieves the list of subattrs for the attr
			"type"	: "list", 
			"path"	: "@${mbean}/${attr}",
			"target"	: {"url" : "@${proxy}"}
		}
	},
	"helios-ui-jstree-subattr" : {
		"types"		: ["subattr"],
		"bind-attr"	: ["subattr", "attr:->.parents('.helios-ui-jstree-attr').data('attr')", "mbean:->.parents('.helios-ui-jstree-mbean').data('mbean')", "proxy:->.parents('.helios-ui-jstree-vm').data('proxy')"],
		"bindop"	: {							// <- retrieves the list of subattrs for the subattr
			"type"	: "list", 
			"path"	: "@${mbean}/${attr}",			// <-- this will not work for now. need to fix.
			"target"	: {"url" : "@${proxy}"}
		}
	}
	

};

// Utilities pending:
//	@${X}   extract tokens from local data segment
//	X:->Y	set the data segment named X to the value of eval(Y)


