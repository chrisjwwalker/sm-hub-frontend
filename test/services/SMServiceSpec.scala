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

import common.{AmberResponse, GreenResponse, RedResponse}
import connectors.{HttpConnector, JsonConnector}
import models.TestRoutesDesc
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json
import org.mockito.Mockito.{reset, when}
import org.mockito.ArgumentMatchers
import org.scalatest.BeforeAndAfterEach

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class SMServiceSpec extends PlaySpec with MockitoSugar with BeforeAndAfterEach {

  val mockHttpConnector = mock[HttpConnector]
  val mockJsonConnector = mock[JsonConnector]

  val testService = new SMService {
    override val httpConnector = mockHttpConnector
    override val jsonConnector = mockJsonConnector
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(
      mockJsonConnector,
      mockHttpConnector
    )
  }

  val profilesJson = Json.obj(
    "ALL"          -> Json.arr("testService1", "testService2", "testService3", "testService4"),
    "TESTPROFILE1" -> Json.arr("testService1", "testService2", "testService3"),
    "TESTPROFILE2" -> Json.arr("1", "1")
  )

  val servicesJson = Json.obj(
    "testService1" -> Json.obj("defaultPort" -> 1024),
    "testService2" -> Json.obj("defaultPort" -> 1025),
    "testService3" -> Json.obj("defaultPort" -> 1026),
    "testService4" -> Json.obj(
      "defaultPort" -> 1026,
      "testRoutes"  -> Json.arr(
        Json.obj(
          "name"        -> "testRoute",
          "route"       -> "/test/uri",
          "description" -> "testDesc"
        )
      )
    ),
    "RABBITMQ"     -> Json.obj()
  )

  "getRunningServices" should {
    "return an empty seq" when {
      "no profile is provided" in {
        assert(testService.getRunningServices().isEmpty)
      }

      "no profile is found" in {
        assert(testService.getRunningServices(currentProfile = "test-profile-not-found").isEmpty)
      }
    }

    "return a sequence of String -> RunningResponses" in {
      when(mockJsonConnector.loadProfilesJson)
        .thenReturn(profilesJson)

      when(mockJsonConnector.loadServicesJson)
        .thenReturn(servicesJson)

      when(mockHttpConnector.pingService(ArgumentMatchers.any()))
        .thenReturn(
          Future(GreenResponse),
          Future(AmberResponse),
          Future(RedResponse)
        )

      val result = testService.getRunningServices(currentProfile = "testProfile1")
      result mustBe Seq("testService1@1024" -> GreenResponse, "testService2@1025" -> AmberResponse, "testService3@1026" -> RedResponse)
    }
  }

  "getValidPortNumbers" should {
    "return an empty seq" when {
      "no range is provided" in {
        testService.getValidPortNumbers(None) mustBe Seq()
      }
    }

    "return a seq of ints" when {
      "a range has been provided" in {
        when(mockJsonConnector.loadServicesJson)
          .thenReturn(servicesJson)

        val result = testService.getValidPortNumbers(Some(1024 -> 1030))
        result mustBe Seq(1027, 1028, 1029, 1030)
      }
    }
  }

  "getAllProfiles" should {
    "return a seq of string" in {
      when(mockJsonConnector.loadProfilesJson)
        .thenReturn(profilesJson)

      testService.getAllProfiles mustBe Seq("TESTPROFILE1", "TESTPROFILE2")
    }
  }

  "getAllServices" should {
    "return all services in a seq" in {
      when(mockJsonConnector.loadServicesJson)
        .thenReturn(servicesJson)

      testService.getAllServices mustBe Seq("testService1", "testService2", "testService3", "testService4")
    }
  }

  "getServicesInProfile" should {
    "return all services in a profile in a seq" in {
      when(mockJsonConnector.loadProfilesJson)
        .thenReturn(profilesJson)

      testService.getServicesInProfile("testprofile1") mustBe Seq("testService1", "testService2", "testService3")
    }
  }

  "getDetailsForService" should {
    "return the defined details for the service" in {
      when(mockJsonConnector.loadServicesJson)
        .thenReturn(servicesJson)

      testService.getDetailsForService("testService1") mustBe Json.obj("defaultPort" -> 1024)
    }
  }

  "getDuplicatePorts" should {
    "return a map of ports to services" in {
      when(mockJsonConnector.loadServicesJson)
        .thenReturn(servicesJson)

      testService.getDuplicatePorts mustBe Map(1026 -> Seq("testService3", "testService4"))
    }
  }

  "getServicesWithDefinedTestRoutes" should {
    "return a seq of test routes for a service" in {
      when(mockJsonConnector.loadServicesJson)
        .thenReturn(servicesJson)

      testService.getServicesWithDefinedTestRoutes mustBe Seq("testService4")
    }
  }

  "getServicesTestRoutes" should {
    "return a defined seq of test routes" in {
      when(mockJsonConnector.loadServicesJson)
        .thenReturn(servicesJson)

      val result = testService.getServicesTestRoutes("testService4")
      result mustBe Some(Seq(TestRoutesDesc(name = "testRoute", route = "http://localhost:1026/test/uri", description = "testDesc")))
    }

    "return an None" in {
      when(mockJsonConnector.loadServicesJson)
        .thenReturn(servicesJson)

      val result = testService.getServicesTestRoutes("testService3")
      result mustBe None
    }
  }
}
