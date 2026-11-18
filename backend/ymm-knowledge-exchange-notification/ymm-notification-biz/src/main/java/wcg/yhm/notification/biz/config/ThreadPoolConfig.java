package wcg.yhm.notification.biz.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-01-22
 * @Description:
 * @Version: 1.0
 */

@Configuration
public class ThreadPoolConfig {
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        //创建线程池实例，spring提供的封装类基于jdk实现
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        //设置核心线程数为10， 这10个线程会一直存在线程池中，即使是空闲状态，也就是说最少有10个线程存活
        executor.setCorePoolSize(10);
        //设置这个线程池中最多允许有50个线程，当10个核心线程都在执行时就会创建新的线程，创建数不允许超过50
        executor.setMaxPoolSize(50);
        //设置任务队列容量为200，表示当所有线程都在执行时，会将任务暂存到这个队列中直到队列满为止
        executor.setQueueCapacity(200);
        //设置拒绝策略。当线程池达到最大线程数并且队列已满时，任务会被拒绝。CallerRunsPolicy 是一种拒绝策略，它会将任务返回给调用者线程执行，避免任务丢失。
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        //设置除了核心线程数10个以外的线程如果执行完任务最多存活30秒
        executor.setKeepAliveSeconds(30);
        //设置线程池中的线程名前缀
        executor.setThreadNamePrefix("Notification-");
        //表示线程池会等待所有任务执行完后才会关闭线程池
        executor.setWaitForTasksToCompleteOnShutdown(true);
        //(关闭过程中刚好出现新任务)表示设置线程池在关闭时等待任务完成的最大时间，超过这个时间后，不论是否完成任务都会强制关闭线程池
        executor.setAwaitTerminationSeconds(60);
        //初始化线程池，必须调用此方法才能使配置生效并启动线程池。
        executor.initialize();
        return executor;
    }
}
    
