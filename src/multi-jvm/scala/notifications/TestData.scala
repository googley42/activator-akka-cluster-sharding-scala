package notifications

import notification.NotificationsActor.NotificationsState
import notification.model.Notification

object TestData {
  val clientId = "10000"
  val NotificationOne = Notification("SOME-PAYLOAD1", "some/url1")
  val NotificationTwo = Notification("SOME-PAYLOAD2", "some/url2")
  val EmptyNotificationsState = NotificationsState(Nil)
  val OneNotificationsState = NotificationsState(List(NotificationOne))
  val TwoNotificationsState = NotificationsState(List(NotificationOne, NotificationTwo))
}
