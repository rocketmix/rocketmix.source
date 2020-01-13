package com.essec.microservices.actuator.apicalls;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.apache.lucene.analysis.core.LowerCaseFilterFactory;
import org.apache.lucene.analysis.core.WhitespaceTokenizerFactory;
import org.apache.lucene.analysis.miscellaneous.ASCIIFoldingFilterFactory;
import org.hibernate.search.annotations.AnalyzerDef;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.TokenFilterDef;
import org.hibernate.search.annotations.TokenizerDef;

@Entity
@Indexed
@AnalyzerDef(name = "customanalyzer",
tokenizer = @TokenizerDef(factory = WhitespaceTokenizerFactory.class),
filters = {
        @TokenFilterDef(factory = ASCIIFoldingFilterFactory.class), // Replace accented characeters by their simpler counterpart (è => e, etc.)
        @TokenFilterDef(factory = LowerCaseFilterFactory.class) // Lowercase all characters
})
public class ApiCall {
	
	@Id
	@GeneratedValue
	private Long id;
	
	@Field
	private Date date = new Date();

	@Field
	private String requestURL;

	@Field
	private String requestData;
	
	@Field
	private String responseData;
	
	@Field
	private int responseCode;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public String getRequestURL() {
		return requestURL;
	}

	public void setRequestURL(String requestURL) {
		this.requestURL = requestURL;
	}

	public String getRequestData() {
		return requestData;
	}

	public void setRequestData(String requestData) {
		this.requestData = requestData;
	}

	public String getResponseData() {
		return responseData;
	}

	public void setResponseData(String responseData) {
		this.responseData = responseData;
	}

	public int getResponseCode() {
		return responseCode;
	}

	public void setResponseCode(int responceCode) {
		this.responseCode = responceCode;
	}
	
	

}