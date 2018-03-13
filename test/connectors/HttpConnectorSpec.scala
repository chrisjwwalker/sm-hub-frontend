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

package connectors

import java.net.ConnectException

import common.{AmberResponse, GreenResponse, Http, RedResponse}
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.{when, reset}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.ws.WSResponse
import play.api.test.{DefaultAwaitTimeout, FutureAwaits}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.TimeoutException

class HttpConnectorSpec extends PlaySpec with MockitoSugar with FutureAwaits with DefaultAwaitTimeout with BeforeAndAfterEach {

  val mockHttpClient = mock[Http]

  val testConnector = new HttpConnector {
    override val http = mockHttpClient
  }

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockHttpClient)
  }

  val OK = 200
  val INS = 500
  val testPort = 9000

  def fakeResponse(statusInt: Int, bodyIn: String = ""): WSResponse = new WSResponse {
    override def cookie(name: String) = ???
    override def underlying[T]        = ???
    override def body                 = bodyIn
    override def bodyAsBytes          = ???
    override def cookies              = ???
    override def allHeaders           = ???
    override def xml                  = ???
    override def statusText           = ???
    override def json                 = ???
    override def header(key: String)  = ???
    override def status               = statusInt
  }

  "pingService" should {
    "return a GreenResponse" when {
      "a call to a services ping ping has been successful" in {
        when(mockHttpClient.get(ArgumentMatchers.any()))
          .thenReturn(Future(fakeResponse(OK)))

        val result = await(testConnector.pingService(testPort))
        result mustBe GreenResponse
      }
    }

    "return a RedResponse" when {
      "a ConnectException was thrown" in {
        when(mockHttpClient.get(ArgumentMatchers.any()))
          .thenReturn(Future.failed(new ConnectException()))

        val result = await(testConnector.pingService(testPort))
        result mustBe RedResponse
      }

      "the status code was anything other than an OK" in {
        when(mockHttpClient.get(ArgumentMatchers.any()))
          .thenReturn(Future(fakeResponse(INS)))

        val result = await(testConnector.pingService(testPort))
        result mustBe RedResponse
      }
    }

    "return an AmberResponse" when {
      "a TimeoutException was thrown" in {
        when(mockHttpClient.get(ArgumentMatchers.any()))
          .thenReturn(Future.failed(new TimeoutException()))

        val result = await(testConnector.pingService(testPort))
        result mustBe AmberResponse
      }
    }
  }

  "getBodyOfPage" should {
    "parse a response and return a Jsoup document" in {
      val ids = Map(
        "title" -> "Directory listing for /assets/",
        "v1"    -> "2.214.0/",
        "v2"    -> "2.211.0/"
      )

      val htmlResponse =
        """
          |<body>
          |<h2 id="title">Directory listing for /assets/</h2>
          |<hr>
          |<ul>
          |<li><a id="v1" href="2.214.0">2.214.0/</a>
          |<li><a id="v2" href="2.211.0">2.211.0/</a>
          |</ul>
          |<hr>
          |</body>
        """.stripMargin

      val testDoc = Jsoup.parse(htmlResponse.toString)

      when(mockHttpClient.get(ArgumentMatchers.any()))
        .thenReturn(Future(fakeResponse(OK, htmlResponse.toString)))

      val result = await(testConnector.getBodyOfPage(1234, "/test"))
      ids.map {
        case (id, text) =>
          result.getElementById(id).text mustBe text
      }
    }
  }
}
