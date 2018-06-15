# akka-cluster

# Steps

TODO
- phase1 - existing example
    - refine solution OUTSIDE of an endpoint
        - what is best way to bootstrap Sharded actor system?
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
          
          
          
          