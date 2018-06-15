# akka-cluster

# Steps

sbt 'runMain sample.blog.BlogApp 2551'
sbt 'runMain sample.blog.BlogApp 2552'
sbt 'runMain sample.blog.BlogApp 0'

- example
    - run example on multiple terminal windows
    - understand output
    - understand output after node failure
- make sender more robust    
- enhance traffic generator
- wire into notifications
    

TODO
- phase1 - existing example
    - refine solution OUTSIDE of an endpoint
        - what is best way to bootstrap Sharded actor system?
        - how to determine what node an entity is running one?
    - manually test node outage
    - create a `Bot2` to fire load
- phase2 - `customs-notification`
    - create a branch
    - add cluster/shard stuff
    - create solution
    - test with harness 

DONE
- existing example
    - keep existing logic to keep tests running
    - upgrade example to latest akka version - fix anything that breaks
    - add a noddy Notifications persistent actor and wire into a `ShardRegion`     

# Startup logging output with multiple nodes

got error:

    [error] (run-main-0) java.lang.ClassNotFoundException: scala.Int
    java.lang.ClassNotFoundException: scala.Int
    [trace] Stack trace suppressed: run last compile:runMain for the full output.

FIXED by upping from 

    sbt.version=0.13.8 to sbt.version=0.13.13
    
Tried to run example as per instructions but got persistence errors - I guess from levelDB. After I switched to mongo these went away.   

To run example on 3 nodes:

Start 1st node

    sbt 'runMain sample.blog.BlogApp 2551'

This gives output 

```
[info] Running sample.blog.BlogApp 2551
[INFO] [06/15/2018 10:02:12.541] [run-main-0] [akka.remote.Remoting] Starting remoting
[INFO] [06/15/2018 10:02:12.681] [run-main-0] [akka.remote.Remoting] Remoting started; listening on addresses :[akka.tcp://ClusterSystem@127.0.0.1:2551]
[INFO] [06/15/2018 10:02:12.692] [run-main-0] [akka.cluster.Cluster(akka://ClusterSystem)] Cluster Node [akka.tcp://ClusterSystem@127.0.0.1:2551] - Starting up...
[INFO] [06/15/2018 10:02:12.760] [run-main-0] [akka.cluster.Cluster(akka://ClusterSystem)] Cluster Node [akka.tcp://ClusterSystem@127.0.0.1:2551] - Registered cluster JMX MBean [akka:type=Cluster]
[INFO] [06/15/2018 10:02:12.760] [run-main-0] [akka.cluster.Cluster(akka://ClusterSystem)] Cluster Node [akka.tcp://ClusterSystem@127.0.0.1:2551] - Started up successfully
```

however complains about other seed node not available

```
[WARN] [06/15/2018 10:12:49.867] [ClusterSystem-akka.remote.default-remote-dispatcher-6] [akka.tcp://ClusterSystem@127.0.0.1:2551/system/endpointManager/reliableEndpointWriter-akka.tcp%3A%2F%2FClusterSystem%40127.0.0.1%3A2552-0] Association with remote system [akka.tcp://ClusterSystem@127.0.0.1:2552] has failed, address is now gated for [5000] ms. Reason: [Association failed with [akka.tcp://ClusterSystem@127.0.0.1:2552]] Caused by: [Connection refused: /127.0.0.1:2552]
[INFO] [06/15/2018 10:12:49.989] [ClusterSystem-akka.actor.default-dispatcher-18] [akka://ClusterSystem/deadLetters] Message [akka.cluster.InternalClusterAction$InitJoin$] from Actor[akka://ClusterSystem/system/cluster/core/daemon/firstSeedNodeProcess-1#586167805] to Actor[akka://ClusterSystem/deadLetters] was not delivered. [1] dead letters encountered. This logging can be turned off or adjusted with configuration settings 'akka.log-dead-letters' and 'akka.log-dead-letters-during-shutdown'.
```

Then you get output for singleton coordinator

