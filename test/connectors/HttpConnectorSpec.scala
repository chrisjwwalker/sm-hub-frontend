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
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.ws.WSResponse
import play.api.test.{DefaultAwaitTimeout, FutureAwaits}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.TimeoutException

class HttpConnectorSpec extends PlaySpec with MockitoSugar with FutureAwaits with DefaultAwaitTimeout {

  val mockHttpClient = mock[Http]

  val testConnector = new HttpConnector {
    override val http = mockHttpClient
  }

  val OK = 200
  val INS = 500
  val testPort = 9000

  val successResponse = new WSResponse {
    override def cookie(name: String) = ???
    override def underlying[T]        = ???
    override def body                 = ???
    override def bodyAsBytes          = ???
    override def cookies              = ???
    override def allHeaders           = ???
    override def xml                  = ???
    override def statusText           = ???
    override def json                 = ???
    override def header(key: String)  = ???
    override def status               = OK
  }

  val failResponse = new WSResponse {
    override def cookie(name: String) = ???
    override def underlying[T]        = ???
    override def body                 = ???
    override def bodyAsBytes          = ???
    override def cookies              = ???
    override def allHeaders           = ???
    override def xml                  = ???
    override def statusText           = ???
    override def json                 = ???
    override def header(key: String)  = ???
    override def status               = INS
  }

  "pingService" should {
    "return a GreenResponse" when {
      "a call to a services ping ping has been successful" in {
        when(mockHttpClient.get(ArgumentMatchers.any()))
          .thenReturn(Future(successResponse))

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
          .thenReturn(Future(failResponse))

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
}
