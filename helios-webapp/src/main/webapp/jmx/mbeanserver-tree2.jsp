<html>
	<head>
		<link rel="stylesheet" href="jquery/tree/jquery.treeview.css" />
		<link rel="stylesheet" href="jquery/tree/screen.css" />
		<link rel="stylesheet" href="jquery/jstree/themes/apple/style.css" />
		<link type="text/css" href="jquery/css/sunny/jquery-ui-1.8.10.custom.css" rel="stylesheet" />	
		<script type="text/javascript" src="jquery/js/jquery-1.4.4.min.js"></script>
		<script type="text/javascript" src="jquery/js/jquery-ui-1.8.9.custom.min.js"></script>
		
		<script src="jquery/tree/lib/jquery.cookie.js" type="text/javascript"></script>
		<script src="jquery/tree/jquery.treeview.js" type="text/javascript"></script>
		<script src="jquery/jstree/jquery.jstree.js" type="text/javascript"></script>
		<script type="text/javascript">
			$(function(){
				console.info("Initializing MBeanServerTree");
				$("#mbeanserver-tree").treeview({collapsed:true});				
				var mbeans = jolokia.search("org.helios.jmx.mbeanservers:*");
				for(mbean in mbeans) {
					console.debug("Reading MBean [%s]", mbeans[mbean]);
					var resp = jolokia.request({ type: "read", mbean: mbeans[mbean] });
					console.debug(resp.value);
					var server = resp.value;
					console.debug("Server: Host:%s, VMId:%s, Domain:%s", server.HostId, server.VMId, server.DefaultDomain);
					var nextParent = popServer(server, "HostId", "#helios-root");
					nextParent = popServer(server, "VMId", nextParent);
					nextParent = popServer(server, "DefaultDomain", nextParent);
				}
			});
			function popServer(server, type, parent, index) {
				var key = index==null ? server[type] : server[type][index];
				var id = type + "-" + key;
				var sel = "#" + id; 
				if($(id).length==0) {
					console.debug("Adding [%s]", id);
					var folder = genFolder(server, type, parent, index);
					var branches = $(folder).appendTo(parent);
					console.debug("Branch Code Appended to [%s] - [%s]", parent, folder);
					$("#mbeanserver-tree").treeview({add: branches});
				}
				return sel;
			}
			function genFolder(server, type, parent, index) {
				var key = index==null ? server[type] : server[type][index];
				var id = type + "-" + key;
				var sel = "#" + id; 
				return "<li><span class='folder'>" + key + "</span><ul id='" +  id +  "'>" + "</ul></li>"
			}
			
		</script>
	
	
	</head>
	<body>
		<h3>Available MBeanServers</h3>
		<ul id="mbeanserver-tree" class="jmxtree">
			<ul id='helios-root'>Helios</ul>
		</ul>
	</body>
</html>