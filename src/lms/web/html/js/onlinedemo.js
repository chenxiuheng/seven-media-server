var domUtils = UE.dom.domUtils;
URL = window.UEDITOR_HOME_URL;
var options = {
    //图片上传配置区
        imageUrl:URL+"img.put"             //图片上传提交地址
        ,imagePath:URL + "jsp/"                     //图片修正地址，引用了fixedImagePath,如有特殊需求，可自行配置
        //,imageFieldName:"upfile"                   //图片数据的key,若此处修改，需要在后台对应文件修改对应参数
        //,compressSide:0                            //等比压缩的基准，确定maxImageSideLength参数的参照对象。0为按照最长边，1为按照宽度，2为按照高度
        //,maxImageSideLength:900                    //上传图片最大允许的边长，超过会自动等比缩放,不缩放就设置一个比较大的值，更多设置在image.html中

        //涂鸦图片配置区
        ,scrawlUrl:URL+"jsp/scrawlUp.jsp"           //涂鸦上传地址
        ,scrawlPath:URL+"jsp/"                            //图片修正地址，同imagePath

        //附件上传配置区
        ,fileUrl:URL+"jsp/fileUp.jsp"               //附件上传提交地址
        ,filePath:URL + "jsp/"                   //附件修正地址，同imagePath
        //,fileFieldName:"upfile"                    //附件提交的表单名，若此处修改，需要在后台对应文件修改对应参数

        //远程抓取配置区
        //,catchRemoteImageEnable:true               //是否开启远程图片抓取,默认开启
        ,catcherUrl:URL +"jsp/getRemoteImage.jsp"   //处理远程图片抓取的地址
        ,catcherPath:URL + "jsp/"                  //图片修正地址，同imagePath
        
        //,catchFieldName:"upfile"                   //提交到后台远程图片uri合集，若此处修改，需要在后台对应文件修改对应参数
        ,separater:';'               //提交至后台的远程图片地址字符串分隔符
        //,localDomain:[]                            //本地顶级域名，当开启远程图片抓取时，除此之外的所有其它域名下的图片都将被抓取到本地,默认不抓取127.0.0.1和localhost

        //图片在线管理配置区
        ,imageManagerUrl:URL + "jsp/imageManager.jsp"       //图片在线管理的处理地址
        ,imageManagerPath:URL + "jsp/"                                    //图片修正地址，同imagePath

        //屏幕截图配置区
        ,snapscreenHost: location.hostname                                 //屏幕截图的server端文件所在的网站地址或者ip，请不要加http://
        ,snapscreenServerUrl: URL +"jsp/imageUp.jsp" //屏幕截图的server端保存程序，UEditor的范例代码为“URL +"server/upload/jsp/snapImgUp.jsp"”
        ,snapscreenPath: URL + "jsp/"
        ,snapscreenServerPort: location.port                                   //屏幕截图的server端端口
        //,snapscreenImgAlign: ''                                //截图的图片默认的排版方式

        //word转存配置区
        ,wordImageUrl:URL + "jsp/imageUp.jsp"             //word转存提交地址
        ,wordImagePath:URL + "jsp/"                       //
        //,wordImageFieldName:"upfile"                     //word转存表单名若此处修改，需要在后台对应文件修改对应参数

        //获取视频数据的地址
        ,getMovieUrl:URL+"jsp/getMovie.jsp"                   //视频数据获取地址

        //工具栏上的所有的功能按钮和下拉框，可以在new编辑器的实例时选择自己需要的从新定义
        
        , toolbars: [["fullscreen","source","undo","redo","insertunorderedlist","insertorderedlist","unlink","link","cleardoc","selectall","print","searchreplace","preview","help","|","gmap","pagebreak","insertimage","scrawl","music","snapscreen","emotion","insertvideo","attachment","insertframe","date","time","wordimage","map","webapp","horizontal","anchor","spechars","blockquote","insertcode","template","background","bold","italic","underline","strikethrough","forecolor","backcolor","superscript","subscript","justifyleft","justifycenter","justifyright","justifyjustify","touppercase","tolowercase","directionalityltr","directionalityrtl","indent","removeformat","formatmatch","autotypeset","customstyle","paragraph","rowspacingbottom","rowspacingtop","lineheight","fontfamily","fontsize","imagenone","imageleft","imageright","imagecenter","inserttable","deletetable","mergeright","mergedown","splittorows","splittocols","splittocells","mergecells","insertcol","insertrow","deletecol","deleterow","insertparagraphbeforetable"],[]]
        //当鼠标放在工具栏上时显示的tooltip提示,留空支持自动多语言配置，否则以配置值为准
//        ,labelMap:{
//            'anchor':'', 'undo':''
//        }
        //webAppKey
        //百度应用的APIkey，每个站长必须首先去百度官网注册一个key后方能正常使用app功能
        //,webAppKey:""

        //语言配置项,默认是zh-cn。有需要的话也可以使用如下这样的方式来自动多语言切换，当然，前提条件是lang文件夹下存在对应的语言文件：
        //lang值也可以通过自动获取 (navigator.language||navigator.browserLanguage ||navigator.userLanguage).toLowerCase()
    ,lang:/^zh/.test(navigator.language || navigator.browserLanguage || navigator.userLanguage) ? 'zh-cn' : 'en',
    langPath:UEDITOR_HOME_URL + "lang/",

    webAppKey:"9HrmGf2ul4mlyK8ktO2Ziayd",
    initialFrameWidth:860,
    initialFrameHeight:420,
    focus:true
};

