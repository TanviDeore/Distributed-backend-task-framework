package com.task.worker.dao;

import java.util.Optional;

import com.task.worker.entity.Status;
import com.task.worker.entity.Task;

public interface TaskUpdateDao {
	public void updateTaskStatus(String taskId, Status newStatus, String outputImageUrl, String failureReason);
	public Optional<Task> retriveTaskById(String taskId);
	
}
