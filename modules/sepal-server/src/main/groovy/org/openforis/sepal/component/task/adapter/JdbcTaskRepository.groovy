package org.openforis.sepal.component.task.adapter

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.sql.GroovyRowResult
import groovy.sql.Sql
import org.openforis.sepal.component.task.api.Task
import org.openforis.sepal.component.task.api.TaskRepository
import org.openforis.sepal.component.task.api.Timeout
import org.openforis.sepal.transaction.SqlConnectionManager
import org.openforis.sepal.util.Clock

import static org.openforis.sepal.component.task.api.Task.State.ACTIVE
import static org.openforis.sepal.component.task.api.Task.State.PENDING


class JdbcTaskRepository implements TaskRepository {
    private final SqlConnectionManager connectionManager
    private final Clock clock

    JdbcTaskRepository(SqlConnectionManager connectionManager, Clock clock) {
        this.connectionManager = connectionManager
        this.clock = clock
    }

    void insert(Task task) {
        def taskParams = JsonOutput.toJson(task.params)
        sql.executeInsert('''
                INSERT INTO task(id, state, username, session_id, operation, params, status_description, creation_time, update_time, removed)
                VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, FALSE)''', [
                task.id, task.state.name(), task.username, task.sessionId, task.operation, taskParams,
                task.statusDescription, task.creationTime, task.updateTime
        ])
    }

    void update(Task task) {
        sql.executeUpdate('''
                UPDATE task
                SET state = ?, status_description = ?, update_time = ?
                WHERE id = ?''', [task.state.name(), task.statusDescription, clock.now(), task.id])
    }

    void remove(Task task) {
        sql.execute('UPDATE task SET removed = TRUE WHERE id = ?', [task.id])
    }

    void removeNonPendingOrActiveUserTasks(String username) {
        sql.execute('''
                UPDATE task
                SET removed = TRUE
                WHERE username = ?
                AND state NOT IN (?, ?)''', [username, PENDING.name(), ACTIVE.name()])
    }

    Task getTask(String taskId) {
        def row = sql.firstRow('''
                SELECT id, state, username, session_id, operation, params, status_description, creation_time, update_time
                FROM task
                WHERE id = ?''', [taskId])
        if (!row)
            throw new IllegalStateException("Non-existing task: $taskId")
        return toTask(row)
    }

    List<Task> timedOutTasks() {
        def now = clock.now()
        sql.rows('''
                SELECT id, state, username, session_id, operation, params, status_description, creation_time, update_time
                FROM task
                WHERE (state = ? AND update_time < ?)
                OR (state = ? AND update_time < ?)
            ''', [
                PENDING.name(), Timeout.PENDING.lastValidUpdate(now),
                ACTIVE.name(), Timeout.ACTIVE.lastValidUpdate(now)
        ]).collect { toTask(it) }
    }

    List<Task> pendingOrActiveTasksInSession(String sessionId) {
        sql.rows('''
                SELECT id, state, username, session_id, operation, params, status_description, creation_time, update_time
                FROM task
                WHERE session_id = ?
                AND state IN (?, ?)''', [sessionId, PENDING.name(), ACTIVE.name()])
                .collect { toTask(it) }
    }

    List<Task> userTasks(String username) {
        sql.rows('''
                SELECT id, state, username, session_id, operation, params, status_description, creation_time, update_time
                FROM task
                WHERE username = ?
                AND REMOVED = FALSE''', [username])
                .collect { toTask(it) }
    }


    List<Task> pendingOrActiveUserTasks(String username) {
        sql.rows('''
                SELECT id, state, username, session_id, operation, params, status_description, creation_time, update_time
                FROM task
                WHERE username = ?
                AND state IN (?, ?)''', [username, PENDING.name(), ACTIVE.name()])
                .collect { toTask(it) }
    }

    private Task toTask(GroovyRowResult row) {
        def state = row.state as Task.State
        new Task(
                id: row.id,
                state: state,
                username: row.username,
                operation: row.operation,
                params: new JsonSlurper().parseText(row.params) as Map,
                sessionId: row.session_id,
                statusDescription: row.status_description ?: state.description,
                creationTime: row.creation_time,
                updateTime: row.update_time
        )
    }

    private Sql getSql() {
        connectionManager.sql
    }
}