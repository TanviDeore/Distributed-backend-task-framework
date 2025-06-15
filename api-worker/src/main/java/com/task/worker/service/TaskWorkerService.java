package com.task.worker.service;

import java.awt.image.BufferedImage;
import java.io.IOException;

import software.amazon.awssdk.services.sqs.model.Message;

public interface TaskWorkerService {
	public void pollAndProcessTasks();
	public void processMessage(Message message);
	public BufferedImage downloadImage(String imageUrl, String taskId) throws IOException;
	public BufferedImage convertToGrayscale(BufferedImage originalImage);
	public void uploadImageToS3(BufferedImage image, String s3Key);
	public void deleteMessageFromSqs(Message message);

}
