/*
 * Copyright 2017 HM Revenue & Customs
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

package uk.gov.hmrc.rasapi.services

import play.api.Logger
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.cache.client.SessionCache
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext
import uk.gov.hmrc.rasapi.config.RasSessionCache
import uk.gov.hmrc.rasapi.models.{CallbackData, RasSession, ResultsFileMetaData}

import scala.concurrent.ExecutionContext.Implicits.global

trait SessionCacheService {

  val sessionCache: SessionCache = RasSessionCache
  private val source = "ras"
  private val cacheId = "fileUploadSession"
  def updateRasSession(envelopeId : String, userFile:CallbackData, resultsFile:Option[ResultsFileMetaData])(implicit hc: HeaderCarrier) = {
    implicit val executionContext = MdcLoggingExecutionContext.fromLoggingDetails(hc)
    sessionCache.fetchAndGetEntry[RasSession](source,cacheId,envelopeId).flatMap{session =>
      sessionCache.cache[RasSession](source,cacheId,envelopeId,
      RasSession(session.get.envelopeId, Some(userFile),resultsFile,session.get.userId) )
    }.recover {
      case ex: Throwable => Logger.error(s"Error while saving data to cache for RasSession => " +
        s"${envelopeId} , userFile : ${userFile.toString} , resultsFile id : " +
        s"${if(resultsFile.isDefined) resultsFile.get.id}, \n Exception is ${ex.getMessage}" )
        throw new RuntimeException("Error in saving sessionCache" + ex.getMessage)
/*
        Logger.warn("retrying to save cache")
        updateRasSession(envelopeId,userFile,resultsFile)
*/
    }
  }

}

object SessionCacheService extends SessionCacheService {
  override val sessionCache: SessionCache = RasSessionCache
}