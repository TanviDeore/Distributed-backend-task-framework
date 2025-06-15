package com.api.apiservice.entity;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;

@DynamoDbBean
public class Task {
	private String taskId;
	private String taskName;
	private Status status;
    private String inputImageUrl;
    private String outputImageUrl;
    private String creationTimestamp;
    private String completionTimestamp;
    private String failureReason;

	
	@DynamoDbPartitionKey
	public String getTaskId(){
		return taskId;
	}


	public Status getStatus() {
		return status;
	}


	public void setStatus(Status status) {
		this.status = status;
	}


	public String getInputImageUrl() {
		return inputImageUrl;
	}


	public void setInputImageUrl(String inputImageUrl) {
		this.inputImageUrl = inputImageUrl;
	}


	public String getOutputImageUrl() {
		return outputImageUrl;
	}


	public void setOutputImageUrl(String outputImageUrl) {
		this.outputImageUrl = outputImageUrl;
	}


	public String getCreationTimestamp() {
		return creationTimestamp;
	}


	public void setCreationTimestamp(String creationTimestamp) {
		this.creationTimestamp = creationTimestamp;
	}


	public String getCompletionTimestamp() {
		return completionTimestamp;
	}


	public void setCompletionTimestamp(String completionTimestamp) {
		this.completionTimestamp = completionTimestamp;
	}


	public String getFailureReason() {
		return failureReason;
	}


	public void setFailureReason(String failureReason) {
		this.failureReason = failureReason;
	}

	// for application to set id
	public void setTaskId(String taskId) {
		this.taskId = taskId;
	}

	@DynamoDbSecondaryPartitionKey(indexNames="taskName-index")
	public String getTaskName() {
		return taskName;
	}


	public void setTaskName(String taskName) {
		this.taskName = taskName;
	}
	
	
}
