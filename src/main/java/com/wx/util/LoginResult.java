package com.wx.util;

import org.apache.commons.httpclient.Cookie;

public class LoginResult {

	private boolean isLogin;
	private String cookiestr;
	private String token;
	private Cookie[] cookie;
	private String loginErrCode;
	public boolean isLogin() {
		return isLogin;
	}
	public void setLogin(boolean isLogin) {
		this.isLogin = isLogin;
	}
	public String getCookiestr() {
		return cookiestr;
	}
	public void setCookiestr(String cookiestr) {
		this.cookiestr = cookiestr;
	}
	public String getToken() {
		return token;
	}
	public void setToken(String token) {
		this.token = token;
	}
	public Cookie[] getCookie() {
		return cookie;
	}
	public void setCookie(Cookie[] cookie) {
		this.cookie = cookie;
	}
	public String getLoginErrCode() {
		return loginErrCode;
	}
	public void setLoginErrCode(String loginErrCode) {
		this.loginErrCode = loginErrCode;
	}
	
	
}
