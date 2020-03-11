package com.essec.microservices.admin.extension.actuator;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.web.annotation.RestControllerEndpoint;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.essec.microservices.admin.extension.model.ApiCallEntry;
import com.essec.microservices.admin.extension.service.ApiCallSearchService;
import com.essec.microservices.admin.extension.service.ApiCallThroughputService;

@Component
@RestControllerEndpoint(id = "apicalls")
public class ApiCallActuatorEndpoint {

	@Autowired
	private ApiCallThroughputService throughputService;

	@Autowired
	private ApiCallSearchService searchService; 

	
	@GetMapping("/throughput")
	public float throughput() {
		return this.throughputService.getThroughputPerSecond();
	}
	
	
	@GetMapping("/findAll")
	public List<ApiCallEntry> findAll() {
		return this.searchService.findAll();
	}
	
	@GetMapping("/find/{keyword}")
	public List<ApiCallEntry> find(@PathVariable("keyword") String keyword) {
		return this.searchService.performSearch(keyword);
	}
	
}
