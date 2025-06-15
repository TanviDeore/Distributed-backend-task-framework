package com.api.apiservice.service;

import java.util.Optional;

import com.api.apiservice.entity.Task;

public interface TaskService {
	public Task saveOrUpdateTheTask(Task task);
	public Optional<Task> retriveTaskById(String taskId);
	public Optional<Task> retriveTaskByName(String taskName);
}
