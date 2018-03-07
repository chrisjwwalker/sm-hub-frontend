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

  private val portRange = 1025 to 65535

  private val exclusions = Set("MONGO", "RABBITMQ", "NGINX", "REPAYMENTS_BANKREP_SETUP_DATA")

  private def filterServicesWithExclusions(exclude: Boolean = true): Seq[(String, JsValue)] = {
    if(exclude) {
      jsonConnector.loadServicesJson.fields.filterNot{case (service, _) => exclusions (service)}
    } else {
      jsonConnector.loadServicesJson.fields
    }
  }

  def getRunningServices(currentProfile: String = ""): Seq[(String, RunningResponse)] = {
    val profile = currentProfile.trim.replace(" ", "_")
    if (profile.isEmpty) { Seq.empty } else {
      val validServices  = jsonConnector.loadProfilesJson.\(profile.toUpperCase).as[Set[String]]

      filterServicesWithExclusions(false) collect {
        case (name, json) if validServices(name) =>
          val port        = json.\("defaultPort").as[Int]
          val isRunning   = await(httpConnector.pingService(port))
          name -> isRunning
      }
    }
  }

  def getValidPortNumbers(searchedRange: Option[(Int, Int)]): Seq[Int] = {
    searchedRange match {
      case Some((start, end)) =>
        val currentPorts   = filterServicesWithExclusions() map { case (_, details) => details.\("defaultPort").as[Int] }
        val availablePorts = portRange.diff(currentPorts).toSet
        (start to end).filter(availablePorts)
      case _ => Seq.empty
    }
  }

  def getAllProfiles: Seq[String] = {
    jsonConnector.loadProfilesJson.fields collect {case (service, _) if service != "ALL" => service}
  }

  def getAllServices: Seq[String] = {
    filterServicesWithExclusions() map {case (service, _) => service}
  }

  def getServicesInProfile(profile: String): Seq[String] = {
    jsonConnector.loadProfilesJson.\(profile.capitalize).as[Seq[String]]
  }

  def getDetailsForService(service: String): JsObject = {
    jsonConnector.loadServicesJson.\(service).as[JsObject]
  }

  def getDuplicatePorts: Map[Int, Seq[String]] = {
    val validServices = filterServicesWithExclusions()
    val allPorts = validServices.map {
      case(_, js) => js.\("defaultPort").as[Int]
    }
    val duplicatePorts = allPorts.diff(allPorts.distinct)

    validServices
      .map{case (service, json) => service -> json.\("defaultPort").as[Int]}
      .filter{case (_, port) => duplicatePorts.contains(port)}
      .groupBy{case (_, port) => port}
      .map{case (port, services) => port -> services.map{
        case (name, _) => name}
      }
  }

  def getServicesWithDefinedTestRoutes: Seq[String] = {
    filterServicesWithExclusions() collect {
      case (service, js) if js.\("testRoutes").asOpt[JsValue].isDefined => service
    }
  }

  def getServicesTestRoutes(service: String): Option[Seq[TestRoutesDesc]] = {
    val details = getDetailsForService(service)
    filterServicesWithExclusions()
      .collect {
        case (name, js) if name == service => js.\("testRoutes").asOpt[Seq[TestRoutesDesc]]
      }
      .head
      .map(testRoutes => testRoutes.map { testRoute =>
        testRoute.copy(route = s"http://localhost:${details.\("defaultPort").as[Int]}${testRoute.route}")
      }
    )
  }
}
