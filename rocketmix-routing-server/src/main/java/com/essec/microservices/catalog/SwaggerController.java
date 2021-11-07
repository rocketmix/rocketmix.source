package com.essec.microservices.catalog;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;

import javax.net.ssl.SSLContext;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContextBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.shared.Application;

@RestController
@RequestMapping("/catalog")
public class SwaggerController {

	private static final List<String> IGNORED_APPLICATION_NAMES = Arrays.asList("ZUULSERVER");
	private static final String DOC_PATH = "/services/openapi.json";
	private static final String PROXY_ROOT_PATH = "/catalog/swagger-docs/proxy";
	private static final String PROXY_PATH = PROXY_ROOT_PATH + "?url={0}&vipaddress={1}";
	private static final String HTTP_PROTOCOL_PREFIX_SEARCH_PATTERN = "^(?i)http://";
	private static final String HTTPS_PROTOCOL_PREFIX = "https://";

	@Autowired
	private EurekaClient eurekaClient;

	@Value("${spring.application.name}")
	private String LOCAL_SERVER_NAME;

	@RequestMapping(value = "/swagger-ui/index.html", method = RequestMethod.GET)
	public @ResponseBody byte[] index() throws IOException {
		ClassPathResource htmlResource = new ClassPathResource(
				"/META-INF/resources/webjars/swagger-ui/4.0.1/index.html");
		InputStream htmlStream = htmlResource.getInputStream();
		String content = IOUtils.toString(htmlStream);
		content = content.replaceAll("url.*:.*,", getUrls());
		ClassPathResource jsResource = new ClassPathResource("/swaggerui-updater.js");
		InputStream jsStream = jsResource.getInputStream();
		String jsContent = IOUtils.toString(jsStream);
		content = content.replace("window.ui = ui", "window.ui = ui;\r\n" + jsContent);
		return content.getBytes();
	}

	@RequestMapping(value = "/swagger-ui/swagger-ui.css", method = RequestMethod.GET)
	public @ResponseBody byte[] css() throws IOException {
		ClassPathResource classPathResource = new ClassPathResource(
				"/META-INF/resources/webjars/swagger-ui/4.0.1/swagger-ui.css");
		InputStream inputStream = classPathResource.getInputStream();
		String content = IOUtils.toString(inputStream);
		content = content.replaceFirst("\\.swagger-ui \\.topbar[ ]*\\{[^\\}]*\\}",
				".swagger-ui .topbar{padding:8px 0;background-color: #000000;height: 52px;white-space: nowrap;}");
		content = content.replaceFirst("\\.swagger-ui \\.info[ ]*\\{[^\\}]*\\}",
				".swagger-ui .info {margin: 0px 0px 0px 0px;}");
		content = content.replaceFirst("\\.swagger-ui \\.info \\.title[ ]*\\{[^\\}]*\\}",
				".swagger-ui .info .title{font-size: 24px;margin-top: 0px; margin-bottom: 50px; margin-left: auto; margin-right: auto; padding-top: 30px; padding-bottom: 30px;font-family:sans-serif;color: #FFFFFF; max-width: 1400px; letter-spacing: 2px;}");
		content = content.replaceFirst("\\.swagger-ui \\.info a[ ]*\\{[^\\}]*\\}",
				".swagger-ui .info a{visibility: hidden;display: none;}");
		content = content.replaceFirst("\\.swagger-ui \\.opblock-tag[ ]*\\{[^\\}]*\\}",
				".swagger-ui .opblock-tag{visibility: hidden;display: none;}");
		content = content.replaceFirst("\\.swagger-ui \\.errors-wrapper \\.error-wrapper[ ]*\\{[^\\}]*\\}",
				".swagger-ui .errors-wrapper .error-wrapper{visibility: hidden;display: none;}");
		content = content.replace("#547f00", "#42d3a5");
		content = content.replace("#3b4151", "#7a7a7a");
		content = content + " div.information-container.wrapper {max-width: 100%; background-color: #42d3a5;} ";
		content = content + " div.servers,span.servers-title {display:none;} ";
		content = content + " .swagger-ui .errors-wrapper hgroup > button {display:none;} ";
		content = content + " .swagger-ui .errors-wrapper div:first-child:after {content:\"Access denied\";} ";
		content = content + " .swagger-ui .info .description {display:none;} ";
		return content.getBytes();
	}

