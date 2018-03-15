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

import common.{Logging, RunningResponse}
import connectors.{HttpConnector, JsonConnector}
import models.TestRoutesDesc
import play.api.Configuration
import play.api.libs.functional.Functor
import play.api.libs.json.{JsObject, JsValue}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Awaitable, Future}

class DefaultSMService @Inject()(val jsonConnector: JsonConnector,
                                 val httpConnector: HttpConnector,
                                 config: Configuration) extends SMService

trait SMService extends Logging {
  val jsonConnector: JsonConnector
  val httpConnector: HttpConnector

  private def await[T](future : Awaitable[T]) : T = Await.result(future, 5.seconds)

  private val portRange = 1025 to 65535

  def getRunningServices(currentProfile: String = ""): Seq[(String, RunningResponse)] = {
    val profile = currentProfile.trim.replace(" ", "_")
    if (profile.isEmpty) { Seq.empty } else {
      jsonConnector.loadProfilesJson.\(profile.toUpperCase).asOpt[Set[String]].fold(Seq.empty[(String, RunningResponse)]) { validServices =>
        jsonConnector.loadServicesJson.fields collect {
          case (name, json) if validServices(name) =>
            val port      = json.\("defaultPort").as[Int]
            val isRunning = json.\("healthcheck").\("url").asOpt[String] match {
              case Some(url) => await(httpConnector.pingService(name, url.replace("${port}", port.toString), port))
              case None      => await(httpConnector.pingService(name, s"http://localhost:$port/ping/ping", port))
            }

            s"$name@$port" -> isRunning
        }
      }
    }
  }

  def getValidPortNumbers(searchedRange: Option[(Int, Int)]): Seq[Int] = {
    searchedRange match {
      case Some((start, end)) =>
        val currentPorts   = jsonConnector.loadServicesJson.fields map { case (_, details) => details.\("defaultPort").asOpt[Int] }
        val availablePorts = portRange.diff(currentPorts.flatten).toSet
        (start to end).filter(availablePorts)
      case _ => Seq.empty
    }
  }

  def getAllProfiles: Seq[String] = {
    jsonConnector.loadProfilesJson.fields collect { case (service, _) if service != "ALL" => service }
  }

  def searchForProfile(query: String): Seq[String] = {
    if(query.contains("\"")) {
      exactMatchSearch(jsonConnector.loadProfilesJson.fields, query)
    } else {
      jsonConnector.loadProfilesJson.fields.collect { case (name, _) if name.contains(query) => name}
    }
  }

  def getAllServices: Seq[String] = {
    jsonConnector.loadServicesJson.fields map { case (service, _) => service}
  }

  def searchForService(query: String): Seq[String]= {
    if(query.contains("\"")) {
      exactMatchSearch(jsonConnector.loadServicesJson.fields, query)
    } else {
      jsonConnector.loadServicesJson.fields.collect { case (name, _) if name.contains(query) => name}
    }
  }

  private def exactMatchSearch(f: => Seq[(String, JsValue)], quey: String): Seq[String] = {
    val query = quey.trim.replaceAll("\"", "")
    f.collect { case (name, _) if name.split("_").contains(query) => name }
  }

  def getServicesInProfile(profile: String): Seq[String] = {
    jsonConnector.loadProfilesJson.\(profile.toUpperCase).as[Seq[String]]
  }

  def getDetailsForService(service: String): JsObject = {
    jsonConnector.loadServicesJson.\(service).as[JsObject]
  }

  def getDuplicatePorts: Map[Int, Seq[String]] = {
    val validServices = jsonConnector.loadServicesJson.fields
      .filter(Function.tupled((_, js) => js.\("defaultPort").asOpt[Int].isDefined))

    val allPorts = validServices.map {
      case(_, js) => js.\("defaultPort").as[Int]
    }
    val duplicatePorts = allPorts.diff(allPorts.distinct)

    validServices
      .map{case (service, json) => service -> json.\("defaultPort").as[Int]}
      .filter{case (_, port) => duplicatePorts.contains(port)}
      .groupBy{case (_, port) => port}
      .map{ case (port, services) => port -> services.map { case (name, _) => name } }
  }

  def getServicesWithDefinedTestRoutes: Seq[String] = {
    jsonConnector.loadServicesJson.fields collect {
      case (service, js) if js.\("testRoutes").asOpt[JsValue].isDefined => service
    }
  }

  def getServicesTestRoutes(service: String): Option[Seq[TestRoutesDesc]] = {
    val details = getDetailsForService(service)
    jsonConnector.loadServicesJson.fields
      .collect { case (name, js) if name == service => js.\("testRoutes").asOpt[Seq[TestRoutesDesc]] }
      .head
      .map(testRoutes => testRoutes.map { testRoute => testRoute.copy(route = s"http://localhost:${details.\("defaultPort").as[Int]}${testRoute.route}") })
  }

  def getAssetsFrontendVersions: Future[List[String]] = {
    httpConnector.getBodyOfPage(9032, "/assets") map { doc =>
      doc.getElementsByTag("a").text.replace("/", "").split(" ").toList
    } recover {
      case _ =>
        logger.warn("Assets frontend currently isn't available on port 9032")
        List.empty[String]
    }
  }

  def getAllGHERefs: Seq[(String, String)] = {
    jsonConnector.loadServicesJson.fields
      .filter(Function.tupled((_, js) => js.\("sources").asOpt[JsObject].isDefined))
      .collect {
        case (service, js) if js.\("sources").\("repo").as[String].contains("tools") =>
          service -> js.\("sources").\("repo").as[String]
      }
  }
}
