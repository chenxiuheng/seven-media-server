/*
 * 加载
 * */
var loadObj = AJAX_MONITOR.PDC.createInstance(1);
loadObj.start_event();

var ue = UE.getEditor('editor', options);

ue.addListener("ready", function () {
    loadObj.mark("ready");
});

ue.addListener("afteruiready", function () {
    loadObj.mark("ui");
    loadObj.ready();
});


/*
 * 粘贴
 * */
var pasteObj = AJAX_MONITOR.PDC.createInstance(2);

ue.addListener("beforepaste", function () {
    pasteObj.start_event();
});
ue.addListener("afterpaste", function (type, html) {
    if (html.length > 0 && html.length < 1000) {
        pasteObj.mark("p1000");
    } else if (html.length > 1000 && html.length < 10000) {
        pasteObj.mark("p10000");
    } else if (html.length > 10000 && html.length < 100000) {
        pasteObj.mark("p100000");
    }else{
        pasteObj.mark("ptop");
    }

    pasteObj.ready();
});