	private String getUrls() {
		StringBuilder builder = new StringBuilder();
		JsonArray serversArray = new JsonArray();
		JsonObject welcomeServer = new JsonObject();
		welcomeServer.addProperty("url", "/openapi/default.json");
		welcomeServer.addProperty("name", "DEFAULT");
		serversArray.add(welcomeServer);
		builder.append("urls: ");
		try {
			List<Application> applications = eurekaClient.getApplications().getRegisteredApplications();
			for (Application application : applications) {
				try {
					List<InstanceInfo> applicationsInstances = application.getInstances();
					for (InstanceInfo applicationsInstance : applicationsInstances) {
						String name = applicationsInstance.getAppName();
						System.out.println("Application name found for docs : " + name);
						if (IGNORED_APPLICATION_NAMES.contains(name)) {
							continue;
						}
						String homePageUrl = applicationsInstance.getHomePageUrl();
						if (LOCAL_SERVER_NAME.equals(name)) { // Need to get external address, not local ip
							String currentServerURL = getCurrentRequest().getRequestURL().toString();
							String servletPath = getCurrentRequest().getServletPath();
							homePageUrl = currentServerURL.replace(servletPath, "/");
						}
						String jsonAPIDefinitionUrl = homePageUrl + DOC_PATH;
						jsonAPIDefinitionUrl = jsonAPIDefinitionUrl.replace("//", "/");
						jsonAPIDefinitionUrl = jsonAPIDefinitionUrl.replace(":/", "://");
						String url = MessageFormat.format(PROXY_PATH, URLEncoder.encode(jsonAPIDefinitionUrl, "UTF-8"),
								URLEncoder.encode(applicationsInstance.getVIPAddress(), "UTF-8"));
						JsonObject aServer = new JsonObject();
						aServer.addProperty("url", url);
						aServer.addProperty("name", name);
						serversArray.add(aServer);
						break;
					}
				} catch (Exception e1) {
					e1.printStackTrace();
					continue;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		String jsonServersArrayString = serversArray.toString();
		builder.append(jsonServersArrayString);
		builder.append(",");
		return builder.toString();
	}

	@RequestMapping(value = "/swagger-docs/proxy", method = RequestMethod.GET)
	public @ResponseBody byte[] proxy(@RequestParam String url, @RequestParam String vipaddress) throws IOException {
		String currentServerURL = getCurrentRequest().getRequestURL().toString();
		currentServerURL = fixReverseProxyProtocol(currentServerURL);
		currentServerURL = currentServerURL.replace(PROXY_ROOT_PATH, "");
		Executor executor = Executor.newInstance(noSslHttpClient());
		String content = executor.execute(Request.Get(url)).returnContent().asString(StandardCharsets.UTF_8);
		Gson gson = new Gson();
		JsonObject jsonObject = gson.fromJson(content, JsonObject.class);
		jsonObject.remove("servers");
		JsonArray serversArray = new JsonArray();
		JsonObject routingServer = new JsonObject();
		routingServer.addProperty("url", currentServerURL + "/" + vipaddress.toLowerCase() + "/services/");
		serversArray.add(routingServer);
		jsonObject.add("servers", serversArray);
		content = jsonObject.toString();
		return content.getBytes();
	}

	private static HttpServletRequest getCurrentRequest() {
		RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
		Assert.state(requestAttributes != null, "Could not find current request via RequestContextHolder");
		Assert.isInstanceOf(ServletRequestAttributes.class, requestAttributes);
		HttpServletRequest servletRequest = ((ServletRequestAttributes) requestAttributes).getRequest();
		Assert.state(servletRequest != null, "Could not find current HttpServletRequest");
		return servletRequest;
	}

	private String fixReverseProxyProtocol(String url) {
		HttpServletRequest currentRequest = getCurrentRequest();
		if (currentRequest == null) {
			return url;
		}
		if (StringUtils.isNotBlank(url) && currentRequest.isSecure()) {
			url = url.replaceAll(HTTP_PROTOCOL_PREFIX_SEARCH_PATTERN, HTTPS_PROTOCOL_PREFIX);
		}
		return url;
	}

	private static CloseableHttpClient noSslHttpClient() throws IOException {
		try {
			final SSLContext sslContext = new SSLContextBuilder()
					.loadTrustMaterial(null, (x509CertChain, authType) -> true).build();
			return HttpClientBuilder.create().setSSLContext(sslContext)
					.setConnectionManager(new PoolingHttpClientConnectionManager(RegistryBuilder
							.<ConnectionSocketFactory>create().register("http", PlainConnectionSocketFactory.INSTANCE)
							.register("https",
									new SSLConnectionSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE))
							.build()))
					.build();
		} catch (Exception e) {
			throw new IOException(e);
		}
	}

}
