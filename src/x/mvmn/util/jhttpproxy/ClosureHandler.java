package x.mvmn.util.jhttpproxy;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;

import groovy.lang.Closure;

public class ClosureHandler extends org.eclipse.jetty.server.handler.AbstractHandler {
	private final Closure<?> closure;

	public ClosureHandler(Closure<?> closure) {
		this.closure = closure;
	}

	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		baseRequest.setHandled(true);
		closure.call(request, response, baseRequest, target);
	}
}