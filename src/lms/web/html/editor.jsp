<%@ page language="java" import="java.util.*" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<!DOCTYPE html>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=utf-8"/>
    <meta name="keywords" content="UEditor,Editor,javascript 编辑器,html 编辑器,百度编辑器,百度 Editor, 在线编辑器"/>
    <meta name="description" content="UEditor是百度的一个javascript编辑器的开源项目，能带给您良好的富文本使用体验">
    <title>UEditor - 示例</title>
    <link rel="stylesheet" href="<c:url value="/pub/css/public.css"/>" type="text/css">
    <link rel="stylesheet" href="<c:url value="/pub/css/onlinedemo.css"/>" type="text/css">

    <script type="text/javascript" charset="utf-8">
        window.UEDITOR_HOME_URL = "<c:url value='/pub/ueditor/'/>";
    </script>
    <script type="text/javascript" charset="utf-8" src="<c:url value="/pub/ueditor/ueditor.config.js"/>"></script>
    <script type="text/javascript" charset="utf-8" src="<c:url value="/pub/ueditor/ueditor.all.min.js"/>"></script>
</head>
<body>

<div id="wrapper">

    <div id="content" class="w900 border-style1 bg">
        <div class="section">
            <h3>UEditor - 完整示例</h3>


            <div class="details">
                <div>
                    <script type="text/plain" id="editor"></script>
                    <div class="con-split"></div>
                </div>
            </div>
        </div>
        <div class="section">
            <h4>常用API</h4>

            <div id="allbtn" class="details">
                <div id="btns">
                    <div>
                        <input type="button" value="保存" onclick="UE.getEditor('editor').execCommand( 'autosubmit' )"/>
                    </div>
                </div>
            </div>
        </div>
    </div>
    <div id="footer">
        @ 百度ueditor版权所有
    </div>
</div>

<script src="<c:url value="/html/js/util.js"/>" type="text/javascript"></script>
<script src="<c:url value="/html/js/effect.js"/>" type="text/javascript"></script>
<script src="<c:url value="/html/js/onlinedemo.js"/>" type="text/javascript"></script>
<script type="text/javascript">
var ue = UE.getEditor('editor', options);

ue.ready(function(){
	ue.focus();
})

</script>


</body>
</html>
