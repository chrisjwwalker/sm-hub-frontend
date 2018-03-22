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

import javax.inject.Inject

import play.api.Configuration
import play.api.libs.json.{JsObject, Json}

import scala.io.Source

// $COVERAGE-OFF$
class DefaultJsonConnector @Inject()(configuration: Configuration) extends JsonConnector {
  val pathToSM: String = {
    val path = configuration.underlying.getString("smPath")
    if(path.endsWith("/")) path else path.concat("/")
  }

  override def sourceFileJson(fileName: String): String = {
    Source.fromFile(s"$pathToSM$fileName.json").getLines().mkString
  }
}
// $COVERAGE-ON$

trait JsonConnector {
  def sourceFileJson(fileName: String): String

  def loadServicesJson: JsObject = loadAndParse("services")
  def loadProfilesJson: JsObject = loadAndParse("profiles")

  private def loadAndParse(fileName: String): JsObject = {
    Json.parse(sourceFileJson(fileName)).as[JsObject]
  }
}
