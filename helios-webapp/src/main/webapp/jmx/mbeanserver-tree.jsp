<html>
	<head>		
		<link type="text/css" href="jquery/css/sunny/jquery-ui-1.8.10.custom.css" rel="stylesheet" />	
		
		<!-- <script type="text/javascript" src="jquery/js/jquery-1.5.1.min.js"></script> -->
		<!--  <script type="text/javascript" src="jquery/js/jquery-1.4.4.min.js"></script> -->
		<script type="text/javascript" src="jquery/js/jquery-ui-1.8.9.custom.min.js"></script>
		<script type="text/javascript" src="jquery/js/jquery.dialogextend.pack.js"></script>
		
		
				
		<script src="jquery/jstree/lib/jquery.cookie.js" type="text/javascript"></script>
		<script src="jquery/jstree/lib/jquery.hotkeys.js" type="text/javascript"></script>
		<script src="jquery/jstree/jquery.jstree.js" type="text/javascript"></script>		
		
		<script src="heliosjs/helios.js" type="text/javascript"></script>
		<style type="text/css">
			.scroll-tree {
		    	border: 0px;
		    	float: none;
		    	height: 80%;
		    	overflow: auto;
		    	width: 20%;
		    }
		    #tabsx {
		    	background: url("img/Quad_paper_0.png") repeat-x scroll 100% top #FEEEBD;
		    }
			.contentBox {
		        display: inherit;
		        border-width: 0px;
		        border-style: dotted;
		        border-color: 000;
		        padding:0px;
		        margin-top:0px;
		        width:100%;
		        height:100%;
		        overflow:auto;
	        }
			.resizeBox {
		        display:inherit;
		        border-width: 0px;
		        padding:1px;
		        margin-top:0px;
		        width:200px;
		        height:10%;
	        }
	        .tabsContainer {
	        	height:90%;
	        	background: url("img/Swipe.jpg") 100% #FEEEBD;
	        	background-repeat:no-repeat;
	        	background-size: 100%;	        	
	        }
	        .contentContainer {
		    	border: 2px red double;
		    	float: none;
		    	height: 100%;
		    	overflow: auto;
		    	width: 100%;	        	
	        }
		    
		</style>			
		<!--
		
		-->
	</head>
	<body>
	
	
		<!-- class="containerPlus draggable resizable {buttons:'m,c,i', icon:'browser.png', skin:'white', width:'500',iconized:'true', dock:'dock', grid:100, rememberMe:true, title:'Available MBeanServers'}" style="top:100px;left:400px" -->
		
			<div id="mbeanserverContainerX" class="tabsContainer">
						<!-- <table><tr><td><img id="mbeanserver-tree-control" src="img/collapse.gif" style="position: relative; top: 0; left 0;"></td><td><h4 style="margin-bottom: 0px; margin-top: 0px">Available MBeanServers</h4></td></tr></table>  -->					
						<div id="mbeanserver-tree" class="helios-jstree-tree"></div>
						<div id="mbeanserver-tree-open" class="ui-dialog-titlebar ui-widget-header ui-corner-all ui-helper-clearfix" style="width: 20%;">
							<span class="ui-dialog-title" id="ui-dialog-title-mbeanserver-tree">Open AvailableMBeanServers</span>
						</div>
			</div>
			
		
	</body>
</html>

<!--
						<a href="#" class="ui-dialog-titlebar-close ui-corner-all" role="button">
							<span class="ui-icon ui-icon-closethick">close</span>
						</a>
						<a href="#" class="ui-dialog-titlebar-maximize ui-corner-all" role="button" style="display: none; right: -9999em;">
							<span class="ui-icon ui-icon-extlink">maximize</span>
						</a>
						<a href="#"	class="ui-dialog-titlebar-minimize ui-corner-all" role="button"	style="right: 1.4em;">
							<span class="ui-icon ui-icon-minus">minimize</span>
						</a>
						<a href="#" class="ui-dialog-titlebar-restore ui-corner-all" role="button"	style="display: none; right: -9999em;">
							<span class="ui-icon ui-icon-newwin">restore</span>
						</a>
-->