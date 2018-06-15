package notification

import akka.actor.ActorSystem
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings}
import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import notification.RootActor.SendNotificationMsg
import notification.model.Notification

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._

object NotificationApp {
  def main(args: Array[String]): Unit = {
    if (args.isEmpty)
      startup(Seq("2551", "2552", "0"))
    else
      startup(args)
  }

  def startup(ports: Seq[String]): Unit = {
    ports foreach { port =>
      // Override the configuration of the port
      val config = ConfigFactory.parseString("akka.remote.netty.tcp.port=" + port).
        withFallback(ConfigFactory.load())

      // Create an Akka system
      val clusterSystem = ActorSystem("ClusterSystem", config)

//      startupSharedJournal(clusterSystem, startStore = (port == "2551"), path =
//        ActorPath.fromString("akka.tcp://ClusterSystem@127.0.0.1:2551/user/store"))

      ClusterSharding(clusterSystem).start(
        typeName = NotificationsActor.ShardName,
        entityProps = NotificationsActor.props,
        settings = ClusterShardingSettings(clusterSystem),
        extractEntityId = NotificationsActor.idExtractor,
        extractShardId = NotificationsActor.shardResolver)

      if (port != "2551" && port != "2552") {
        Thread.sleep(5000)
        sendMsgsToRootActor(clusterSystem)
      }
    }

    def sendMsgsToRootActor(system: ActorSystem) = {
      val clientId = "10000"

      println("1. Getting root actor")
      val rootActor = system.actorOf(RootActor.props, "rootActor")
      println("2. Root actor found " + rootActor)

      implicit val timeout: Timeout = Timeout(2 seconds)

      def futureAsk: Future[Unit] = {
        val f = rootActor ? SendNotificationMsg(clientId, Notification("SOME-PAYLOAD", "some/url"))
        f.map(msg => println("Application got acknowledgement " + msg))
      }

      futureAsk

      Thread.sleep(5000)
      system.terminate() // TODO: how to terminate a cluster?
      Thread.sleep(5000)
    }

//    def startupSharedJournal(system: ActorSystem, startStore: Boolean, path: ActorPath): Unit = {
//      // Start the shared journal one one node (don't crash this SPOF)
//      // This will not be needed with a distributed journal
//      if (startStore)
//        system.actorOf(Props[SharedLeveldbStore], "store")
//      // register the shared journal
//      implicit val timeout = Timeout(15.seconds)
//      val f = (system.actorSelection(path) ? Identify(None))
//      f.onSuccess {
//        case ActorIdentity(_, Some(ref)) => SharedLeveldbJournal.setStore(ref, system)
//        case _ =>
//          system.log.error("Shared journal not started at {}", path)
//          system.terminate()
//      }
//      f.onFailure {
//        case _ =>
//          system.log.error("Lookup of shared journal at {} timed out", path)
//          system.terminate()
//      }
//    }

  }

}

