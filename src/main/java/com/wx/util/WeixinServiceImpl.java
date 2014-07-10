package com.wx.util;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

public class WeixinServiceImpl {
	private final static Log log = LogFactory.getLog(WeixinServiceImpl.class);
	public final static String HOST = "http://mp.weixin.qq.com";
	public final static String LOGIN_URL = "https://mp.weixin.qq.com/cgi-bin/login?lang=zh_CN";
	public final static String INDEX_URL = "http://mp.weixin.qq.com/cgi-bin/indexpage?t=wxm-index&lang=zh_CN";
	public final static String SENDMSG_URL = "https://mp.weixin.qq.com/cgi-bin/singlesend";
	public final static String FANS_URL = "http://mp.weixin.qq.com/cgi-bin/contactmanagepage?t=wxm-friend&lang=zh_CN&pagesize=10&pageidx=0&type=0&groupid=0";
	public final static String LOGOUT_URL = "http://mp.weixin.qq.com/cgi-bin/logout?t=wxm-logout&lang=zh_CN";
	
	public final static String USER_AGENT_H = "User-Agent";
	public final static String REFERER_H = "Referer";
	public final static String USER_AGENT = "Mozilla/5.0 (Windows NT 6.2; WOW64) AppleWebKit/537.22 (KHTML, like Gecko) Chrome/25.0.1364.172 Safari/537.22";
	public final static String UTF_8 = "UTF-8";
	private static Map<String, String> cookiemap = new LinkedHashMap<String, String>();
	private final static String WEIXIN_URL = "/weixin/wxNotify.htm";
	private final static String WEIXIN_TOKEN = "27057a36f7e711e2841d00163e122bbb";
	private HttpClient client = new HttpClient();
	
	public WeixinServiceImpl() {}

	/**
	 * 登录,登录失败会重复请求登录
	 */
	public LoginResult login(String username, String pwd) {
		LoginResult result = _login(username, pwd);
		int i = 0;
		while (result!=null&&!result.isLogin()) {
			result = _login(username, pwd);
			i++;
			if(i > 3){
				return result;
			}
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				result = _login(username, pwd);
			}
		}
		System.out.println("登陆成功");
		return result;
	}

	/**
	 * 发送登录信息,记录cookie，登录状态，token等信息
	 * 
	 * @return
	 */
	private LoginResult _login(String username, String pwd) {
		LoginResult loginResult = new LoginResult();
		try {
			PostMethod post = new PostMethod(LOGIN_URL);
			post.setRequestHeader("Referer", "https://mp.weixin.qq.com/");
			post.setRequestHeader(USER_AGENT_H, USER_AGENT);
			NameValuePair[] params = new NameValuePair[] {
					new NameValuePair("username", username),
					new NameValuePair("pwd", DigestUtils.md5Hex(pwd
							.getBytes())), new NameValuePair("f", "json"),
					new NameValuePair("imagecode", "") };
			post.setQueryString(params);
			int status = client.executeMethod(post);
			if (status == HttpStatus.SC_OK) {
				String ret = post.getResponseBodyAsString();
				LoginJson retcode = JSON.parseObject(ret, LoginJson.class);
				// System.out.println(retcode.getRet());
				if ((retcode.getBase_resp().getRet() == 302 || retcode
						.getBase_resp().getRet() == 0)) {
					loginResult.setCookie(client.getState().getCookies());
					StringBuffer cookie = new StringBuffer();
					for (Cookie c : client.getState().getCookies()) {
						cookie.append(c.getName()).append("=")
								.append(c.getValue()).append(";");
						cookiemap.put(c.getName(), c.getValue());
					}
					loginResult.setCookiestr(cookie.toString());
					loginResult.setToken(getToken(retcode.getRedirect_url()));
					loginResult.setLogin(true);
					return loginResult;
				}else{
					loginResult.setLogin(false);
					return loginResult;
				}
			}
		} catch (Exception e) {
			String info = "【登录失败】【发生异常：" + e.getMessage() + "】";
			System.err.println(info);
			log.debug(info);
			log.info(info);
			return loginResult;
		}
		return loginResult;
	}

	/**
	 * 从登录成功的信息中分离出token信息
	 * 
	 * @param s
	 * @return
	 */
	private String getToken(String s) {
		try {
			if (StringUtils.isBlank(s))
				return null;
			String[] ss = StringUtils.split(s, "?");
			String[] params = null;
			if (ss.length == 2) {
				if (!StringUtils.isBlank(ss[1]))
					params = StringUtils.split(ss[1], "&");
			} else if (ss.length == 1) {
				if (!StringUtils.isBlank(ss[0]) && ss[0].indexOf("&") != -1)
					params = StringUtils.split(ss[0], "&");
			} else {
				return null;
			}
			for (String param : params) {
				if (StringUtils.isBlank(param))
					continue;
				String[] p = StringUtils.split(param, "=");
				if (null != p && p.length == 2
						&& StringUtils.equalsIgnoreCase(p[0], "token"))
					return p[1];

			}
		} catch (Exception e) {
			String info = "【解析Token失败】发生异常" + e.getMessage();
			System.err.println(info);
			log.debug(info);
			log.info(info);
			return null;
		}
		return null;
	}

	/**
	 * 关闭编辑模式
	 * @param loginResult
	 * @return
	 * @throws IOException
	 */
	private int closeEditModel(LoginResult loginResult) throws IOException{
		return changeModelPost("1", "0", loginResult);
	}
	
	/**
	 * 开启开发模式
	 * @param loginResult
	 * @return
	 */
	private int openDevelopModel(LoginResult loginResult){
		return changeModelPost("2", "1", loginResult);
	}
	
