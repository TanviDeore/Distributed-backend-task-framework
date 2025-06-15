package com.api.apiservice.controller;

import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.api.apiservice.entity.Task;
import com.api.apiservice.service.TaskService;
import com.api.apiservice.service.TaskServiceImpl;

@RestController
@RequestMapping("/tasks")
@CrossOrigin("*")
public class TaskController {
	private static final Logger logger = LogManager.getLogger(TaskController.class);
	
	@Autowired
	TaskService taskService;
	
	@PostMapping("/add")
	public ResponseEntity<Task> addOrUpdateTask(@RequestBody Task task) {
		try {
			Task st  = taskService.saveOrUpdateTheTask(task);
			return new ResponseEntity<>(st, HttpStatus.CREATED);
		} catch (Exception e) {
			logger.error("Exception while adding task", e);
			return new ResponseEntity<>(null, HttpStatus.CONFLICT);
		}
	}
	
	@GetMapping("/name")
	public ResponseEntity<Task> getTaskByTaskName(@RequestParam(name="taskName") String taskName){
		Optional<Task> taskOptional = taskService.retriveTaskByName(taskName);
		return taskOptional
                .map(task -> new ResponseEntity<>(task, HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
	}
	
	@GetMapping("/id/{taskId}")
	public ResponseEntity<Task> getTaskByTaskId(@PathVariable("taskId") String taskId){
		Optional<Task> taskOptional = taskService.retriveTaskById(taskId);
		return taskOptional
                .map(task -> new ResponseEntity<>(task, HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
	}
	
}
