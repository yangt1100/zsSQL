package com.zssql.common.utils;

import javafx.concurrent.Task;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TaskUtils {
    private static ExecutorService threadPool = Executors.newFixedThreadPool(20);

    public static void submit(Task<?> task) {
        threadPool.submit(task);
    }
}
