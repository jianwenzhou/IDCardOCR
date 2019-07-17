package org.opencv.admin;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 作者：smile on 2018/1/22 10:56
 * 简介：代理模式 创建线程池
 */
public class ThreadPoolProxy {

    private volatile ThreadPoolExecutor mExecutor;
    private int mCorePoolSize;
    private int mMaximumPoolSize;

    /**
     * @param nThreads
     *         传递核心数以及最大数进来即可
     */
    public ThreadPoolProxy(int nThreads) {
        mCorePoolSize = nThreads;
        mMaximumPoolSize = nThreads;
    }

    /**
     * 创建对应的线程池对象
     */
    private void initThreadPoolExecutor() {
        //双重检查加锁,只有在第一次实例化的时候才启用同步机制,提高了性能
        if (mExecutor == null || mExecutor.isShutdown() || mExecutor.isTerminated()) {
            synchronized (ThreadPoolProxy.class) {
                if (mExecutor == null || mExecutor.isShutdown() || mExecutor.isTerminated()) {
                    long keepAliveTime = 5000;
                    TimeUnit unit = TimeUnit.MILLISECONDS;
                    BlockingQueue<Runnable> workQueue = new LinkedBlockingQueue<>();
                    ThreadFactory threadFactory = Executors.defaultThreadFactory();
                    RejectedExecutionHandler handler = new ThreadPoolExecutor.DiscardPolicy();
                    mExecutor = new ThreadPoolExecutor(
                            mCorePoolSize,//核心池大小
                            mMaximumPoolSize,//最大线程数
                            keepAliveTime,//保持时间
                            unit,//时间单位
                            workQueue,//任务队列
                            threadFactory,//线程工厂
                            handler//异常捕获器
                    );
                }
            }
        }
    }
    /*
    执行任务和提交任务的区别?
        execute->没有返回值
        submit-->有返回值
    返回的Future对象是什么?
        1.Future接收任务执行完成之后结果
        2.提供了方法,可以检查任务是否执行完成,等待完成,结果结果
        3.其中get方法可以接收结果,阻塞等待任务执行完成
         4.cancle方法,可以取消执行
     */

    /**
     * 执行任务(任务交给线程池执行)
     */
    public void execute(Runnable task) {
        initThreadPoolExecutor();
        mExecutor.execute(task);
    }

    /**
     * 提交任务(任务提交给线程池执行)
     */
    public Future<?> submit(Runnable task) {
        initThreadPoolExecutor();
        return mExecutor.submit(task);
    }

    /**
     * 移除任务(从线程池里面移除任务)
     */
    public void remove(Runnable task) {
        initThreadPoolExecutor();
        mExecutor.remove(task);
    }

    /**
     * 移除所有任务(从线程池里面移除任务)
     */
    public void shutdown() {
        if (mExecutor != null) {
            mExecutor.shutdown();
        }
    }

    /**
     * @return 返回true ，说明线程池内所有任务执行完毕
     */
    public boolean isTerminated() {
        boolean terminated = false;
        if (mExecutor != null) {
            terminated = mExecutor.isTerminated();
        }
        return terminated;
    }
}
