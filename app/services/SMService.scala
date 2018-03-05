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

import javax.inject.Inject

import common.RunningResponse
import connectors.{HttpConnector, JsonConnector}
import models.TestRoutesDesc
import play.api.libs.json.{JsArray, JsObject, JsValue, Json}

import scala.concurrent.duration._
import scala.concurrent.{Await, Awaitable}

class DefaultSMService @Inject()(val jsonConnector: JsonConnector,
                                 val httpConnector: HttpConnector) extends SMService

trait SMService {
  val jsonConnector: JsonConnector
  val httpConnector: HttpConnector

  private def await[T](future : Awaitable[T]) : T = Await.result(future, 5.seconds)

  private def portRange = 1024 to 65535

  private val exclusions = Seq("MONGO", "RABBITMQ", "NGINX", "REPAYMENTS_BANKREP_SETUP_DATA")

  private def filterServicesWithExclusions(exclude: Boolean): Seq[(String, JsValue)] = {
    if(exclude) {
      jsonConnector.loadServicesJson.fields.filterNot(x => exclusions contains x._1)
    } else {
      jsonConnector.loadServicesJson.fields
    }
  }

  def getRunningServices(currentProfile: String = ""): Seq[(String, RunningResponse)] = {
    if(currentProfile.trim.equals("")) {
      Seq.empty
    } else {
      val profile       = currentProfile.toUpperCase.trim.replace(" ", "_")
      val activeProfile = jsonConnector.loadProfilesJson.\(profile).as[List[String]]
      val validApps     = filterServicesWithExclusions(false)
        .filter(activeProfile contains _._1)

      validApps.map { mapping =>
        val port = mapping._2.\("defaultPort").as[Int]
        val rr   = await(httpConnector.pingService(port))
        mapping._1 -> rr
      }
    }
  }

  def getValidPortNumbers(searchedRange: Option[(Int, Int)]): Seq[Int] = {
    searchedRange match {
      case Some((start, end)) =>
        val currentPorts   = filterServicesWithExclusions(true) map(_._2.\("defaultPort").as[Int])
        val availablePorts = portRange.filterNot(a => currentPorts.contains(a))
        (start to end).filter(x => availablePorts.contains(x))
      case _ => Seq.empty
    }
  }

  def getAllProfiles: Seq[String] = {
    jsonConnector.loadProfilesJson.fields.filterNot(_._1 == "ALL").map(_._1)
  }

  def getAllServices: Seq[String] = {
    filterServicesWithExclusions(true) map(_._1)
  }

  def getServicesInProfile(profile: String): Seq[String] = {
    jsonConnector.loadProfilesJson.\(profile.capitalize).as[Seq[String]]
  }

  def getDetailsForService(service: String): JsObject = {
    jsonConnector.loadServicesJson.\(service).as[JsObject]
  }

  def getDuplicatePorts: Map[Int, Seq[String]] = {
    val validServices = filterServicesWithExclusions(true)

    val allPorts       = validServices.map(_._2.\("defaultPort").as[Int])
    val duplicatePorts = allPorts.diff(allPorts.distinct)

    validServices
      .filter(x => duplicatePorts.contains(x._2.\("defaultPort").as[Int]))
      .map(x => x._1 -> x._2.\("defaultPort").as[Int])
      .groupBy(x => x._2)
      .map(x => x._1 -> x._2.map(i => i._1))
  }

  def getServicesWithDefinedTestRoutes: Seq[String] = {
    filterServicesWithExclusions(true)
      .filter(_._2.\("testRoutes").asOpt[JsValue].nonEmpty)
      .map(_._1)
  }

  def getServicesTestRoutes(service: String): Option[Seq[TestRoutesDesc]] = {
    val testRoutes = filterServicesWithExclusions(true)
      .filter(_._1 == service)
      .map(_._2.\("testRoutes").asOpt[Seq[TestRoutesDesc]])
      .head

    testRoutes.map(x => x.map { a =>
      val details = getDetailsForService(service)
      a.copy(route = s"http://localhost:${details.\("defaultPort").as[Int]}${a.route}")
    })
  }
}
