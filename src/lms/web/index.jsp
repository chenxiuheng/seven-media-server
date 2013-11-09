<%@ page language="java" import="java.util.*" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%
String path = request.getContextPath();
String basePath = request.getScheme()+"://"+request.getServerName()+":"+request.getServerPort()+path+"/";
%>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<html>
  <head>
    <base href="<%=basePath%>">
    
    <title>My JSP 'index.jsp' starting page</title>
	<meta http-equiv="pragma" content="no-cache">
	<meta http-equiv="cache-control" content="no-cache">
	<meta http-equiv="expires" content="0">    
	<meta http-equiv="keywords" content="keyword1,keyword2,keyword3">
	<meta http-equiv="description" content="This is my page">
	<!--
	<link rel="stylesheet" type="text/css" href="styles.css">
	-->
	
	<script type="text/javascript" src="<c:url value="/pub/js/jquery-1.10.2.min.js"/>"></script>
    <script type="text/javascript" src="<c:url value="/dwr/interface/validator.js"/>"></script>
    <script type="text/javascript" src="<c:url value="/dwr/interface/springJsonAction.js"/>"></script>
    <script type="text/javascript"  src="<c:url value="/dwr/engine.js"/>"></script>
    <script type="text/javascript"  src="<c:url value="/dwr/util.js"/>"></script>
  </head>
  
  <body>
  <script type="text/javascript">
    springJsonAction.getSession(function(){
    	alert(arguments[0]);
    });
    

  </script>
    This is my JSP page. <br>
  </body>
</html>