//	private int canDev(LoginResult loginResult){
//		return changeModelPost("3", "0", loginResult);
//	}

	
	/**
	 * 
	 * @param url
	 * 
	 */
	private int changeModelPost(String type, String flag, LoginResult loginResult){
		try {
			DefaultHttpClient httpClient = new DefaultHttpClient(); // 创建默认的httpClient实例
			HttpPost post = new HttpPost("http://mp.weixin.qq.com/misc/skeyform?form=advancedswitchform&lang=zh_CN");
	        post.setHeader(USER_AGENT_H, USER_AGENT);
	        post.setHeader(REFERER_H, "https://mp.weixin.qq.com/advanced/advanced?action=edit&t=advanced/edit&lang=zh_CN&token="+loginResult.getToken());
	        post.setHeader("Cookie", loginResult.getCookiestr());
	        post.setHeader("Accept", "application/json, text/javascript, */*; q=0.01");
	        post.setHeader("Accept-Encoding", "gzip, deflate");
	        post.setHeader("Accept-Language", "zh-cn,zh;q=0.8,en-us;q=0.5,en;q=0.3");
	        post.setHeader("Cache-Control", "no-cache");
	        post.setHeader("Connection", "keep-alive");
	        post.setHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
	        post.setHeader("Host", "mp.weixin.qq.com");
	        post.setHeader("Pragma", "no-cache");
	        post.setHeader("X-Requested-With", "XMLHttpRequest");
	        List<BasicNameValuePair> formParams = new ArrayList<BasicNameValuePair>(); // 构建POST请求的表单参数
	        formParams.add(new BasicNameValuePair("flag", flag));//关闭 ：0 开启： 1
	        formParams.add(new BasicNameValuePair("type", type));//编辑模式：1 开发模式：2
	        formParams.add(new BasicNameValuePair("token", loginResult.getToken()));
	        post.setEntity(new UrlEncodedFormEntity(formParams, "UTF-8"));
	        HttpResponse response = httpClient.execute(post); // 执行POST请求
	        // HttpEntity entity = response.getEntity(); //获取响应实体
	        HttpEntity entity = response.getEntity(); // 获取响应实体
	        System.out.println(EntityUtils.toString(entity, "UTF-8"));
			
		} catch (Exception e) {
			return -1;
		}
		
        return 1;
	}
	
	private int changeUrlAndTokenPost(String newUrl, String newToken, LoginResult loginResult) throws NoSuchAlgorithmException, KeyManagementException, IOException{
    	String postUrl = "http://mp.weixin.qq.com/advanced/callbackprofile?t=ajax-response&lang=zh_CN&token="+loginResult.getToken();
    	DefaultHttpClient httpClient = new DefaultHttpClient(); // 创建默认的httpClient实例
        SSLContext ctx = SSLContext.getInstance("TLS");
        X509TrustManager xtm = new X509TrustManager() { // 创建TrustManager
            public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            }
            public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            }
            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }
        };
        // 使用TrustManager来初始化该上下文，TrustManager只是被SSL的Socket所使用
        ctx.init(null, new TrustManager[] { xtm }, null);
        // 创建SSLSocketFactory
        SSLSocketFactory socketFactory = new SSLSocketFactory(ctx);
        // 通过SchemeRegistry将SSLSocketFactory注册到我们的HttpClient上
        httpClient.getConnectionManager().getSchemeRegistry().register(new Scheme("https", 443, socketFactory));
        HttpPost post = new HttpPost(postUrl);
        post.setHeader(USER_AGENT_H, USER_AGENT);
        post.setHeader(REFERER_H, "http://mp.weixin.qq.com/advanced/advanced?action=interface&t=advanced/interface&lang=zh_CN&token="+loginResult.getToken());
        post.setHeader("Cookie", loginResult.getCookiestr());
        post.setHeader("Accept", "application/json, text/javascript, */*; q=0.01");
        post.setHeader("Accept-Encoding", "gzip, deflate");
        post.setHeader("Accept-Language", "zh-cn,zh;q=0.8,en-us;q=0.5,en;q=0.3");
        post.setHeader("Cache-Control", "no-cache");
        post.setHeader("Connection", "keep-alive");
        // post.setHeader("Content-Length", "130");
        post.setHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        post.setHeader("Host", "mp.weixin.qq.com");
        post.setHeader("Pragma", "no-cache");
        post.setHeader("X-Requested-With", "XMLHttpRequest");
        // NameValuePair[] params = null;
        List<BasicNameValuePair> formParams = new ArrayList<BasicNameValuePair>(); // 构建POST请求的表单参数
        formParams.add(new BasicNameValuePair("url", newUrl));
        formParams.add(new BasicNameValuePair("callback_token", newToken));
        post.setEntity(new UrlEncodedFormEntity(formParams, "UTF-8"));
        HttpResponse response = httpClient.execute(post); // 执行POST请求
        // HttpEntity entity = response.getEntity(); //获取响应实体
        HttpEntity entity = response.getEntity(); // 获取响应实体
        String responseContent = null; // 响应内容
        if (null != entity) {
            responseContent = EntityUtils.toString(entity, "UTF-8");
            EntityUtils.consume(entity); // Consume response content
        }else{
        	log.error(postUrl+ "  return null");
        	return -1;
        }
        JSONObject json = JSONObject.parseObject(responseContent);
        String ret = json.getString("ret");
        if("0".equals(ret)){
        	return 1;
        }
        // url 没有保存，测试不通过
        if("-202".equals(ret)){
        	log.error("change url and token return : "+ json);
        	return -1;
        }
        return -1;
	}
	
	/**
	 * 修改url token
	 * 
	 * @username 公众号平台登录账号
	 * @pwd 密码
	 * @param newUrl 
	 * @param newToken
	 * @param mid 商家ID
	 * 
	 * @return 1 成功 -1 错误或异常 -2账号密码错误 -3不是开发者
	 */
	public int changeUrlToken(String username, String pwd, String mid) {
			
        try {
        	LoginResult loginResult = login(username,pwd);
        	String paramStr = "?action=dev&t=advanced/dev&token=" + loginResult.getToken() + "&lang=zh_CN";
            if (loginResult!=null&&loginResult.isLogin()) {
                GetMethod get = new GetMethod("http://mp.weixin.qq.com/advanced/advanced" + paramStr);
                get.setRequestHeader(REFERER_H , "http://mp.weixin.qq.com/cgi-bin/appmsg?begin=0&count=10&t=media/appmsg_list&type=10&action=list&token" );
                get.setRequestHeader("Cookie" , loginResult.getCookiestr());
                int status = client.executeMethod (get);
                if (status == HttpStatus.SC_OK) {
                	String text = get.getResponseBodyAsString();
                	//System.out.println(text);
                	
                	// 已经开启编辑模式
                	if(text.contains("editOpen:\"1\"===\"1\"")){
                		closeEditModel(loginResult);
                	}
                	
                	// 不是开发者
                	if(text.contains("canDev:\"0\"===\"1\"") || text.contains("dev :'0'")){
                		log.error("is not a develop on weixin");
                		return -3;
                	}

                	// 没有开启开发模式
                	if(text.contains("open:\"0\"===\"1\"")){
                		openDevelopModel(loginResult);
                	}
            		//System.out.println(text);
            		int urlStartIndex = text.indexOf("{name:\"URL\",value:\"")+19;
                	int urlEndIndex = text.indexOf("\"", urlStartIndex);
                	String oldUrl = text.substring(urlStartIndex, urlEndIndex);
                	int tokenStartIndex = text.indexOf("{name:\"Token\",value:\"", urlEndIndex)+21;
                	int tokenEndIndex = text.indexOf("\"", tokenStartIndex);
                	String oldToken = text.substring(tokenStartIndex, tokenEndIndex);
                	int openNameStartIndex = text.indexOf("nick_name:")+11;
	            	int openNameEndIndex = text.indexOf("\"", openNameStartIndex);
	            	String openName = text.substring(openNameStartIndex, openNameEndIndex);
                	
                	System.out.println("oldUrl:"+oldUrl + "oldToken:"+oldToken);
                	
                	changeUrlAndTokenPost(WEIXIN_URL, WEIXIN_TOKEN,loginResult);
                }
                return 1;
            }else{
            	return -2;
            }
        } catch (Exception e) {
        	log.error("there is a error when change url and token " + e.getStackTrace());
            return -1;
        }
	}
	
	/**
	 * 获取公众号名称
	 * @param username
	 * @param pwd
	 * @return
	 */
	public String getOpenName(String username, String pwd){
		LoginResult loginResult = login(username, pwd);
		String openName = null;
	    try {
	    	String paramStr = "?action=dev&t=advanced/dev&token=" + loginResult.getToken() + "&lang=zh_CN";
	        if (loginResult.isLogin()) {
	            GetMethod get = new GetMethod("http://mp.weixin.qq.com/advanced/advanced" + paramStr);
	            get.setRequestHeader(REFERER_H , "http://mp.weixin.qq.com/cgi-bin/appmsg?begin=0&count=10&t=media/appmsg_list&type=10&action=list&token" );
	            get.setRequestHeader("Cookie" , loginResult.getCookiestr());
	            int status = client .executeMethod (get);
	            if (status == HttpStatus.SC_OK) {
	            	String text = get.getResponseBodyAsString();
                	// 不是开发者
                	if(text.contains("canDev:\"0\"===\"1\"") || text.contains("dev :'0'")){
                		log.error("is not a develop on weixin");
                		return null;
                	}
	            	if(text.contains("nick_name:")){
	            		int openNameStartIndex = text.indexOf("nick_name:")+11;
		            	int openNameEndIndex = text.indexOf("\"", openNameStartIndex);
		            	openName = text.substring(openNameStartIndex, openNameEndIndex);
		            	System.out.println(openName);
	            	}else{
	            		return null;
	            	}
	            }
	        }
	    } catch (Exception e) {
	    	log.error("there is a error when change url and token " + e.getStackTrace());
	        return null;
	    }
	    return openName;
	}
	
	
	/**
	 * 
	 * @param args
	 * @throws NoSuchAlgorithmException 
	 * @throws KeyManagementException 
	 */
	public static void main(String[] args) throws IOException, KeyManagementException, NoSuchAlgorithmException {

		String LOGIN_USER = ""; //填写正确的账号密码
		String LOGIN_PWD = "";
		WeixinServiceImpl wx = new WeixinServiceImpl();
		//wx.login();
		//wx.getCookiestr();
		//wx.changeUrlAndTokenPost("http://wifi.witown.com/weixin/wxNotify.htm?mid=c6b80fc1f2e911e3aa5790b11c06b333","27057a36f7e711e2841d00163e122bbb");
		//wx.closeEditModel();
		//wx.changeUrlToken(LOGIN_USER,LOGIN_PWD,"http://wifi.witown.com/weixin/wxNotify.htm?mid=c6b80fc1f2e911e3aa5790b11c06b333","27057a36f7e711e2841d00163e122bbb","");
		wx.getOpenName(LOGIN_USER, LOGIN_PWD);
		
	}
}
