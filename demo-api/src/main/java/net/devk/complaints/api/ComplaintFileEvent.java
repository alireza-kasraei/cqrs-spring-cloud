package net.devk.complaints.api;

import lombok.Value;

@Value
public class ComplaintFileEvent {
	private final String id;
	private final String company;
	private final String description;

}