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

package common

import play.api.data.Forms.{number, text}
import play.api.data.Mapping
import play.api.data.validation.{Constraint, Invalid, Valid, ValidationError}
import play.api.i18n.Messages

object Validation {
  def requiredText(key: String)(implicit messages: Messages): Mapping[String] = {
    val textConstraint: Constraint[String] = Constraint("constraints.text"){
        case "" => Invalid(ValidationError(messages(s"validation.required.$key")))
        case _  => Valid
    }
    text.verifying(textConstraint)
  }

  def requiredNumber(key: String, min: Int, max: Int)(implicit messages: Messages): Mapping[Int] = {
    val numberConstraint: Constraint[Int] = Constraint("constraints.number"){
        case num if num < min | num > max => Invalid(ValidationError(messages("validation.required.port", key)))
        case num if num.toString == null  => Invalid(ValidationError(messages("validation.required.port.empty", key)))
        case _                            => Valid
    }
    number.verifying(numberConstraint)
  }
}
