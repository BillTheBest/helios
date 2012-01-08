<html>
	<head>
		<title>Helios WebConsole</title>
		<link type="text/css" href="jquery/css/sunny/jquery-ui-1.8.10.custom.css" rel="stylesheet" />	
		<script type="text/javascript" src="jquery/js/jquery-1.5.1.min.js"></script>
		<!--  <script type="text/javascript" src="jquery/js/jquery-1.4.4.min.js"></script> -->
		<script type="text/javascript" src="jquery/js/jquery-ui-1.8.9.custom.min.js"></script>
		<script type="text/javascript" src="jquery/jstree/jquery.jstree.js"></script>
		<script type="text/javascript" src="jolokia/jolokia.js"></script>
		<script type="text/javascript" src="jolokia/jolokia-simple.js"></script>
		<script type="text/javascript" src="jquery/js/jquery.dialogextend.pack.js"></script>			
		<script src="heliosjs/helios-types.js" type="text/javascript"></script>
		<script src="heliosjs/helios.js" type="text/javascript"></script>
		<script src="heliosjs/heliosp.js" type="text/javascript"></script>
		<script src="heliosjs/jquery.helios-util.js" type="text/javascript"></script>
		<script src="heliosjs/jQuery.helios-util.js" type="text/javascript"></script>
		
		
		
		<script type="text/javascript">
			$(function(){
				$('#tabs').tabs({'cache':false});
				//$('#tabs').css(width : "100%");
			});
			var jolokia = new Jolokia({url: "/jolokia/"});
		</script>
		
		<style type="text/css">
			/*demo page css*/
			body{ font: 62.5% "Trebuchet MS", sans-serif; margin: 50px;}
			.demoHeaders { margin-top: 2em; }
			#dialog_link {padding: .4em 1em .4em 20px;text-decoration: none;position: relative;}
			#dialog_link span.ui-icon {margin: 0 5px 0 0;position: absolute;left: .2em;top: 50%;margin-top: -8px;}
			ul#icons {margin: 0; padding: 0;}
			ul#icons li {margin: 2px; position: relative; padding: 4px 0; cursor: pointer; float: left;  list-style: none;}
			ul#icons span.ui-icon {float: left; margin: 0 4px;}
			.console-base {
		    	float: none;
		    	height: 95%;
		    	width: 95%;
		    }		
			.tabs-base {
		    	float: none;
		    	height: 90%;
		    	width: 90%;
		    }		
			
		</style>			
	</head>
<body class="console-base">
	<table><tr><td valign="middle" ><img src="img/Helios_Symbol_30_45.png"></img></td><td valign="middle"><h3>Helios WebConsole</h3></td></tr></table>
	<div id="tabs" class="tabs-base">
		<ul>
			<li><a href="jmx/mbeanserver-tree.jsp">MBeanServers</a></li>
			<li><a href="jmx/mbeanserver-tree2.jsp">ServerStatus</a></li>
			<li><a href="#dashboards">Dashboards</a></li>
		</ul>
		<div id="serverstatus"></div>
		<div id="dashboards"></div>
	</div>	

</body>
</html>
