package notification

import notification.model.Notification

import scala.concurrent.Future

//TODO: wire in non actor logging
trait DummyConnector {
  var sendCount: Int

  // fails on 1st call, successful on 2nd, fails on 3rd, ... etc etc
  def dummyPost(notification: Notification): Future[Unit] = {
    if (sendCount % 2 == 0) {
      println(s"dummyPostUsingSomeAsyncHttpClientApi - successful post sendCount=$sendCount")
      Future.successful(())
    } else {
      println(s"dummyPostUsingSomeAsyncHttpClientApi - failed post. sendCount=$sendCount")
      //Future.failed(new IllegalStateException("Some really shitty HTTP client exception"))
      throw new IllegalStateException(s"Some really shitty HTTP client exception sendCount=$sendCount")
    }
  }
}
