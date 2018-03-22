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

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}
import org.scalatestplus.play.PlaySpec
import play.api.libs.ws.ahc.AhcWSClient
import play.api.mvc.{Headers, RequestHeader, Result, Results}
import play.api.test.Helpers._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class RequestLoggingFilterSpec extends PlaySpec {

  implicit val system       = ActorSystem()
  implicit val materialiser = ActorMaterializer()
  val ws                    = AhcWSClient()

  val testFilter = new RequestLoggingFilter {
    override implicit def mat: Materializer = materialiser
  }

  val okFunction: RequestHeader => Future[Result] = req => Future(Results.Ok("Hello"))

  "RequestLoggingFilter" should {
    "log and return an ok" in {
      val requestHeader = new RequestHeader {
        override def clientCertificateChain = ???
        override def secure                 = ???
        override def path                   = ???
        override def id                     = ???
        override def remoteAddress          = ???
        override def headers                = Headers("status" -> "200")
        override def method                 = "GET"
        override def queryString            = ???
        override def uri                    = "/test/uri"
        override def version                = ???
        override def tags                   = ???
      }

      val result = testFilter.apply(okFunction)(requestHeader)
      status(result)          mustBe OK
      contentAsString(result) mustBe "Hello"
    }

    "not log and return an ok" in {
      val requestHeader = new RequestHeader {
        override def clientCertificateChain = ???
        override def secure                 = ???
        override def path                   = ???
        override def id                     = ???
        override def remoteAddress          = ???
        override def headers                = Headers("status" -> "200")
        override def method                 = "GET"
        override def queryString            = ???
        override def uri                    = "/test/assets"
        override def version                = ???
        override def tags                   = ???
      }

      val result = testFilter.apply(okFunction)(requestHeader)
      status(result)          mustBe OK
      contentAsString(result) mustBe "Hello"
    }
  }
}