```
[INFO] [06/15/2018 10:12:55.721] [ClusterSystem-akka.actor.default-dispatcher-18] [akka.tcp://ClusterSystem@127.0.0.1:2551/system/sharding/PostCoordinator] Singleton manager starting singleton actor [akka://ClusterSystem/system/sharding/PostCoordinator/singleton]
[INFO] [06/15/2018 10:12:55.723] [ClusterSystem-akka.actor.default-dispatcher-4] [akka.tcp://ClusterSystem@127.0.0.1:2551/system/sharding/AuthorListingCoordinator] Singleton manager starting singleton actor [akka://ClusterSystem/system/sharding/AuthorListingCoordinator/singleton]
[INFO] [06/15/2018 10:12:55.723] [ClusterSystem-akka.actor.default-dispatcher-18] [akka.tcp://ClusterSystem@127.0.0.1:2551/system/sharding/PostCoordinator] ClusterSingletonManager state change [Start -> Oldest]
[INFO] [06/15/2018 10:12:55.725] [ClusterSystem-akka.actor.default-dispatcher-4] [akka.tcp://ClusterSystem@127.0.0.1:2551/system/sharding/AuthorListingCoordinator] ClusterSingletonManager state change [Start -> Oldest]
```

Start 2nd node 

    sbt 'runMain sample.blog.BlogApp 2552'
    
```
[INFO] [06/15/2018 10:16:06.201] [run-main-0] [akka.remote.Remoting] Starting remoting
[INFO] [06/15/2018 10:16:06.337] [run-main-0] [akka.remote.Remoting] Remoting started; listening on addresses :[akka.tcp://ClusterSystem@127.0.0.1:2552]
[INFO] [06/15/2018 10:16:06.349] [run-main-0] [akka.cluster.Cluster(akka://ClusterSystem)] Cluster Node [akka.tcp://ClusterSystem@127.0.0.1:2552] - Starting up...
[INFO] [06/15/2018 10:16:06.419] [run-main-0] [akka.cluster.Cluster(akka://ClusterSystem)] Cluster Node [akka.tcp://ClusterSystem@127.0.0.1:2552] - Registered cluster JMX MBean [akka:type=Cluster]
[INFO] [06/15/2018 10:16:06.419] [run-main-0] [akka.cluster.Cluster(akka://ClusterSystem)] Cluster Node [akka.tcp://ClusterSystem@127.0.0.1:2552] - Started up successfully
[INFO] [06/15/2018 10:16:07.019] [ClusterSystem-akka.actor.default-dispatcher-2] [akka.cluster.Cluster(akka://ClusterSystem)] Cluster Node [akka.tcp://ClusterSystem@127.0.0.1:2552] - Welcome from [akka.tcp://ClusterSystem@127.0.0.1:2551]
[INFO] [06/15/2018 10:16:07.274] [ClusterSystem-akka.actor.default-dispatcher-23] [akka.tcp://ClusterSystem@127.0.0.1:2552/system/sharding/PostCoordinator] ClusterSingletonManager state change [Start -> Younger]
[INFO] [06/15/2018 10:16:07.275] [ClusterSystem-akka.actor.default-dispatcher-18] [akka.tcp://ClusterSystem@127.0.0.1:2552/system/sharding/AuthorListingCoordinator] ClusterSingletonManager state change [Start -> Younger]
```    

On 1st node we get 

```
[INFO] [06/15/2018 10:16:06.873] [ClusterSystem-akka.actor.default-dispatcher-4] [akka.cluster.Cluster(akka://ClusterSystem)] Cluster Node [akka.tcp://ClusterSystem@127.0.0.1:2551] - Node [akka.tcp://ClusterSystem@127.0.0.1:2552] is JOINING, roles []
[INFO] [06/15/2018 10:16:06.922] [ClusterSystem-akka.actor.default-dispatcher-20] [akka.cluster.Cluster(akka://ClusterSystem)] Cluster Node [akka.tcp://ClusterSystem@127.0.0.1:2551] - Leader is moving node [akka.tcp://ClusterSystem@127.0.0.1:2552] to [Up]
```

    sbt 'runMain sample.blog.BlogApp 0'
    
