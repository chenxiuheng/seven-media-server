package github.chenxh.common.file;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * description
 * 
 *<p>
 * </p>
 * 
 * @author chenxh
 * @mail chenxiuheng@gmail.com
 */
public class FileUploaderServlet extends HttpServlet {

    /**   */
    private static final long serialVersionUID = 1L;

    private Map<String, String> errorInfo = new HashMap<String, String>();

    @Override
    public void init() throws ServletException {
        Map<String, String> tmp = errorInfo;
        tmp.put("SUCCESS", "SUCCESS"); // 默认成功
        tmp.put("NOFILE", "未包含文件上传域");
        tmp.put("TYPE", "不允许的文件格式");
        tmp.put("SIZE", "文件大小超出限制");
        tmp.put("ENTYPE", "请求类型ENTYPE错误");
        tmp.put("REQUEST", "上传请求异常");
        tmp.put("IO", "IO异常");
        tmp.put("DIR", "目录创建失败");
        tmp.put("UNKNOWN", "未知错误");
        
        // 允许上传的文件
        String allow = getServletConfig().getInitParameter("allow");
        if (null != allow && allow.length() > 0) {
            fileType = allow.replaceAll("[\\s]+", "").split(",");
        }
        logger.warn("allowed file type {}", fileType);
        
        // 文件保存路径
        String path = getServletConfig().getInitParameter("path");
        if (null != path && path.length() > 0) {
            this.path = path;
        }
    }
    private Logger logger = LoggerFactory.getLogger(getClass());
    private String[] fileType = {".rar", ".doc", ".docx", ".zip", ".pdf", ".txt",
            ".swf", ".wmv"}; // 允许的文件类型
    private String path = "upload";
    
    @Override
    protected void doPost(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding("utf-8");
        response.setCharacterEncoding("utf-8");

        Uploader up = new Uploader(request);
        up.setSavePath(path); // 保存路径
        
        up.setAllowFiles(fileType);
        up.setMaxSize(10000); // 允许的文件最大尺寸，单位KB
        up.upload();
        
        response.getWriter().print(
                "{'url':'" + up.getUrl() + "','fileType':'" + up.getType()
                        + "','state':'" + up.getState() + "','original':'"
                        + up.getOriginalName() + "'}");

    }
}
