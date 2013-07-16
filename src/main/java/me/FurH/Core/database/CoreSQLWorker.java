package me.FurH.Core.database;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import me.FurH.Core.exceptions.CoreException;

/**
 *
 * @author FurmigaHumana All Rights Reserved unless otherwise explicitly stated.
 */
public class CoreSQLWorker extends Thread {

    private BlockingQueue<Runnable> queue = new LinkedBlockingQueue<Runnable>();

    private AtomicBoolean sleep = new AtomicBoolean(false);
    private AtomicBoolean kill = new AtomicBoolean(false);

    private final Object lock       = new Object();
    private boolean commited        = false;
    private int     queue_runs      = 0;

    private final CoreSQLDatabase db;

    public CoreSQLWorker(CoreSQLDatabase db, String name) {
        this.db = db;
        this.setName(name);
        this.setPriority(Thread.MIN_PRIORITY);
        this.setDaemon(true);
        this.start();
    }

    public void enqueue(Runnable command) {
        queue.add(command); wake();
    }

    @Override
    public void run() {

        while (!kill.get()) {

            if (queue.isEmpty()) {
                sleep(); continue;
            }

            Runnable task = queue.poll();

            if (task == null) {
                sleep(); continue;
            }

            queue_runs++;
            task.run();
            commited = false;

            if (queue_runs >= getQueueSpeed()) {

                queue_runs = 0;

                try {
                    db.commit();
                } catch (CoreException ex) {
                    ex.printStackTrace();
                }
                
                try {
                    sleep(5000);
                } catch (InterruptedException ex) { }
            }
        }
        
        this.interrupt();
    }
    
    public void shutdown() {
        kill.set(true);
    }

    public boolean isShutdown() {
        return kill.get();
    }

    public List<Runnable> shutdownNow() {
        kill.set(true);

        List<Runnable> ret = new ArrayList<Runnable>();

        Iterator<Runnable> it = queue.iterator();
        while (it.hasNext()) {
            ret.add(it.next()); it.remove();
        }

        return ret;
    }
    
    public void sleep() {
        
        if (sleep.get()) {
            return;
        }
        
        sleep.set(true);
        
        if (!commited) {
            try {
                db.commit(); commited = true;
            } catch (CoreException ex) {
                ex.printStackTrace();
            }
        }
        
        try {

            synchronized (lock) {
                lock.wait();
            }

        } catch (Throwable ex) {
            ex.printStackTrace();
        }
    }

    public void wake() {
        
        if (!sleep.get()) {
            return;
        }
        
        sleep.set(false);
        
        try {

            synchronized (lock) {
                lock.notify();
            }

        } catch (Throwable ex) {
            ex.printStackTrace();
        }
    }

    public int getQueueSpeed() {

        int count = (int) (((double) queue.size()) * db.queue_speed);
        
        if (count < 100) {
            count = 100;
        }
        
        if (count > 10000) {
            count = 10000;
        }
        
        return count;
    }

    public int size() {
        return queue.size();
    }
}
