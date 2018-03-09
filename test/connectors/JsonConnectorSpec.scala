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

import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsObject, Json}

class JsonConnectorSpec extends PlaySpec {

  val servicesJson = """
                       |{
                       |  "testService1" : {},
                       |  "testService2" : {},
                       |  "testService3" : {}
                       |}
                     """.stripMargin

  val profilesJson = """
                       |{
                       |  "testProfile1" : [],
                       |  "testProfile2" : [],
                       |  "testProfile3" : []
                       |}
                     """.stripMargin

  val testConnector = new JsonConnector {
    override val pathToSM = "testPath"
    override val homeDir  = "testHomeDir"
    override def sourceFileJson(fileName: String): String = {
      fileName match {
        case "services" => servicesJson
        case "profiles" => profilesJson
      }
    }
  }

  "loadServicesJson" should {
    "return a json obj" in {
      testConnector.loadServicesJson mustBe Json.parse(servicesJson).as[JsObject]
    }
  }

  "loadProfilesJson" should {
    "return a json obj" in {
      testConnector.loadProfilesJson mustBe Json.parse(profilesJson).as[JsObject]
    }
  }
}
