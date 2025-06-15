package com.api.apiservice.dao;


import java.util.Optional;


import org.springframework.stereotype.Repository;

import com.api.apiservice.entity.Task;

import jakarta.annotation.PostConstruct;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbIndex;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;

@Repository
public class TaskDaoImpl implements TaskDao {
	
	private final DynamoDbEnhancedClient enhancedClient;
	private DynamoDbTable<Task> tasks;
	

	public TaskDaoImpl(DynamoDbEnhancedClient enhancedClient) {
        this.enhancedClient = enhancedClient;
    }
	
	@PostConstruct
    public void init() {
        // Get a reference to the "tasks" table, mapped to our Task class
        this.tasks = enhancedClient.table("tasks", TableSchema.fromBean(Task.class));
    }

	@Override
	public void saveOrUpdateTheTask(Task task) {
		// TODO Auto-generated method stub
		tasks.putItem(task);
	}

	@Override
	public Optional<Task> retriveTaskById(String taskId) {
		 Key key = Key.builder().partitionValue(taskId).build();
		 return Optional.ofNullable(tasks.getItem(key));
	}

	@Override
	public Optional<Task> retriveTaskByName(String taskName) {
		DynamoDbIndex<Task> nameIndex = tasks.index("taskName-index");
        return nameIndex.query(r -> 
                r.queryConditional(
                  QueryConditional.keyEqualTo(k -> k.partitionValue(taskName))
                )
            )
            .stream()
            .flatMap(page -> page.items().stream())
            .findFirst();
	}

}
