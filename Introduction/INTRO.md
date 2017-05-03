### ZOOKEEPER

#### What is ZooKeeper?
ZooKeeper is a high-performance coordination service for distributed applications.

    1. It exposes common services - such as naming, configuration management, synchronization, and group services - in a simple interface so you don't have to write them from scratch.
    2. You can use it off-the-shelf to implement consensus, group management, leader election, and presence protocols.
    3. And you can build on it for your own, specific needs.

#### Note
This information was taken from the ZooKeeper documentation at https://zookeeper.apache.org/doc/trunk/<br>
Please refer to that site for futher information.

#### Install ZooKeeper

    1. Download and install the latest archive somewhere on the local file system
    2. Installed at /usr/zookeeper/latest -> /usr/zookeeper/zookeeper-x.y.z
       [Note: /opt/zookeeper/... might be a better choice]
    3. Configuration directory is /usr/zookeeper/latest/conf -> /etc/zookeeper/conf
       [should contain log4j.properties, zoo.cfg, zookeeper-env.sh, configuration.xsl]
    4. Data directory is /data/user1/zookeeper [should contain the 'myid' file]
    5. Log directory is /var/log/zookeeper
    6. In case of a single host, multi-server ensemble (only used for development purposes)
       append the 'server_id' at the end of the above paths
       Example:
             -> conf_dir = /etc/zookeeper/conf/zk1
             -> data_dir = /data/user1/zookeeper/zk1
             -> log_dir  = /var/log/zookeeper/zk1

#### Design goals

    1. Simple:
       * Processes coordinate through a namespace similar to a standard file system.
         This name space consists of data registers ["znodes" ~ files and directories] which are kept in memory.
         Note: znodes are NOT meant for storage
    2. Replicated:
       * It is replicated across an "ensemble" of servers
       * The servers should all know about each other
       * Each server mantains an in-memory image of state, while transation logs and snapshots are stored in a persistent store
       * Each client connects to a single server through a TCP connection.
         If the connection breaks, the client will attepmt to connect to another server of the ensemble
    3. Ordered:
       * Each update is stamped with a number that reflects the order of zookeeper transactions
    4. Fast:
       * It performs best when workloads are "read-dominant"

#### Data Model and hierarchical namespace

    1. Similar to a standard file system. A name is a sequence of path elements separated by "/".
       Every node is identified by a path and is referred to as "znode"
    2. There is no difference between files and directories: each znode can both have children and contain data
    3. Znodes mantain a stat system that includes: version numbers for data and ACL changes, timestamps
    4. Each znode has an ACL list
    5. Reads and writes are atomic
    6. Emepheral nodes are znodes that are automatically deleted when the session that created them ends
    7. Clients can watch znodes: a watch is triggered and removed when the znode changes

#### Guarantees

    1. Sequential Consistency - Updates from a client will be applied in the order that they were sent
    2. Atomicity - Updates either succeed or fail. No partial results
    3. Single System Image - A client will see the same view of the service regardless of the server that it connects to
    4. Reliability - Once an update has been applied, it will persist from that time forward until a client overwrites the update
    5. Timeliness - The clients view of the system is guaranteed to be up-to-date within a certain time bound

#### API
Zookeeper was designed to have a very simple programming interface. Thus only the following operations are supported:

    1. create -> creates a node at a location in the tree
    2. delete -> deletes a node
    3. exists -> tests if a node exists at a location
    4. get data -> reads the data from a node
    5. set data -> writes data to a node
    6. get children -> retrieves a list of children of a node
    7. sync -> waits for data to be propagated

These basic operations can (and should) be used to implements higer level operations base on the needs.

#### Implementation overview
A zookeeper service is made of three high level components: a request processor, an atomic broadcast and a replicated database.
Each server replicates its own copy of the last two components.<br>
See https://zookeeper.apache.org/doc/trunk/images/zkcomponents.jpg to get an idea of how these components are connected to each other.

    1. Replicated database:
       * Stored in-memory and contains the whole tree
       * Updates are log to disk for recoverability
       * Writes are persisted to disk before they are applied to the in-memory database
       * Each server services read requests from the local replica of this database
    2. Request Processor:
       * Write request from clients are forwarded to the leader server which propagates the transations to the other followers
       * The messaging layer is atomic. This guarantees that the replicated databases never diverge

### API (TODO)

Packages:

    * org.apache.zookeeper
    * org.apache.zookeeper.data

Classes:

    * ZooKeeper / ZooKeeper(session_id,session_pwd)
        |-> IO thread (java.nio) -> session maintenance (reconnect,heartbear,etc), synchronous responses
        |-> Event thread         -> event callbacks<br>
    * Watcher
    
Notes:

    1. All completions for asynchronous calls and watcher callbacks will be made in order, one at a time.
       The caller can do any processing they wish, but no other callbacks will be processed during that time
    2. Callbacks do not block the processing of the IO thread or the processing of the synchronous calls
