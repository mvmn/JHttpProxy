package x.mvmn.util.jhttpproxy;

import java.awt.BorderLayout;
import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClients;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;

public class JHttpProxy {

	public static void main(final String args[]) throws Exception {
		new JHttpProxy().start(args);
	}

	public void start(final String args[]) throws Exception {
		final Server server = new Server(args.length > 1 ? Integer.parseInt(args[1]) : 1234);
		final HttpClient client = HttpClients.createDefault();
		final HttpHost targetHostDefault;
		{
			String splits[] = args[0].split("://");
			final String scheme = splits[0];
			String hostname = splits[1];
			splits = hostname.split(":");
			int port = scheme.equalsIgnoreCase("https") ? 443 : 80;
			hostname = splits[0];
			if (splits.length > 1) {
				port = Integer.parseInt(splits[1]);
			}
			targetHostDefault = new HttpHost(hostname, port, scheme);
		}

		final JTextArea scriptInput = new JTextArea();
		final JFrame frame = new JFrame("JHttpProxy: Groovy script for requestData manipulation");
		frame.setLayout(new BorderLayout());
		frame.add(new JScrollPane(scriptInput), BorderLayout.CENTER);
		frame.pack();

		server.setHandler(new AbstractHandler() {
			@Override
			public void handle(final String target, final Request baseRequest, final HttpServletRequest request, final HttpServletResponse response)
					throws IOException, ServletException {
				try {
					final HttpRequestData requestData = HttpRequestData.fromHttpServletRequest(request);
					System.out.println("#### Source request data:\n" + requestData + "\n### End\n");
					final Binding gshBinding = new Binding();
					final GroovyShell gsh = new GroovyShell(gshBinding);
					gshBinding.setProperty("requestData", requestData);
					gshBinding.setProperty("targetHost", targetHostDefault);
					gsh.evaluate(scriptInput.getText());
					HttpHost targetHost = targetHostDefault;
					Object targetHostObj = gshBinding.getProperty("targetHost");
					if (targetHostObj != null && targetHostObj instanceof HttpHost) {
						targetHost = (HttpHost) targetHostObj;
					}
					System.out.println("#### Target request data:\n" + requestData + "\n### End\n");
					final HttpResponse targetResponse = client.execute(targetHost, requestData.toHttpRequest());
					System.out.println("#### Target response data (host = " + targetHost + " ):\n" + targetResponse + "\n### End\n");
					for (final Header header : targetResponse.getAllHeaders()) {
						response.addHeader(header.getName(), header.getValue());
					}
					final String origin = request.getHeader("Origin");
					if (origin != null) {
						// Force CORS
						response.addHeader("Access-Control-Allow-Origin", origin);
					}
					response.setStatus(targetResponse.getStatusLine().getStatusCode());
					if (targetResponse.getEntity() != null) {
						targetResponse.getEntity().writeTo(response.getOutputStream());
					}
					baseRequest.setHandled(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		frame.setVisible(true);
		server.start();
	}
}
