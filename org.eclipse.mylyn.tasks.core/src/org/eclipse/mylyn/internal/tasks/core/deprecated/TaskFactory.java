/*******************************************************************************
 * Copyright (c) 2004, 2007 Mylyn project committers and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.mylyn.internal.tasks.core.deprecated;

import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.mylyn.internal.tasks.core.AbstractTask;
import org.eclipse.mylyn.internal.tasks.core.ITaskList;
import org.eclipse.mylyn.internal.tasks.core.TaskDataStorageManager;
import org.eclipse.mylyn.internal.tasks.core.data.TaskDataManager;
import org.eclipse.mylyn.tasks.core.ITask;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.core.ITask.SynchronizationState;

/**
 * @deprecated Do not use. This class is pending for removal: see bug 237552.
 */
@Deprecated
public class TaskFactory implements ITaskFactory {

	private final AbstractLegacyRepositoryConnector connector;

	private final TaskDataManager taskDataManager;

	private final TaskRepository repository;

	private final ITaskList taskList;

	private final AbstractTaskDataHandler dataHandler;

	private final boolean updateTasklist;

	private final boolean forced;

	private final TaskDataStorageManager taskDataStorageManager;

	public TaskFactory(TaskRepository repository, boolean updateTasklist, boolean forced,
			AbstractLegacyRepositoryConnector connector, TaskDataManager taskDataManager, ITaskList taskList) {
		this.repository = repository;
		this.updateTasklist = updateTasklist;
		this.forced = forced;
		this.connector = connector;
		this.taskDataManager = taskDataManager;
		this.taskList = taskList;
		this.dataHandler = connector.getLegacyTaskDataHandler();
		this.taskDataStorageManager = taskDataManager.getTaskDataStorageManager();
	}

	/**
	 * @param updateTasklist
	 *            - synchronize task with the provided taskData
	 * @param forced
	 *            - user requested synchronization
	 * @throws CoreException
	 */
	public AbstractTask createTask(RepositoryTaskData taskData, IProgressMonitor monitor) throws CoreException {
		AbstractTask repositoryTask = (AbstractTask) taskList.getTask(taskData.getRepositoryUrl(), taskData.getTaskId());
		if (repositoryTask == null) {
			repositoryTask = createTaskFromTaskData(connector, repository, taskData, updateTasklist, monitor);
			repositoryTask.setSynchronizationState(SynchronizationState.INCOMING);
			if (!taskData.isPartial()) {
				if (updateTasklist) {
					taskList.addTask(repositoryTask);
					taskDataManager.saveIncoming(repositoryTask, taskData, forced);
				} else {
					taskDataManager.saveOffline(repositoryTask, taskData);
				}
			}
		} else {
			if (updateTasklist) {
				boolean changed;
				if (!taskData.isPartial()) {
					changed = taskDataManager.saveIncoming(repositoryTask, taskData, forced);
					connector.updateTaskFromTaskData(repository, repositoryTask, taskData);
				} else {
					changed = connector.updateTaskFromTaskData(repository, repositoryTask, taskData);
					if (changed) {
						switch (repositoryTask.getSynchronizationState()) {
						case OUTGOING:
							repositoryTask.setSynchronizationState(SynchronizationState.CONFLICT);
						case SYNCHRONIZED:
							repositoryTask.setSynchronizationState(SynchronizationState.INCOMING);
						}
					}
				}
				if (dataHandler != null) {
					for (ITask child : repositoryTask.getChildren()) {
						taskList.removeFromContainer(repositoryTask, child);
					}
					Set<String> subTaskIds = dataHandler.getSubTaskIds(taskData);
					if (subTaskIds != null) {
						for (String subId : subTaskIds) {
							if (subId == null || subId.trim().equals("")) {
								continue;
							}
							AbstractTask subTask = createTaskFromExistingId(connector, repository, subId, false,
									new SubProgressMonitor(monitor, 1));
							if (subTask != null) {
								taskList.addTask(subTask, repositoryTask);
							}
						}
					}
				}
				if (changed) {
					taskList.notifyElementChanged(repositoryTask);
				}
			}
		}
		return repositoryTask;
	}

	/**
	 * Creates a new task from the given task data. Does NOT add resulting task to the tasklist
	 */
	private AbstractTask createTaskFromTaskData(AbstractLegacyRepositoryConnector connector, TaskRepository repository,
			RepositoryTaskData taskData, boolean retrieveSubTasks, IProgressMonitor monitor) throws CoreException {
		AbstractTask repositoryTask = null;
		if (monitor == null) {
			monitor = new NullProgressMonitor();
		}
		try {
			if (taskData != null) {
				// Use connector task factory
				repositoryTask = connector.createTask(repository.getRepositoryUrl(), taskData.getTaskId(),
						taskData.getTaskId() + ": " + taskData.getDescription());
				connector.updateTaskFromTaskData(repository, repositoryTask, taskData);
				if (!taskData.isPartial()) {
					taskDataStorageManager.setNewTaskData(taskData);
				}

				if (retrieveSubTasks) {
					monitor.beginTask("Creating task", connector.getLegacyTaskDataHandler()
							.getSubTaskIds(taskData)
							.size());
					for (String subId : connector.getLegacyTaskDataHandler().getSubTaskIds(taskData)) {
						if (subId == null || subId.trim().equals("")) {
							continue;
						}
						AbstractTask subTask = createTaskFromExistingId(connector, repository, subId, false,
								new SubProgressMonitor(monitor, 1));
						if (subTask != null) {
							taskList.addTask(subTask, repositoryTask);
						}
					}
				}
			}
		} finally {
			monitor.done();
		}
		return repositoryTask;
	}

	/**
	 * Create new repository task, adding result to tasklist
	 */
	private AbstractTask createTaskFromExistingId(AbstractLegacyRepositoryConnector connector,
			TaskRepository repository, String id, boolean retrieveSubTasks, IProgressMonitor monitor)
			throws CoreException {
		AbstractTask repositoryTask = (AbstractTask) taskList.getTask(repository.getRepositoryUrl(), id);
		if (repositoryTask == null && connector.getLegacyTaskDataHandler() != null) {
			RepositoryTaskData taskData = null;
			taskData = connector.getLegacyTaskDataHandler().getTaskData(repository, id,
					new SubProgressMonitor(monitor, 1));
			if (taskData != null) {
				repositoryTask = createTaskFromTaskData(connector, repository, taskData, retrieveSubTasks,
						new SubProgressMonitor(monitor, 1));
				if (repositoryTask != null) {
					repositoryTask.setSynchronizationState(SynchronizationState.INCOMING);
					taskList.addTask(repositoryTask);
				}
			}
		} // TODO: Handle case similar to web tasks (no taskDataHandler but
		// have tasks)

		return repositoryTask;
	}

}
