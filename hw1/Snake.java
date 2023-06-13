package hw1;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Snake {

    public static void main(String[] args) {
        // 創建一個 ExecutorService 使用 newCachedThreadPool
        ExecutorService executorService = Executors.newCachedThreadPool();
        
        // 提交任務到 ExecutorService
        executorService.submit(() -> {
            new GameFrame();
        });
        
        // 關閉 ExecutorService
        executorService.shutdown();
    }
}