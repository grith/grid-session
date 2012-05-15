package grith.gridsession;

import java.util.List;
import java.util.Map;

public interface ISessionManagement {

	public Boolean is_auto_renew();

	public abstract Boolean is_logged_in();

	public int lifetime();

	public abstract List<String> list_institutions();

	public String local_proxy_path();

	public abstract Boolean login(Map<String, Object> config);

	public abstract void logout();

	public String myproxy_password();

	public String myproxy_username();

	public String ping();

	public boolean refresh();

	public boolean set_min_autorefresh(Integer seconds);

	public abstract boolean set_min_lifetime(Integer lt);

	public abstract boolean shutdown();

	public abstract Boolean start(Map<String, Object> config);

	public abstract String status();

	public abstract void stop();

	public boolean upload();

}