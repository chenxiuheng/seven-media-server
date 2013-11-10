package github.chenxh.demo;

import org.slf4j.LoggerFactory;

import com.opensymphony.xwork2.ActionSupport;

public class LoginAction extends ActionSupport {
    /**
     * serialVersionUID
     */
    private static final long serialVersionUID = 1L;

    private String name;
    
    org.slf4j.Logger logger = LoggerFactory.getLogger(LoginAction.class);
    
    @Override
    public String execute() throws Exception {
        System.out.println(name);
        return super.execute();
    }

    /**
     * setName
     * @see {@link #name name} 
     **/
    public final void setName(String name) {
        this.name = name;
    }
}
