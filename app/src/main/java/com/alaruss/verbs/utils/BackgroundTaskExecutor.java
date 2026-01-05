package com.alaruss.verbs.utils;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Replacement for deprecated AsyncTask
 * Provides similar functionality using ExecutorService and Handler
 */
public class BackgroundTaskExecutor {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainThreadHandler = new Handler(Looper.getMainLooper());

    public interface OnProgressListener {
        void onProgress(int progress);
    }

    public interface BackgroundTask<T> {
        T execute(OnProgressListener listener);
    }

    public interface OnCompleteListener<T> {
        void onComplete(T result);
    }

    public <T> void execute(
            BackgroundTask<T> task,
            OnProgressListener progressListener,
            OnCompleteListener<T> completeListener
    ) {
        executor.execute(() -> {
            // Execute background task
            T result = task.execute(progress -> {
                // Post progress updates to main thread
                mainThreadHandler.post(() -> {
                    if (progressListener != null) {
                        progressListener.onProgress(progress);
                    }
                });
            });

            // Post completion to main thread
            mainThreadHandler.post(() -> {
                if (completeListener != null) {
                    completeListener.onComplete(result);
                }
            });
        });
    }

    public void shutdown() {
        executor.shutdown();
    }
}
