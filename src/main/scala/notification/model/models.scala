package notification.model

trait NotificationCmd {
  val clientId: ClientId
}

case class Notification(payload: String, url: String)