function setLanguage(obj) {
    var value = obj.value,
        opt = {
            lang:value
        };
    UE.utils.extend(opt, options, true);

    UE.delEditor("editor");
    //清空语言
    if( !UE.I18N[ opt.lang ] ) {
        UE.I18N = {};
    }
    UE.getEditor('editor', opt);
}
function createEditor() {
    enableBtn();
    UE.getEditor('editor', {
        initialFrameWidth:"100%"
    })
}
function getAllHtml() {
    alert(UE.getEditor('editor').getAllHtml())
}
function getContent() {
    var arr = [];
    arr.push("使用editor.getContent()方法可以获得编辑器的内容");
    arr.push("内容为：");
    arr.push(UE.getEditor('editor').getContent());
    alert(arr.join("\n"));
}
function getPlainTxt() {
    var arr = [];
    arr.push("使用editor.getPlainTxt()方法可以获得编辑器的带格式的纯文本内容");
    arr.push("内容为：");
    arr.push(UE.getEditor('editor').getPlainTxt());
    alert(arr.join('\n'))
}
function setContent() {
    var arr = [];
    arr.push("使用editor.setContent('欢迎使用ueditor')方法可以设置编辑器的内容");
    UE.getEditor('editor').setContent('欢迎使用ueditor');
    alert(arr.join("\n"));
}
function setDisabled() {
    UE.getEditor('editor').setDisabled('fullscreen');
    disableBtn("enable");
}
function setEnabled() {
    UE.getEditor('editor').setEnabled();
    enableBtn();
}
function getText() {
    //当你点击按钮时编辑区域已经失去了焦点，如果直接用getText将不会得到内容，所以要在选回来，然后取得内容
    var range = UE.getEditor('editor').selection.getRange();
    range.select();
    var txt = UE.getEditor('editor').selection.getText();
    alert(txt)
}
function getContentTxt() {
    var arr = [];
    arr.push("使用editor.getContentTxt()方法可以获得编辑器的纯文本内容");
    arr.push("编辑器的纯文本内容为：");
    arr.push(UE.getEditor('editor').getContentTxt());
    alert(arr.join("\n"));
}
function hasContent() {
    var arr = [];
    arr.push("使用editor.hasContents()方法判断编辑器里是否有内容");
    arr.push("判断结果为：");
    arr.push(UE.getEditor('editor').hasContents());
    alert(arr.join("\n"));
}
function setFocus() {
    UE.getEditor('editor').focus();
}
function deleteEditor() {
    disableBtn();
    UE.getEditor('editor').destroy();
}
function disableBtn(str) {
    var div = document.getElementById('btns');
    var btns = domUtils.getElementsByTagName(div, "input");
    for (var i = 0, btn; btn = btns[i++];) {
        if (btn.id == str) {
            domUtils.removeAttributes(btn, ["disabled"]);
        } else {
            btn.setAttribute("disabled", "true");
        }
    }
}
function enableBtn() {
    var div = document.getElementById('btns');
    var btns = domUtils.getElementsByTagName(div, "input");
    for (var i = 0, btn; btn = btns[i++];) {
        domUtils.removeAttributes(btn, ["disabled"]);
    }
}