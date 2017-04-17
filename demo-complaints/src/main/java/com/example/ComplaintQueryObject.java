package com.example;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class ComplaintQueryObject {

	@Id
	private String complaintId;
	private String company;
	private String description;

	public ComplaintQueryObject() {
		super();
	}

	public ComplaintQueryObject(String complaintId, String company, String description) {
		super();
		this.complaintId = complaintId;
		this.company = company;
		this.description = description;
	}

	public String getComplaintId() {
		return complaintId;
	}

	public void setComplaintId(String complaintId) {
		this.complaintId = complaintId;
	}

	public String getCompany() {
		return company;
	}

	public void setCompany(String company) {
		this.company = company;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

}
