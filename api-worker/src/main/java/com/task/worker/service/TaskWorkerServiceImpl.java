package com.task.worker.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Base64;
import java.util.Optional;

import javax.imageio.ImageIO;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.task.worker.dao.TaskUpdateDao;
import com.task.worker.entity.Status;
import com.task.worker.entity.Task;

import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.SqsException;

@Service
public class TaskWorkerServiceImpl implements TaskWorkerService {

	private final SqsClient sqsClient;
    private final S3Client s3Client;
    private final TaskUpdateDao taskUpdateDao; 
    private final ObjectMapper objectMapper;
    private final String taskQueueUrl;
    private final String outputBucketName;
    private final String awsRegion; // To construct S3 output URL
    private static final Logger logger = LogManager.getLogger(TaskWorkerServiceImpl.class);
    
    public TaskWorkerServiceImpl(SqsClient sqsClient, S3Client s3Client, TaskUpdateDao taskUpdateDao,
			ObjectMapper objectMapper, @Value("${aws.sqs.task-queue-url}") String taskQueueUrl,@Value("${aws.s3.output-bucket-name}") String outputBucketName,@Value("${aws.region}") String awsRegion) {
		super();
		this.sqsClient = sqsClient;
		this.s3Client = s3Client;
		this.taskUpdateDao = taskUpdateDao;
		this.objectMapper = objectMapper;
		this.taskQueueUrl = taskQueueUrl;
		this.outputBucketName = outputBucketName;
		this.awsRegion = awsRegion;
	}
    
    @Override
    @Scheduled(fixedDelayString = "5000")
	public void pollAndProcessTasks() {
    	ReceiveMessageRequest receiveMessageRequest = ReceiveMessageRequest.builder()
                .queueUrl(taskQueueUrl)
                .maxNumberOfMessages(1) // Fetching 1 message at a time
                .waitTimeSeconds(20)    
                .build();

        try {
            logger.info("INFO: Polling SQS queue: {}", taskQueueUrl);
            sqsClient.receiveMessage(receiveMessageRequest)
                     .messages()
                     .forEach(this::processMessage);
        } catch (SqsException e) {
            logger.error("ERROR: Error polling SQS queue {}: {}", taskQueueUrl, e.getMessage(), e);
        } catch (SdkClientException e) {
            logger.error("ERROR: AWS SDK client error while polling SQS: {}", e.getMessage(), e);
        }
		
	}

