package app.shazin.aharemen;

import java.util.*;
import java.util.concurrent.*;

/** Runs independent reads together; never used for transactions. */
final class ParallelCalls {
    static <T> List<T> run(ExecutorService executor,List<Callable<T>> calls) throws Exception {
        List<Future<T>> pending=new ArrayList<>();
        try {
            for(Callable<T> call:calls)pending.add(executor.submit(call));
            List<T> results=new ArrayList<>();
            for(Future<T> result:pending)results.add(result.get());
            return results;
        } finally {
            for(Future<T> result:pending)if(!result.isDone())result.cancel(true);
        }
    }
}
