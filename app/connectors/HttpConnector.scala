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
import javax.inject.Inject

import common._
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, TimeoutException}

class DefaultHttpConnector @Inject()(val http: Http) extends HttpConnector

trait HttpConnector {
  val http: Http

  def getBodyOfPage(port: Int, path: String): Future[Document] = {
    http.get(s"http://localhost:$port$path").map(resp => Jsoup.parse(resp.body))
  }

  def pingService(port: Int): Future[RunningResponse] = {
    http.get(s"http://localhost:$port/ping/ping") map { x =>
      if(x.status == 200) GreenResponse else RedResponse
    } recover {
      case _: ConnectException => RedResponse
      case _: TimeoutException => AmberResponse
    }
  }
}
