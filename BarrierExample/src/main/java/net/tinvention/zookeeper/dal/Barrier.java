package net.tinvention.zookeeper.dal;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.concurrent.Callable;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A barrier is a primitive that enables a group of processes to synchronize the beginning and the
 * end of a computation.<br>
 * The general idea of this implementation is to have a barrier node that serves the purpose of
 * being a parent for individual process nodes.<br>
 * Suppose that we call the barrier node "/b1". Each process "p" then creates a node "/b1/p".<br>
 * Once enough processes have created their corresponding nodes, joined processes can start the
 * computation.
 */
public class Barrier extends SyncPrimitive implements Callable<Boolean> {
  private static final Logger logger = LoggerFactory.getLogger(Barrier.class);

  private String barrierNode;
  private int numProcesses;

  private String name;

  @Override
  public Boolean call() {
    if (zookeeper == null || barrierNode == null || name == null) {
      throw new RuntimeException("Barrier not initialized properly");
    }
    try {
      boolean entered = this.enter();
      if (!entered) {
        logger.warn("Could not enter Barrier");
        return false;
      } else {
        Thread.sleep(1000);
        logger.info("{} Up and running!", name);

        boolean exited = this.exit();
        if (!exited) {
          logger.warn("Could not exit Barrier");
          return false;
        }
        return true;
      }
    } catch (Exception e) {
      throw new RuntimeException("Woops!", e);
    }
  }

  public Barrier(String zkQuorum, String barrierNode, int numProcesses) throws InterruptedException {
    super(zkQuorum);
    this.barrierNode = barrierNode;
    this.numProcesses = numProcesses;

    // Create the barrier node if it doesn't exist
    if (zookeeper != null) {
      try {
        Stat stat = zookeeper.exists(barrierNode, false);
        if (stat == null) {
          zookeeper.create(barrierNode, new byte[0], Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
          logger.info("Barrier {} created", barrierNode);
        } else {
          logger.info("Barrier {} already exists", barrierNode);
        }
      } catch (KeeperException e1) {
        logger.error("Woops_1!", e1);
      } catch (InterruptedException e2) {
        logger.error("Woops_2!", e2);
      }
    } else {
      logger.warn("No ZooKeeper available");
    }

    // Initialize my name
    try {
      this.name = InetAddress.getLocalHost().getCanonicalHostName();
    } catch (UnknownHostException e3) {
      logger.warn("Woops_3!", e3);
    }
  }

  public boolean enter() throws KeeperException, InterruptedException {
    String path = zookeeper.create(barrierNode + "/" + name, new byte[0], Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
    logger.info("Created path: {}", path);
    this.name = path;
    while (true) {
      synchronized (mutex) {
        List<String> children = zookeeper.getChildren(barrierNode, true); // NOTE: we are setting a
                                                                          // watch here. Will it be
                                                                          // triggered even if the
                                                                          // method returns true?
                                                                          // Need to check.
        if (children.size() < numProcesses) {
          mutex.wait();
        } else {
          return true;
        }
      }
    }
  }

  public boolean exit() throws KeeperException, InterruptedException {
    // delete my node and wait until all other processes have exited
    zookeeper.delete(name, 0);
    logger.info("{} exiting", name);
    while (true) {
      synchronized (mutex) {
        List<String> children = zookeeper.getChildren(barrierNode, true);
        if (children.size() > 0) {
          mutex.wait();
        } else {
          logger.info("{} exited", name);
          return true;
        }
      }
    }

  }
}
