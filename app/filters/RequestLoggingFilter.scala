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

package filters

import javax.inject.Inject

import akka.stream.Materializer
import common.Logging
import org.joda.time.DateTimeUtils
import play.api.mvc.{Filter, RequestHeader, Result}
import play.utils.Colors

import scala.concurrent.Future
import scala.language.implicitConversions
import scala.concurrent.ExecutionContext.Implicits.global

class DefaultRequestLoggingFilter @Inject()(implicit val mat: Materializer) extends RequestLoggingFilter

trait RequestLoggingFilter extends Filter with Logging {

  private implicit def numberToString[T](number: T): String = number.toString

  private def getElapsedTime(start: Long): Long = DateTimeUtils.currentTimeMillis - start

  private def createLogMessage(result: Future[Result], rh: RequestHeader, start: Long): Future[String] = result map { res =>
    s"${Colors.yellow(rh.method.capitalize)} request to ${Colors.green(rh.uri)} returned a ${Colors.cyan(res.header.status)} and took ${Colors.magenta(s"${getElapsedTime(start)}ms")}"
  }

  override def apply(f: RequestHeader => Future[Result])(rh: RequestHeader): Future[Result] = {
    val result = f(rh)

    if(!rh.uri.contains("assets")) {
      createLogMessage(result, rh, DateTimeUtils.currentTimeMillis()) map logger.info
    }
    result
  }
}
