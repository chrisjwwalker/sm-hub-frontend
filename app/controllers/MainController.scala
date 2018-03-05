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

package controllers

import javax.inject.Inject

import forms.{AvailablePortsForm, RunningServicesForm}
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, Controller}
import services.SMService
import views.html._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class DefaultMainController @Inject()(val smService: SMService) extends MainController

trait MainController extends Controller {

  val smService: SMService

  def homeRedirect(): Action[AnyContent] = Action.async { implicit request =>
    Future(Redirect(routes.MainController.home()))
  }

  def home(): Action[AnyContent] = Action.async { implicit request =>
    val profile = request.session.get("last-searched-profile").getOrElse("")
    val apps    = smService.getRunningServices(profile)
    Future(Ok(HomeView(apps, RunningServicesForm.form.fill(profile))))
  }

  def submitHome(): Action[AnyContent] = Action.async { implicit request =>
    RunningServicesForm.form.bindFromRequest.fold(
      errors => {
        Future(BadRequest(HomeView(smService.getRunningServices(), errors)))
      },
      valid => {
        Future.successful(Redirect(routes.MainController.home()).withSession("last-searched-profile" -> valid))
      }
    )
  }

  def availablePorts(): Action[AnyContent] = Action.async { implicit request =>
    val ports = smService.getValidPortNumbers(None)
    Future(Ok(PortsView(ports, AvailablePortsForm.form)))
  }

  def submitAvailablePorts(): Action[AnyContent] = Action.async { implicit request =>
    AvailablePortsForm.form.bindFromRequest.fold(
      errors => Future(BadRequest(PortsView(smService.getValidPortNumbers(None), errors))),
      valid  => {
        val ports = smService.getValidPortNumbers(Some(valid))
        Future(Ok(PortsView(ports, AvailablePortsForm.form.fill(valid))))
      }
    )
  }

  def currentProfiles(): Action[AnyContent] = Action.async { implicit request =>
    val profiles = smService.getAllProfiles
    Future(Ok(ProfilesView(profiles)))
  }

  def currentServices(): Action[AnyContent] = Action.async { implicit request =>
    val services = smService.getAllServices
    Future(Ok(ServicesView(services)))
  }

  def servicesInProfile(profile: String): Action[AnyContent] = Action.async { implicit request =>
    val services = smService.getServicesInProfile(profile)
    Future(Ok(ServicesInProfileView(profile, services)))
  }

  def detailsForService(service: String): Action[AnyContent] = Action.async { implicit request =>
    val serviceDetails = smService.getDetailsForService(service)
    Future(Ok(ServiceDetailsView(serviceDetails)))
  }

  def potentialConflicts(): Action[AnyContent] = Action.async { implicit request =>
    val potentialConflicts = smService.getDuplicatePorts
    Future(Ok(PotentialConflictsView(potentialConflicts)))
  }

  def serviceTestRoutes(): Action[AnyContent] = Action.async { implicit request =>
    val servicesWithTestRoutes = smService.getServicesWithDefinedTestRoutes
    Future(Ok(ServiceTestRoutesView(servicesWithTestRoutes)))
  }

  def serviceTestRoutesExpanded(service: String): Action[AnyContent] = Action.async { implicit request =>
    val serviceTestRoutes = smService.getServicesTestRoutes(service)
    Future(Ok(ServiceTestRoutesExpandedView(service, serviceTestRoutes)))
  }
}
