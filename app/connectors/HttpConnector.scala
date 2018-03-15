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
<<<<<<< HEAD
=======
<<<<<<< HEAD
<<<<<<< HEAD
import java.util.concurrent.TimeoutException
=======
>>>>>>> running services, searching, ports, GHE, AF, test routes
=======
import java.util.concurrent.TimeoutException
>>>>>>> Fix to cater for exclusions
import javax.inject.Inject
>>>>>>> Fix to cater for exclusions

import common._
import javax.inject.Inject
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.utils.Colors

import scala.concurrent.ExecutionContext.Implicits.global
<<<<<<< HEAD
import scala.concurrent.{Future, TimeoutException}
=======
<<<<<<< HEAD
<<<<<<< HEAD
import scala.concurrent.Future
=======
import scala.concurrent.{Future, TimeoutException}
>>>>>>> running services, searching, ports, GHE, AF, test routes
=======
import scala.concurrent.Future
>>>>>>> Fix to cater for exclusions
>>>>>>> Fix to cater for exclusions

class DefaultHttpConnector @Inject()(val http: Http) extends HttpConnector

trait HttpConnector extends Logging {
  val http: Http

  def getBodyOfPage(port: Int, path: String): Future[Document] = {
    http.get(s"http://localhost:$port$path").map(resp => Jsoup.parse(resp.body))
  }

  def pingService(service: String, url: String, port: Int): Future[RunningResponse] = {
    http.get(url) map { x =>
      if(x.status == 200) GreenResponse else RedResponse
    } recover {
      case _: ConnectException =>
        logger.error(s"Could not connect to ${Colors.yellow(service)} on port ${Colors.magenta(port.toString)}")
        RedResponse
      case _: TimeoutException =>
        logger.warn(s"Connection to ${Colors.yellow(service)} on port ${Colors.magenta(port.toString)} timed out")
        AmberResponse
    }
  }
}