Start 3rd node
    
```
[INFO] [06/15/2018 10:20:05.304] [ClusterSystem-akka.actor.default-dispatcher-4] [akka.cluster.Cluster(akka://ClusterSystem)] Cluster Node [akka.tcp://ClusterSystem@127.0.0.1:37815] - Welcome from [akka.tcp://ClusterSystem@127.0.0.1:2552]
[INFO] [06/15/2018 10:20:06.894] [ClusterSystem-akka.actor.default-dispatcher-2] [akka.tcp://ClusterSystem@127.0.0.1:37815/system/sharding/PostCoordinator] ClusterSingletonManager state change [Start -> Younger]
[INFO] [06/15/2018 10:20:06.894] [ClusterSystem-akka.actor.default-dispatcher-17] [akka.tcp://ClusterSystem@127.0.0.1:37815/system/sharding/AuthorListingCoordinator] ClusterSingletonManager state change [Start -> Younger]
[INFO] [06/15/2018 10:20:16.980] [ClusterSystem-akka.actor.default-dispatcher-23] [akka.tcp://ClusterSystem@127.0.0.1:37815/user/bot] Posts by Martin: 
	Post 1 from ClusterSystem@127.0.0.1:42507
	Post 6 from ClusterSystem@127.0.0.1:42507
	Post 1 from ClusterSystem@127.0.0.1:38471
	Post 1 from ClusterSystem@127.0.0.1:37815
[INFO] [06/15/2018 10:20:20.035] [ClusterSystem-akka.actor.default-dispatcher-23] [akka.tcp://ClusterSystem@127.0.0.1:37815/system/sharding/Post/51/a2d2ff61-e9f5-4686-adbe-e35ab1c9d245] New post saved: Post 2 from ClusterSystem@127.0.0.1:37815
```    

Bot starts send messages which can be seen on nodes 1 and 2. Stop node 1

I get persistence related error:

```
[ERROR] [06/15/2018 10:46:31.246] [ClusterSystem-akka.actor.default-dispatcher-30] [akka.tcp://ClusterSystem@127.0.0.1:44349/system/sharding/AuthorListingCoordinator/singleton/coordinator] Persistence failure when replaying events for persistenceId [/sharding/AuthorListingCoordinator]. Last known sequence number [0] (akka.pattern.CircuitBreakerOpenException)
```

# Docs

