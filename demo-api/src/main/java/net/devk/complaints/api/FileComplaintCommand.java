package net.devk.complaints.api;

import lombok.Value;

@Value
public class FileComplaintCommand {

	private final String id;
	private final String company;
	private final String description;

}