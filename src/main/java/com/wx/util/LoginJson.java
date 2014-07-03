package com.wx.util;

/**
 * 登录返回信息Json对象
 *
 * @author Kone
 */
public class LoginJson {

    private Base_resp base_resp;
    
    private String redirect_url;
    
    /**
     * 获取base_resp
     * @return base_resp base_resp
     */
    public Base_resp getBase_resp() {
        return base_resp;
    }


    /**
     * 设置base_resp
     * @param base_resp base_resp
     */
    public void setBase_resp(Base_resp base_resp) {
        this.base_resp = base_resp;
    }


    /**
     * 获取redirect_url
     * @return redirect_url redirect_url
     */
    public String getRedirect_url() {
        return redirect_url;
    }


    /**
     * 设置redirect_url
     * @param redirect_url redirect_url
     */
    public void setRedirect_url(String redirect_url) {
        this.redirect_url = redirect_url;
    }


    public class Base_resp{
        
        public int ret;
        
        public String err_msg;

        /**
         * 获取ret
         * @return ret ret
         */
        public int getRet() {
            return ret;
        }

        /**
         * 设置ret
         * @param ret ret
         */
        public void setRet(int ret) {
            this.ret = ret;
        }

        /**
         * 获取err_msg
         * @return err_msg err_msg
         */
        public String getErr_msg() {
            return err_msg;
        }

        /**
         * 设置err_msg
         * @param err_msg err_msg
         */
        public void setErr_msg(String err_msg) {
            this.err_msg = err_msg;
        }
        
    }
}
