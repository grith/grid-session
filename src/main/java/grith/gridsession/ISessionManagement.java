package grith.gridsession;

import java.util.List;
import java.util.Map;

public interface ISessionManagement {

	public abstract List<String> getIdPs();

	public int getRemainingLifetime();

	public abstract Boolean isLoggedIn();

	public abstract Boolean login(Map<String, Object> config);

	public boolean refresh();

	public abstract boolean shutdown();

	public abstract String status();

}