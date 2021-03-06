package notification

import java.util.concurrent.TimeUnit

import akka.actor.SupervisorStrategy.{Escalate, Restart}
import akka.actor._
import akka.persistence.{PersistentActor, SnapshotOffer}
import com.typesafe.config.Config
import notification.NotificationsActor._
import notification.RootActor.NotificationEnqueuedAck
import notification.model.{ClientId, Notification, NotificationCmd}
import akka.cluster.sharding.ShardRegion
import akka.cluster.sharding.ShardRegion.Passivate

import scala.concurrent.duration._
import scala.language.postfixOps

/*
Things I have had to change from vanilla akka solution
- persistenceId contains `self.path.name`
- on ReceiveTimeout we send wrapped msg parent to context.parent ! Passivate(stopMessage = PoisonPill)
- root actor gets reference to this entity via `ShardRegion`
- passivation MSG is sent to special `Shard` supervisor

*/
//TODO rename to ClientNotificationQueueActor
object NotificationsActor {
  val ShardName = "Notifications"

  @deprecated("use ShardRegion entry point instead")
  def notificationsActorId(clientId: ClientId) = s"NotificationsActor:$clientId"

  val idExtractor: ShardRegion.ExtractEntityId = {
    case cmd: NotificationCmd => (cmd.clientId, cmd)
  }

  val shardResolver: ShardRegion.ExtractShardId = {
    case cmd: NotificationCmd => (math.abs(cmd.clientId.hashCode) % 100).toString
  }

  // commands
  case class EnqueueCmd(clientId: ClientId, notification: Notification) extends NotificationCmd
  case class QueryNotificationsCmd(clientId: ClientId) extends NotificationCmd
  case class SendAckCmd(clientId: ClientId, notification: Notification) extends NotificationCmd

  // events
  case class EnqueuedEvt(notification: Notification)
  case object NotificationsEvt
  case class SentAckEvt(notification: Notification)

  case class NotificationsState(events: List[Notification] = Nil) {
    def updated(evt: EnqueuedEvt): NotificationsState = copy(evt.notification :: events)
    def size: Int = events.length
    def remove(evt: SentAckEvt) = copy(events = this.events.filterNot(_ == evt.notification))
    override def toString: String = events.reverse.toString
  }

  def props: Props = Props(classOf[NotificationsActor])
}

//TODO: passivate after timeout to preserve resources
class NotificationsActor() extends PersistentActor with ActorLogging {

  // self.path.parent.name is the type name (utf-8 URL-encoded)
  // self.path.name is the entry identifier (utf-8 URL-encoded)
  override def persistenceId: String = "Notifications" + "-" + self.path.name

  var state = NotificationsState()

  //Using this scheduled task as the passivation mechanism
  context.setReceiveTimeout(getPersistentEntityTimeout(context.system.settings.config, TimeUnit.SECONDS))

  private def getPersistentEntityTimeout(config: Config, timeUnit: TimeUnit): Duration =
    Duration.create(config.getDuration("persistent-entity-timeout", TimeUnit.SECONDS), timeUnit)

  override val supervisorStrategy =
    OneForOneStrategy(maxNrOfRetries = 2, withinTimeRange = 1 minute) {
      case _: IllegalStateException     ⇒ Restart
      case _: Exception                ⇒ Escalate
    }

  def enqueue(event: EnqueuedEvt): Unit =
    state = state.updated(event)

  def remove(event: SentAckEvt): Unit =
    state = state.remove(event)

  val receiveRecover: Receive = {
    case evt: EnqueuedEvt =>
      enqueue(evt)
    case evt: SentAckEvt =>
      remove(evt)
    case SnapshotOffer(_, snapshot: NotificationsState) =>
      state = snapshot
  }

  val receiveCommand: Receive = {
    case EnqueueCmd(_, notification) =>
      persist(EnqueuedEvt(notification)){ event =>
        enqueue(event)
        context.system.eventStream.publish(event)
        log.info(state.toString)
        log.info(s"EnqueueCmd sender().toString() = ${sender().toString()}")
        sendNotification(notification) //TODO: think about doing this in another message handler on a message to self
        sender() ! NotificationEnqueuedAck("conversationId")  // Note sender() is originating sender. TODO: return some unique id for ACK
      }
    case QueryNotificationsCmd =>
      persist(NotificationsEvt){ event =>
        context.system.eventStream.publish(event)
        log.info(state.toString)
        log.info(s"QueryNotificationsCmd sender().toString() = ${sender().toString()}")
        sender() ! state
      }
    case SendAckCmd(_, notification) =>
      persist(SentAckEvt(notification)){ event =>
        context.system.eventStream.publish(event)
        log.info(s"SendAckCmd sender().toString() = ${sender().toString()}")
        log.info(s"State PRE remove size = ${state.size}")
        remove(event)
        log.info(s"State POST remove size = ${state.size}")
      }
    //Have been idle too long, time to start passivation process
    case ReceiveTimeout =>
      log.info("Notifications entity with id {} is being passivated due to inactivity", persistenceId)
      context.parent ! Passivate(stopMessage = PoisonPill)
    case "print" => println(state)
  }

  def sendNotification(n: Notification): Unit = {
    val senderActorRef = context.actorOf(SenderActor.props) // TODO: give a unique name
    senderActorRef ! SenderActor.SendMsg(notification = n, originalSender = self)
  }
}
