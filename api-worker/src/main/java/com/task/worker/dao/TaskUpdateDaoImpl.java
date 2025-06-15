package com.task.worker.dao;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.task.worker.entity.Status;
import com.task.worker.entity.Task;

import jakarta.annotation.PostConstruct;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeAction;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.AttributeValueUpdate;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;

@Repository
public class TaskUpdateDaoImpl implements TaskUpdateDao{
	

	private final DynamoDbEnhancedClient enhancedClient;
	private final DynamoDbClient dynamoDbClient;
	private String tableName = "tasks";
	private DynamoDbTable<Task> tasks;
	
	public TaskUpdateDaoImpl(DynamoDbEnhancedClient enhancedClient, DynamoDbClient dynamoDbClient) {
		//super();
		this.enhancedClient = enhancedClient;
		this.dynamoDbClient = dynamoDbClient;
	}
	
	
	@PostConstruct
    public void init() {
        // Get a reference to the "tasks" table, mapped to our Task class
        this.tasks = enhancedClient.table("tasks", TableSchema.fromBean(Task.class));
    }
	
	@Override
	public void updateTaskStatus(String taskId, Status newStatus, String outputImageUrl, String failureReason) {
		Map<String, AttributeValueUpdate> updates = new HashMap<>();

        // Always update status
        updates.put("status", AttributeValueUpdate.builder()
                .value(AttributeValue.builder().s(newStatus.name()).build())
                .action(AttributeAction.PUT)
                .build());

        // Always update completionTimestamp (even for FAILED)
        updates.put("completionTimestamp", AttributeValueUpdate.builder()
                .value(AttributeValue.builder().s(Instant.now().toString()).build())
                .action(AttributeAction.PUT)
                .build());

        // Conditionally update outputImageUrl
        if (newStatus == Status.COMPLETED && outputImageUrl != null) {
            updates.put("outputImageUrl", AttributeValueUpdate.builder()
                    .value(AttributeValue.builder().s(outputImageUrl).build())
                    .action(AttributeAction.PUT)
                    .build());
        } else if (newStatus != Status.COMPLETED) {
            // Ensure outputImageUrl is removed or set to null if status is not COMPLETED
            updates.put("outputImageUrl", AttributeValueUpdate.builder()
                    .action(AttributeAction.DELETE) // Or PUT with null string, but DELETE is cleaner
                    .build());
        }

        // Conditionally update failureReason
        if (newStatus == Status.FAILED && failureReason != null) {
            updates.put("failureReason", AttributeValueUpdate.builder()
                    .value(AttributeValue.builder().s(failureReason).build())
                    .action(AttributeAction.PUT)
                    .build());
        } else if (newStatus != Status.FAILED) {
            // Ensure failureReason is removed or set to null if status is not FAILED
            updates.put("failureReason", AttributeValueUpdate.builder()
                    .action(AttributeAction.DELETE)
                    .build());
        }


        UpdateItemRequest request = UpdateItemRequest.builder()
                .tableName(tableName)
                .key(Map.of("taskId", AttributeValue.builder().s(taskId).build())) // Your primary key
                .attributeUpdates(updates)
                .build();

        try {
            dynamoDbClient.updateItem(request);
            System.out.println("DynamoDB: Task " + taskId + " status updated to " + newStatus);
        } catch (DynamoDbException e) {
            System.err.println("DynamoDB Error updating task " + taskId + ": " + e.getMessage());
            throw new RuntimeException("Failed to update task status in DynamoDB", e);
        }
		
	}

	@Override
	public Optional<Task> retriveTaskById(String taskId) {
		Key key = Key.builder().partitionValue(taskId).build();
		return Optional.ofNullable(tasks.getItem(key));
	}

}
