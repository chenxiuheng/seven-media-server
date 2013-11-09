package github.chenxh.json;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.struts2.interceptor.ServletRequestAware;
import org.apache.struts2.interceptor.SessionAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opensymphony.xwork2.ActionSupport;

/**
 * json 类型返回  action 
 * @author xiuheng chen  chenxiuheng@gmail.com
 */
public class JsonAction extends ActionSupport implements ServletRequestAware, SessionAware{
    private Variable var;

    private String userName;
    private String sessionId;
    
    private Map<String, Object> session;
    
    final Logger logger = LoggerFactory.getLogger(getClass());

    public JsonAction() {
        logger.warn("新建了对象");
        sessionId = String.valueOf(Math.random());
    }
    @Override
    public String execute() throws Exception {
        logger.debug("{}, {}", sessionId, session);
        
        var = new Variable();
        var.putAll(session);
        
        var.put("sessionId", sessionId);
        
        return super.execute();
    }
    
    public String index() {
        return "index";
    }
    
    
    /**
     * @param json
     * @author chenxh 2013-11-7 下午11:42:48
     */
    public final void setVar(Variable json) {
        this.var = json;
    }

    public void setServletRequest(HttpServletRequest request) {
        sessionId = request.getSession().getId();
    }

    public void setSession(Map<String, Object> session) {
        this.session = session;
    }

    public final Variable getVar() {
        return var;
    }

    public final String getUserName() {
        return userName;
    }

    public final void setUserName(String userName) {
        this.userName = userName;
    }

    public final String getSessionId() {
        return sessionId;
    }

    public final void setSessionId(String sessionId) {
        logger.warn("{},{}", Thread.currentThread().getId(), hashCode());
    }

    public final Map<String, Object> getSession() {
        Map<String, Object> rst = new HashMap<String, Object>(){{
            put("name", "value");
            put("hash", JsonAction.this.hashCode());
        }};
        logger.debug("{}", rst);
        return rst;
    }
}
