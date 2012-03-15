package grith.gridsession;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.XmlRpcRequest;
import org.apache.xmlrpc.common.XmlRpcHttpRequestConfig;
import org.apache.xmlrpc.server.AbstractReflectiveHandlerMapping;
import org.apache.xmlrpc.server.PropertyHandlerMapping;
import org.apache.xmlrpc.server.XmlRpcHandlerMapping;
import org.apache.xmlrpc.webserver.XmlRpcServlet;

public class GrithXmlRpcServlet extends XmlRpcServlet {
	private final RpcAuthToken t;

	public GrithXmlRpcServlet() throws Exception {
		t = new RpcAuthToken();
	}


	private boolean isAuthenticated(String pUserName, String pPassword) {
		String un = System.getProperty("user.name");

		return un.equals(pUserName) && t.getAuthToken().equals(pPassword);
	}

	@Override
	protected XmlRpcHandlerMapping newXmlRpcHandlerMapping()
			throws XmlRpcException {
		PropertyHandlerMapping mapping = (PropertyHandlerMapping) super
				.newXmlRpcHandlerMapping();
		AbstractReflectiveHandlerMapping.AuthenticationHandler handler = new AbstractReflectiveHandlerMapping.AuthenticationHandler() {
			public boolean isAuthorized(XmlRpcRequest pRequest) {
				XmlRpcHttpRequestConfig config = (XmlRpcHttpRequestConfig) pRequest
						.getConfig();
				return isAuthenticated(config.getBasicUserName(),
						config.getBasicPassword());
			};
		};
		mapping.setAuthenticationHandler(handler);
		return mapping;
	}
}