- akka official docs
    - [Cluster Specification](https://doc.akka.io/docs/akka/current/common/cluster.html)
    - [Cluster Usage](https://doc.akka.io/docs/akka/current/cluster-usage.html)
    - [persistence + sharding example code](https://github.com/typesafehub/activator-akka-cluster-sharding-scala)
    

# TODO

- Questions
    - what is dynamic solution to `seed-nodes` ?
    - what functionality does mongodb journal have ?
        - does it have `distributed journal` ?
    - what is the entry point for an Actor to associate with a node?
        - [A] `ShardRegion`
    - do we need a **root** actor? Technically we could use `ShardRegion` directly
        - [A] better to shield the user from sharding concerns. Also rood actor is a good place to publish interface MSGs for user
    - does the sharding example have use any [1] `akka-clustering` specific interfaces? or does it use solely [2]`akka-clustering-sharding`?
        - I think it uses mainly [2]
    - define pattern for child actor sending that can survive restarts of parent entity
        - [A] cluster does not handle this - so this is a vanilla akka problem to solve
    - akka docs say do not use auto downing for production `akka.cluster.auto-down-unreachable-after = 120s` 
         - so what are the alternatives? manual commands to cluster?
    - SBT - what is `MultiJvm` setting?    
    - distributed journal - WTF are the implementations?
        - levelDb is single point of failure
    
# Concepts of cluster

In the short term we can focus more on the sharding side, them come back to this to understand the underpinnings

- AkkaSystem WRT cluster
- Node
- Cluster
- role?

simple cluster app `akka-samples-cluster-scala`

# Sharding

given persistence ID, there will only ever be one actor instance currently in memory in the cluster to represent that persistence ID

"One of the most important concepts in starting up a ShardRegion actor is the definition of the extractEntityId and extractShardId 
partial functions. These are partial functions that you will need to implement, per sharded entity, to arrive on the entity ID 
and the shard ID per request message, which those entities handle. To extract the entity ID, this means that any message that 
gets forwarded through the ShardRegion must carry an entity identifier on it"


```Scala
val postRegion = ClusterSharding(context.system).shardRegion(Post.shardName)
``` 

tree below

- Shard Co-ordinator - runs as ShardSingleton
- ShardRegion (Actor)
    - Shard   (Supervisor Actor - has default (restarting) strategy) 
        - Entity


- persistence id
```
override def persistenceId: String = self.path.parent.name + "-" + self.path.name
```
- passivation
    - sharding has built in passivation mechanism/msg which is sent to parent(which is Shard) - it then sends enclosed message 
    back to child
    "To support graceful passivation without losing such messages the entity actor can send 
    ShardRegion.Passivate to its parent Shard. The specified wrapped message in Passivate will be sent back to the entity, 
    which is then supposed to stop itself. Incoming messages will be buffered by the Shard between reception of Passivate and 
    termination of the entity. Such buffered messages are thereafter delivered to a new incarnation of the entity."
    
        - akka.cluster.sharding.ShardRegion.Passivate
        ```Scala
        // passivate the entity when no activity
        context.setReceiveTimeout(2.minutes)
        // ....  
        case ReceiveTimeout => context.parent ! Passivate(stopMessage = PoisonPill)
        ```

- persistence of sharding system data - 2 modes
    - `akka.cluster.sharding.state-store-mode = ddata`
    - `akka.cluster.sharding.state-store-mode = persistence`

## **location transparency**
    - local akka already forces you to think about issues like asynchronicity, serialization, and non-guaranteed message delivery,
      so switch to cluster is transparent.
    - opposite of RMI `transparent remoting`  

## running a simple app

### akka-cluster examples:
    
The `libsigar-amd64-linux.so` lib gets put under following path after SBT build:
    
    target/native/libsigar-amd64-linux.so    
    
- you need to pass in `-Djava.library.path=./native` else you get error
```
[error] no libsigar-amd64-linux.so in java.library.path
[error] org.hyperic.sigar.SigarException: no libsigar-amd64-linux.so in java.library.path
[error] 	at org.hyperic.sigar.Sigar.loadLibrary(Sigar.java:174)
[error] 	at org.hyperic.sigar.Sigar.<clinit>(Sigar.java:102)
[error] 	at akka.cluster.metrics.SigarProvider.verifiedSigarInstance(Provision.scala:47)
```    

eg    

    sbt -Djava.library.path=./native "runMain sample.cluster.simple.SimpleClusterApp 0"    
    
## getting started

- seed nodes
    - static
    ```
    seed-nodes = [
      "akka.tcp://ClusterSystem@127.0.0.1:2551",
      "akka.tcp://ClusterSystem@127.0.0.1:2552"]
    ```
    - dynaminc - 3rd party APIs used for discovery
        
- https://blog.codecentric.de/en/2016/01/getting-started-akka-cluster/
    
- downing a failed/failing node (so auto downing for spike is a good idea)
    - If this node is not downed, then it will more than likely form a cluster of one, as it will see the rest of 
      the cluster as unreachable too and then become its own leader
    - however with auto downing
        - It works great for node crashes, but not so well when you run into a network partition.
        - That's why it's best in production to not use auto-downing
          
## akka persistence mongo
          
- https://github.com/scullxbones/akka-persistence-mongo/blob/master/docs/akka25.md
    - create branch `mongo` in `https://github.com/googley42/notifications/tree/mongo`       
    - `reactivemongo` get not such method error which implies different versions of AKKA on classpath
    - `casbah` worked first time
    ```
    used customs-akka-persistence
    db.dropDatabase()
    ```

          
          