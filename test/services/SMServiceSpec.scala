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

package services

import connectors.{HttpConnector, JsonConnector}
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec

class SMServiceSpec extends PlaySpec with MockitoSugar {

  val mockHttpConnector = mock[HttpConnector]
  val mockJsonConnector = mock[JsonConnector]

  val testPort = 9973

  val testService = new SMService {
    override val httpConnector = mockHttpConnector
    override val jsonConnector = mockJsonConnector
  }

  "getRunningServices" should {
    "return an empty seq" when {
      "no profile is provided" in {
        assert(testService.getRunningServices().isEmpty)
      }
    }

    "return a sequence of String -> RunningResponses" in {
      val result = testService.getRunningServices()
    }
  }
}
