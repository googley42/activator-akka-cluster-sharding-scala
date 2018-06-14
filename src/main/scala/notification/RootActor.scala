package notification

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import notification.NotificationsActor.{EnqueueCmd, QueryNotificationsCmd, notificationsActorId}
import notification.RootActor.{NotificationsMsg, SendNotificationMsg}
import notification.model.{Notification, NotificationCmd}

object RootActor {
  case class NotificationEnqueuedAck(id: String)
  case class SendNotificationMsg(clientId: String, notification: Notification)
  case class NotificationsMsg(clientId: String)

  def props: Props = Props(classOf[RootActor]) //TODO: do we need a pass in a unique name?
}

//
// TODO: wire into ShardRegion
//
class RootActor extends Actor with ActorLogging {
  override def receive: Receive = {
    case SendNotificationMsg(clientId, notification) =>
      forwardCommand(EnqueueCmd(clientId, notification))
      log.info("forwarded notification to aggregate root for clientId {}", clientId)
    case NotificationsMsg(clientId) =>
      log.info("PRE forwarded notifications query")
      forwardCommand(QueryNotificationsCmd(clientId))
      log.info("forwarded notifications query to aggregate root for clientId {}", clientId)
  }

  /**
    * Looks up the entity child for the supplied id and then
    * forwards the supplied message to it
    * @param msg The message to forward
    */
  def forwardCommand(msg: NotificationCmd): Unit = {
    val child = lookupOrCreateChild(msg.id)
    log.info("about to forward msg {} to actor {}", msg, child.toString())
    // Forwards the message and passes the original sender actor as the sender.
    // Works, no matter whether originally sent with tell/'!' or ask/'?'.
    child.forward(msg)
  }

  protected def lookupOrCreateChild(clientId: String): ActorRef = {
    val id = notificationsActorId(clientId)
    context.child(id).fold {
      log.info("pre - context = {}", context.children)
      log.info("Creating new NotificationsActor actor to handle a request for clientId {}", clientId)
      val child = context.actorOf(entityProps(clientId), id)
      log.info("post - context = {}", context.children)
      log.info("Created.")
      child
    }{ ref: ActorRef =>
      log.info("Actor already created, returning ref")
      ref
    }
  }

  def entityProps(clientId: String): Props = NotificationsActor.props
}
