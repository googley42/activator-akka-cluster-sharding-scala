//package notifications
//
//import akka.actor.{ActorRef, ActorSystem, PoisonPill}
//import akka.testkit.{ImplicitSender, TestKit}
//import notification.NotificationsActor
//import notification.NotificationsActor._
//import notification.RootActor.NotificationEnqueuedAck
//import notifications.TestData.{NotificationOne, NotificationTwo}
//import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers, WordSpec}
//
//
//class NotificationsActorSpec extends WordSpec with Matchers with BeforeAndAfterAll {
//  implicit val system = ActorSystem()
//
//  val clientId = "1000"
//
//  class SetUp extends TestKit(system) with ImplicitSender {
//    val notifications: ActorRef = system.actorOf(NotificationsActor.props)
//  }
//
//  override def afterAll(): Unit = {
//    system.terminate()
//  }
//
//  /*
//  We test persistent actors using the following steps:
//  - Create the actor you want to test.
//  - Send commands to your actor. The actor will persist the events triggered by these commands.
//  - Restart the actor.
//  - Test the actor's state. It should be the same as before restarting it.
//   */
//  "NotificationsActor" should {
//    "enqueue" in new SetUp {
//      notifications ! EnqueueCmd(clientId, NotificationOne)
//      expectMsg(NotificationEnqueuedAck("conversationId"))
//
//      notifications ! EnqueueCmd(clientId, NotificationTwo)
//      expectMsg(NotificationEnqueuedAck("conversationId"))
//
//      notifications ! PoisonPill
//
//      val notificationsAfterRestart: ActorRef = system.actorOf(NotificationsActor.props)
//
//      notificationsAfterRestart ! QueryNotificationsCmd
//      expectMsg(NotificationsState(List(NotificationTwo, NotificationOne)))
//
//      notificationsAfterRestart ! SendAckCmd(clientId, NotificationOne)
//
//      notifications ! PoisonPill
//
//      val notificationsAfterRestart2: ActorRef = system.actorOf(NotificationsActor.props)
//
//      // this demonstrated outcome of the replay of enqueue and remove events
//      // to prove receiveRecover processing is working
//      notificationsAfterRestart2 ! QueryNotificationsCmd
//      expectMsg(NotificationsState(List(NotificationTwo)))
//    }
//  }
//}
