package com.essec.microservices.catalog;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class SwaggerConfiguration implements WebMvcConfigurer {

	
	@Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
       registry.addResourceHandler("/catalog/swagger-ui/**").addResourceLocations("classpath:/META-INF/resources/webjars/swagger-ui/4.0.1/");
    }
	
	@Override
	public void addViewControllers(ViewControllerRegistry registry) {
		registry.addRedirectViewController("/catalog", "/catalog/swagger-ui/index.html?urls.primaryName=DEFAULT");
	}
	
}
