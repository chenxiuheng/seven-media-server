package github.chenxh.elearning.demo;

import org.slf4j.LoggerFactory;

import com.opensymphony.xwork2.ActionSupport;

/**
 * @author xiuheng chen  chenxiuheng@gmail.com
 *
 */
public class LoginAction extends ActionSupport {
    private String user;
    
    private String pass;
    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    private org.slf4j.Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public String execute(){
        logger.debug("{} login with [{}]", user, pass);
        return SUCCESS;
    }

    public final void setUser(String user) {
        this.user = user;
    }

    public final void setPass(String pass) {
        this.pass = pass;
    }
}
