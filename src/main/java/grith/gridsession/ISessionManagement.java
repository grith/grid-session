package grith.gridsession;

import java.util.List;
import java.util.Map;

public interface ISessionManagement {

	public String credential_type();

	public abstract void destroy();

	public abstract String dn();

	public String group_proxy_path(String group);

	public List<String> groups();

	public abstract Boolean is_logged_in();

	public boolean is_renewable();

	public abstract Boolean is_uploaded();

	public abstract Boolean is_uploaded(String group);

	public int lifetime();

	public abstract List<String> list_institutions();

	public abstract Boolean login(Map<String, Object> config);

	public abstract void logout();

	public abstract String myproxy_host();

	public String myproxy_password();

	public String myproxy_password(String group);

	public abstract int myproxy_port();

	public String myproxy_username();

	public String myproxy_username(String group);

	public String ping();

	public String proxy_path();

	public boolean refresh();

	public abstract boolean set_min_lifetime(Integer lt);

	public abstract void set_myProxy_host(String myProxyServer);

	public abstract void set_myProxy_port(int port);

	public abstract boolean shutdown();

	public abstract Boolean start(Map<String, Object> config);

	public abstract String status();

	public abstract void stop();

	public boolean upload();

	public boolean upload(String group);

	public boolean upload(String group, String myproxyhost);

	public boolean upload(String group, String myproxyhost,
			String myproxyusername);

	public boolean upload(String group, String myproxyhost,
			String myproxyusername, char[] myproxypassword);

}