package app.shazin.aharemen;
import java.util.*;
import java.util.concurrent.*;

public final class ParallelCallsTest {
    public static void main(String[] args) throws Exception {
        ExecutorService pool=Executors.newFixedThreadPool(4),coordinator=Executors.newSingleThreadExecutor();
        try {
            CountDownLatch started=new CountDownLatch(4),release=new CountDownLatch(1);
            List<Callable<Integer>> calls=new ArrayList<>();
            for(int i=0;i<4;i++){final int value=i;calls.add(()->{started.countDown();if(!release.await(3,TimeUnit.SECONDS))throw new Exception("Reads were serialized");return value;});}
            Future<List<Integer>> batch=coordinator.submit(()->ParallelCalls.run(pool,calls));
            try {if(!started.await(2,TimeUnit.SECONDS))throw new AssertionError("All four reads must start before any finishes");}
            finally {release.countDown();}
            if(!batch.get(3,TimeUnit.SECONDS).equals(Arrays.asList(0,1,2,3)))throw new AssertionError("Results lost endpoint order");
            CountDownLatch siblingStarted=new CountDownLatch(1),cancelled=new CountDownLatch(1);
            List<Callable<Integer>> failing=Arrays.asList(
                ()->{if(!siblingStarted.await(2,TimeUnit.SECONDS))throw new AssertionError("Sibling never started");throw new Exception("offline");},
                ()->{siblingStarted.countDown();try{new CountDownLatch(1).await();return 1;}catch(InterruptedException e){cancelled.countDown();throw e;}});
            try {ParallelCalls.run(pool,failing);throw new AssertionError("Failure must propagate");}
            catch(ExecutionException expected){if(!"offline".equals(expected.getCause().getMessage()))throw expected;}
            if(!cancelled.await(2,TimeUnit.SECONDS))throw new AssertionError("Failed batch left sibling running");
            System.out.println("4 parallel-read scheduling, ordering, failure and cancellation checks passed.");
        } finally {pool.shutdownNow();coordinator.shutdownNow();}
    }
}