	@Override
	public void processMessage(Message message){
		Task task  = null;
		try {
            //Deserializing the message body into a Task object
            task = objectMapper.readValue(message.body(), Task.class);
            logger.info("INFO: Starting processing for taskId: {} (Receipt Handle: {})", task.getTaskId(), message.receiptHandle());

            //Updating task status to RUNNING in DynamoDB
            taskUpdateDao.updateTaskStatus(task.getTaskId(), Status.RUNNING, null, null);
            logger.info("INFO: Task {} status updated to RUNNING in DynamoDB.", task.getTaskId());

            
            Optional<Task> existingTaskOptional = taskUpdateDao.retriveTaskById(task.getTaskId());
            if (existingTaskOptional.isPresent() && existingTaskOptional.get().getStatus() == Status.COMPLETED) {
                logger.warn("WARN: Task {} already COMPLETED. Skipping reprocessing.", task.getTaskId());
                return;
            }
            

            // Downloading the input image
            BufferedImage originalImage = downloadImage(task.getInputImageUrl(), task.getTaskId());
            logger.info("INFO: Image downloaded for taskId: {}", task.getTaskId());

            // Performing image transformation (grayscale)
            BufferedImage grayImage = convertToGrayscale(originalImage);
            logger.info("INFO: Image transformed to grayscale for taskId: {}", task.getTaskId());

            // Storing processed image in S3
            String outputS3Key = "processed-images/" + task.getTaskId() + ".png"; 
            String outputS3Url = String.format("https://%s.s3.%s.amazonaws.com/%s", outputBucketName, awsRegion, outputS3Key);

            uploadImageToS3(grayImage, outputS3Key);
            logger.info("INFO: Processed image uploaded to S3: {}", outputS3Url);

            // Updating task status to COMPLETED in DynamoDB, including output URL
            taskUpdateDao.updateTaskStatus(task.getTaskId(), Status.COMPLETED, outputS3Url, null);
            logger.info("INFO: Task {} status updated to COMPLETED in DynamoDB. Output URL: {}", task.getTaskId(), outputS3Url);

            // Deleting the message from SQS ONLY if all steps above succeed
            deleteMessageFromSqs(message);
            logger.info("INFO: SQS message deleted for taskId: {}", task.getTaskId());

        } catch (IOException e) {
            // Error during image download, transformation, or stream operations
            logger.error("ERROR: Image processing I/O error for taskId: {}: {}", (task != null) ? task.getTaskId() : "Unknown", e.getMessage(), e);
           
           } catch (S3Exception e) {
            // Error during S3 operations (get or put)
            logger.error("S3 error for taskId: {}: {}. AWS Error Code: {}", task != null ? task.getTaskId() : "Unknown", e.getMessage(), e.awsErrorDetails().errorCode(), e);
            if (task != null) {
                taskUpdateDao.updateTaskStatus(task.getTaskId(), Status.FAILED, null, "S3 error: " + e.awsErrorDetails().errorMessage());
                logger.error("ERROR: Task {} status updated to FAILED due to S3 error.", task.getTaskId());
            }
            
        } catch (RuntimeException e) {
            logger.error("ERROR: Runtime error processing task ID: {}: {}. Message will be returned to queue.", task != null ? task.getTaskId() : "Unknown", e.getMessage(), e);
            deleteMessageFromSqs(message);
            if (task != null) {
                taskUpdateDao.updateTaskStatus(task.getTaskId(), Status.FAILED, null, "Processing error: " + e.getMessage());
                logger.warn("WARN: Task {} status updated to FAILED due to runtime error.", task.getTaskId());
            }
            
        } catch (Exception e) {
         
            logger.error("An unexpected error occurred for task ID: {}: {}. Message will be returned to queue.", task != null ? task.getTaskId() : "Unknown", e.getMessage(), e);
            deleteMessageFromSqs(message);
            if (task != null) {
                taskUpdateDao.updateTaskStatus(task.getTaskId(), Status.FAILED, null, "Unexpected error: " + e.getMessage());
                logger.warn("Task {} status updated to FAILED due to unexpected error.", task.getTaskId());
            }
            
        }

	}

	@Override
	public BufferedImage downloadImage(String imageUrl, String taskId) throws IOException{
		logger.debug("Attempting to download image from: {}", imageUrl);
		if (imageUrl.startsWith("data:")) {
		    String[] parts = imageUrl.split(",", 2);
		    byte[] bytes = Base64.getDecoder().decode(parts[1]);
		    return ImageIO.read(new ByteArrayInputStream(bytes));
		  }
		  URL url = new URL(imageUrl);
		  return ImageIO.read(url);
		
	}

	@Override
	public BufferedImage convertToGrayscale(BufferedImage originalImage) {
		if (originalImage == null) {
			
            throw new IllegalArgumentException("Original image cannot be null for grayscale conversion.");
        }

        BufferedImage grayImage = new BufferedImage(
                originalImage.getWidth(), originalImage.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        grayImage.getGraphics().drawImage(originalImage, 0, 0, null);
        return grayImage;
	}

	@Override
	public void uploadImageToS3(BufferedImage image, String s3Key) {
		try {
			ByteArrayOutputStream os = new ByteArrayOutputStream();
	        ImageIO.write(image, "png", os); // Write as PNG
	        byte[] imageBytes = os.toByteArray();

	        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
	                .bucket(outputBucketName)
	                .key(s3Key)
	                .contentType("image/png") // Set content type
	                .build();

	        s3Client.putObject(putObjectRequest, RequestBody.fromBytes(imageBytes));
	        logger.debug("DEBUG: Uploaded {} bytes to S3 key: {}", imageBytes.length, s3Key);
		}catch(Exception e) {
			logger.error("ERROR: Error oaccured while uploading the image to S3 "+e.getMessage());
		}
	}

	@Override
	public void deleteMessageFromSqs(Message message) {
		DeleteMessageRequest deleteMessageRequest = DeleteMessageRequest.builder()
                .queueUrl(taskQueueUrl)
                .receiptHandle(message.receiptHandle())
                .build();
        try {
            sqsClient.deleteMessage(deleteMessageRequest);
        } catch (SqsException e) {
            logger.error("ERROR: Error deleting SQS message {} from queue {}: {}", message.messageId(), taskQueueUrl, e.getMessage(), e);
        }

	}

}
