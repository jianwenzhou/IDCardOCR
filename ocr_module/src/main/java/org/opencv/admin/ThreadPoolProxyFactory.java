package org.opencv.admin;


/**
 * 作者：Zhou on 2018/1/25 14:03
 * 简介：生成线程池工厂，采用单例模式
 */
public class ThreadPoolProxyFactory {

   private volatile static ThreadPoolProxy mThreadProxy;


    /**
     * 返回一个线程池代理
     *
     * @return
     */
    public static ThreadPoolProxy createThreadProxy(int threads) {
        if (mThreadProxy == null) {
            synchronized (ThreadPoolProxy.class) {
                if (mThreadProxy == null) {
                    mThreadProxy = new ThreadPoolProxy(threads);
                }
            }
        }
        return mThreadProxy;
    }


}
