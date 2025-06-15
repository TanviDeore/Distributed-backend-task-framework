package com.api.apiservice.service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.api.apiservice.dao.TaskDao;
import com.api.apiservice.entity.Status;
import com.api.apiservice.entity.Task;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

@Service
public class TaskServiceImpl implements TaskService {
	
	private final TaskDao taskDao;
	private final SqsClient sqsClient;
	private final String taskQueueUrl;
	private final ObjectMapper objectMapper;
	private static final Logger logger = LogManager.getLogger(TaskServiceImpl.class);
		
	@Autowired
	 public TaskServiceImpl(TaskDao taskDao, SqsClient sqsClient,@Value("${aws.sqs.task-queue-url}") String taskQueueUrl,ObjectMapper objectMapper) {
	        this.taskDao = taskDao;
	        this.sqsClient=sqsClient;
	        this.taskQueueUrl=taskQueueUrl;
	        this.objectMapper = objectMapper;
	    }

	@Override
	public Task saveOrUpdateTheTask(Task task) {
		try {
			Instant now = Instant.now();
			task.setTaskId(UUID.randomUUID().toString());
			task.setCreationTimestamp(now.toString());
			task.setStatus(Status.PENDING);
			logger.info("INFO:Successfully created task with ID: {}", task.getTaskId());
			taskDao.saveOrUpdateTheTask(task);
			logger.info("INFO: Successfully saved task with ID: {} to DynamoDb", task.getTaskId());
			
			String messageBody = objectMapper.writeValueAsString(task); // Correctly use 'task' parameter
            
            SendMessageRequest sendMsgRequest = SendMessageRequest.builder()
                .queueUrl(taskQueueUrl)
                .messageBody(messageBody)
                .build();

            sqsClient.sendMessage(sendMsgRequest);
            logger.info("Successfully sent message to SQS for taskId: {}. Message Body: {}", task.getTaskId(), messageBody); 
			
			
			return task;
		} catch (JsonProcessingException e) {
            logger.error("ERROR: Error serializing task object to JSON for SQS message for taskId: {}", task.getTaskId(), e);
            throw new RuntimeException("Failed to serialize task for SQS message.", e);
        } catch (SdkClientException e) {
             logger.error("ERROR: Error sending message to SQS for taskId: {}. AWS SDK Error: {}", task.getTaskId(), e.getMessage(), e);
             throw new RuntimeException("Failed to send SQS message.", e);
        }
		catch (Exception e) {
			logger.info("Error occurred: "+e.getMessage());
			throw new RuntimeException("Could not create task due to a persistence error.", e);
		}
	}

	@Override
	public Optional<Task> retriveTaskById(String taskId) {
		// TODO Auto-generated method stub
		return taskDao.retriveTaskById(taskId);
	}

	@Override
	public Optional<Task> retriveTaskByName(String taskName) {
		// TODO Auto-generated method stub
		return taskDao.retriveTaskByName(taskName);
	}

}
