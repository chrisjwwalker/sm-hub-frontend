/*
 * Copyright 2018 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package common

import javax.inject.Inject
import org.slf4j.{Logger, LoggerFactory}
import play.api.http.HttpErrorHandler
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{RequestHeader, Result}
import play.api.mvc.Results._
import play.api.http.Status.NOT_FOUND
import play.utils.Colors
import views.html.errors.{NotFoundView, StandardErrorView}

import scala.concurrent.Future

class DefaultErrorHandler @Inject()(val messagesApi: MessagesApi) extends ErrorHandler

trait ErrorHandler extends HttpErrorHandler with I18nSupport {

  val logger: Logger = LoggerFactory.getLogger(getClass)

  override def onClientError(request: RequestHeader, statusCode: Int, message: String): Future[Result] = {
    logger.error(s"- [onClientError] ${Colors.yellow(request.method)} request to ${Colors.red(request.uri)} returned a ${Colors.red(statusCode.toString)}")
    implicit val rh: RequestHeader = request
    statusCode match {
      case NOT_FOUND  => Future.successful(NotFound(NotFoundView()))
      case _          => Future.successful(Status(statusCode)(StandardErrorView()))
    }
  }

  override def onServerError(request: RequestHeader, exception: Throwable): Future[Result] = {
    logger.error(s"- [onServerError] - exception : $exception")
    implicit val rh: RequestHeader = request
    exception.printStackTrace()
    Future.successful(InternalServerError(StandardErrorView()))
  }
}