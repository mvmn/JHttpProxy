package x.mvmn.util.jhttpproxy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpRequest;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpTrace;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.message.BasicNameValuePair;
import org.eclipse.jetty.http.HttpMethod;

public class HttpRequestData {

	public static class HttpPartData {
		protected String name;
		protected final List<NameValuePair> headers = new ArrayList<NameValuePair>();
		protected byte[] body;
		protected String contentType;
		protected String submittedFileName;

		public static HttpPartData fromHttpServletRequestPart(final Part part) throws Exception {
			final HttpPartData result = new HttpPartData();
			for (final String headerName : part.getHeaderNames()) {
				for (final String headerVal : part.getHeaders(headerName)) {
					result.headers.add(new BasicNameValuePair(headerName, headerVal));
				}
			}
			result.name = part.getName();
			result.contentType = part.getContentType();
			result.submittedFileName = part.getSubmittedFileName();
			result.body = IOUtils.toByteArray(part.getInputStream());
			return result;
		}

		@Override
		public String toString() {
			return new StringBuilder("HttpPartData [name=").append(name).append(", headers=").append(headers).append(", body=").append(Arrays.toString(body))
					.append(", contentType=").append(contentType).append(", submittedFileName=").append(submittedFileName).append("]").toString();
		}

	}

	protected String method;
	protected String uri;
	protected List<HttpPartData> parts = new ArrayList<HttpPartData>();
	protected byte[] body;
	protected final List<NameValuePair> headers = new ArrayList<NameValuePair>();

	public static HttpRequestData fromHttpServletRequest(final HttpServletRequest httpServletRequest) throws Exception {
		final HttpRequestData result = new HttpRequestData();
		result.method = httpServletRequest.getMethod();
		result.uri = httpServletRequest.getRequestURI();
		final String queryString = httpServletRequest.getQueryString();
		if (queryString != null && !queryString.isEmpty()) {
			result.uri += "?" + queryString;
		}
		if (httpServletRequest.getContentType() != null && httpServletRequest.getContentType().startsWith("multipart/form-data")) {
			for (Part part : httpServletRequest.getParts()) {
				result.parts.add(HttpPartData.fromHttpServletRequestPart(part));
			}
		} else {
			result.body = IOUtils.toByteArray(httpServletRequest.getInputStream());
		}
		final Enumeration<String> headerNames = httpServletRequest.getHeaderNames();
		while (headerNames.hasMoreElements()) {
			final String headerName = headerNames.nextElement();
			final Enumeration<String> headerValues = httpServletRequest.getHeaders(headerName);
			result.addHeader(headerName, headerValues.nextElement());
		}

		return result;
	}

	public void addHeader(final String headerName, final String headerValue) {
		this.headers.add(new BasicNameValuePair(headerName, headerValue));
	}

	public NameValuePair deleteHeader(final String headerName) {
		NameValuePair result = null;

		final Iterator<NameValuePair> iterator = this.headers.iterator();
		while (iterator.hasNext()) {
			final NameValuePair header = iterator.next();
			if (header.getName().equals(headerName)) {
				iterator.remove();
				if (result == null) {
					// Take first value for multiple
					result = header;
				}
			}
		}

		return result;
	}

	public HttpRequest toHttpRequest() {
		HttpRequest result = null;
		if (this.method.equalsIgnoreCase(HttpMethod.GET.asString())) {
			result = new HttpGet(this.uri);
		} else if (this.method.equalsIgnoreCase(HttpMethod.HEAD.asString())) {
			result = new HttpHead(this.uri);
		} else if (this.method.equalsIgnoreCase(HttpMethod.OPTIONS.asString())) {
			result = new HttpOptions(this.uri);
		} else if (this.method.equalsIgnoreCase(HttpMethod.DELETE.asString())) {
			result = new HttpDelete(this.uri);
		} else if (this.method.equalsIgnoreCase(HttpMethod.TRACE.asString())) {
			result = new HttpTrace(this.uri);
		} else if (this.method.equalsIgnoreCase(HttpMethod.POST.asString()) || this.method.equalsIgnoreCase(HttpMethod.PUT.asString())) {
			final HttpEntityEnclosingRequestBase postOrPut = this.method.equalsIgnoreCase(HttpMethod.POST.asString()) ? new HttpPost(this.uri) : new HttpPut(
					this.uri);
			result = postOrPut;
			if (!this.parts.isEmpty()) {
				final MultipartEntityBuilder mpEntityBuilder = MultipartEntityBuilder.create();
				for (final HttpPartData part : this.parts) {
					if (part.contentType != null && !part.contentType.isEmpty()) {
						mpEntityBuilder.addPart(part.name, new ByteArrayBody(part.body, ContentType.parse(part.contentType), part.submittedFileName));
					} else {
						mpEntityBuilder.addPart(part.name, new ByteArrayBody(part.body, part.submittedFileName));
					}
				}
				postOrPut.setEntity(mpEntityBuilder.build());
			} else if (this.body != null && this.body.length > 0) {
				postOrPut.setEntity(new ByteArrayEntity(body));
			}
		}
		for (final NameValuePair header : this.headers) {
			result.addHeader(header.getName(), header.getValue());
		}
		return result;
	}

	@Override
	public String toString() {
		return new StringBuilder("HttpRequestData [method=").append(method).append(", uri=").append(uri).append(", parts=").append(parts).append(", body=")
				.append(Arrays.toString(body)).append(", headers=").append(headers).append("]").toString();
	}
}
