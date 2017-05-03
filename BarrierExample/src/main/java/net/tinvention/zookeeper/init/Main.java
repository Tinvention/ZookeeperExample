package net.tinvention.zookeeper.init;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import ch.qos.logback.core.joran.spi.JoranException;
import net.tinvention.zookeeper.dal.Barrier;

public class Main {
  private static final String zkQuorum = "127.0.0.1:2181,127.0.0.1:2182,127.0.0.1:21813";
  private static final String barrierNode = "/barrierNode";
  private static final int numProcesses = 3;

  public static void main(String[] args) throws InterruptedException, JoranException, ExecutionException {
    Barrier a = new Barrier(zkQuorum, barrierNode, numProcesses);
    Barrier b = new Barrier(zkQuorum, barrierNode, numProcesses);
    Barrier c = new Barrier(zkQuorum, barrierNode, numProcesses);

    ExecutorService executorService = Executors.newFixedThreadPool(numProcesses);
    List<Future<Boolean>> results = executorService.invokeAll(Arrays.asList(new Barrier[] { a, b, c }));

    while (!(results.get(0).isDone() && results.get(1).isDone() && results.get(2).isDone())) {
      Thread.sleep(200);
    }
    executorService.shutdown();
    executorService.awaitTermination(20, TimeUnit.SECONDS);
  }

}
