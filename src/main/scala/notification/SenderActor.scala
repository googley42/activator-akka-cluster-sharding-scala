package notification

import akka.actor.{Actor, ActorLogging, ActorRef, PoisonPill, Props}
import notification.NotificationsActor.SendAckCmd
import notification.SenderActor.SendMsg
import notification.model.Notification

import scala.concurrent.ExecutionContext.Implicits.global

object SenderActor {
  val MaxRetries = 1
  // Messages
  //TODO: keep reference to original sender
  case class SendMsg(notification: Notification, sendCount: Int = 0, originalSender: ActorRef)

  def props: Props = Props(classOf[SenderActor]) //TODO: do we need a pass in a unique name?
}

class SenderActor extends Actor with ActorLogging with DummyConnector {
  var sendCount = 0

  override def postStop(): Unit = {
    log.info("about to stop")
    super.postStop()
  }

  override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
    log.info("in preRestart")
    message.fold{
      val msg = "WTF! This should never happen!"
      log.error(msg)
      throw new RuntimeException(msg)
    }{message =>
      val sendMsg = message.asInstanceOf[SendMsg]
      val sendMsg2 = sendMsg.copy(sendCount = this.sendCount)
      log.info("sending failed message {} to myself", sendMsg2)
      self ! sendMsg2
    }

    super.preRestart(reason, message)
  }

  override def receive: Receive = {
    case SendMsg(notification, count, originalSender) => {
      log.info(s"PRE sendCount=$sendCount")
      this.sendCount = count + 1
      log.info(s"POST sendCount=$sendCount")
      dummyPost(notification).map{ _ =>
        originalSender ! SendAckCmd("TODO: get clientId", notification) //TODO
        self ! PoisonPill
      }
    }
  }

}
