package net.tinvention.zookeeper.dal;

import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SyncPrimitive implements Watcher {
  private static final Logger logger = LoggerFactory.getLogger(SyncPrimitive.class);

  private static final int defaultTimeout = 2000;

  protected static ZooKeeper zookeeper;
  protected static Integer mutex;

  public SyncPrimitive(String zkQuorum) throws InterruptedException {
    if (zookeeper == null) {
      try {
        logger.info("Initializing new ZooKeeper for {}", zkQuorum);
        zookeeper = new ZooKeeper(zkQuorum, defaultTimeout, this);
        mutex = new Integer(-1);
        logger.info("Initialized new ZooKeeper for {}", zkQuorum);
      } catch (Exception e) {
        logger.error("Woops!", e);
        zookeeper = null; // TODO: shouldn't we use the .close method?
      }
    }
    if (zookeeper != null) {
      while (!zookeeper.getState().isConnected()) {
        Thread.sleep(20);
      }
    }
  }

  @Override
  synchronized public void process(WatchedEvent event) {
    synchronized (mutex) {
      mutex.notify();
    }
  }

}
