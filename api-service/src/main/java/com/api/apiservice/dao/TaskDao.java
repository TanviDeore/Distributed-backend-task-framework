package com.api.apiservice.dao;

import java.util.Optional;

import com.api.apiservice.entity.Task;

public interface TaskDao {
	public void saveOrUpdateTheTask(Task task);
	
	public Optional<Task> retriveTaskById(String taskId);
	
	public Optional<Task> retriveTaskByName(String taskName);
}
