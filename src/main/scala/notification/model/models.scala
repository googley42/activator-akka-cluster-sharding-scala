package notification.model

trait NotificationCmd {
  val id: ClientId
}

case class Notification(payload: String, url: String)

