package github.chenxh.cms.html;

import java.util.HashMap;
import java.util.Map;

import com.opensymphony.xwork2.ActionSupport;

/**
 * 新闻编辑器
 *
 *<p></p>
 * @author chenxh 
 * @mail  chenxiuheng@gmail.com
 */
public class HtmlEditor extends ActionSupport {
    private String id;

    private String text;
    
    private long timeToken;
    
    
    private Map<String, Boolean> result = new HashMap<String, Boolean>();


    
    /**
     * 打开新的编辑页面
     * 
     * <p></p>
     * @return
     * @author chenxh 2013-11-27
     */
    public String open() {
        return "open";
    }
    
    /**
     * 准备编辑一个新闻
     * 
     * <p></p>
     * @return
     * @author chenxh 2013-11-27
     */
    public String edit() {
        
    }

    /**
     * 更新或者保存一个网页
     * 
     * <p></p>
     * @return
     * @author chenxh 2013-11-27
     */
    public String saveOrUpdate() {
        
    }
    
    /**
     * 预览一个网页
     * 
     * <p></p>
     * @return
     * @author chenxh 2013-11-27
     */
    public String preview() {
        
    }
}
