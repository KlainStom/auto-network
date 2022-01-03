package com.github.klainstom.autonetwork;

public class RepeatingTask {
    private final int repeat;
    private final Runnable task;
    private Thread thread = null;
    private boolean running;

    public RepeatingTask(int repeat, Runnable task) {
        this.repeat = repeat;
        this.task = task;
    }

    public void schedule() {
        running = true;
        thread = new Thread(new Task());
        thread.start();
    }
    public void cancel() {
        running = false;
        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private class Task implements Runnable {
        @Override
        public void run() {
            try {
                long loops = 0;
                while (running) {
                    Thread.sleep(repeat * 100L);
                    loops++;
                    if (loops >= 10) {
                        task.run();
                        loops = 0;
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
