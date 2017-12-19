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

import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfter
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.OneServerPerSuite
import play.api.libs.json.{Json, Writes}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.cache.client.{CacheMap, SessionCache}
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.rasapi.models.{CallbackData, RasSession, ResultsFileMetaData}

import scala.concurrent.Future

class SessionCacheServiceSpec extends UnitSpec with OneServerPerSuite with ScalaFutures with MockitoSugar with BeforeAndAfter {
  implicit val hc: HeaderCarrier = HeaderCarrier()
  val fileId = "file-id-1"
  val fileStatus = "AVAILABLE"
  val reason: Option[String] = None
  val callbackData = CallbackData("1234", fileId, fileStatus, reason)
  val resultsFile = ResultsFileMetaData(fileId,Some("fileName.csv"),Some(1234L),123,1234L)
  val rasSession = RasSession("1234",Some(callbackData),Some(resultsFile),"userId")
  val json = Json.toJson(rasSession)

  val mockSessionCache = mock[SessionCache]
  val SUT = new SessionCacheService {
    override val sessionCache: SessionCache = mockSessionCache
    when(sessionCache.fetchAndGetEntry[RasSession] (any(), any(),any())
      (any(),any(), any()))
      .thenReturn(Future.successful(Some(rasSession)))
    when(sessionCache.cache[RasSession] (any(), any(),any(),any())
      (any[Writes[RasSession]], any[HeaderCarrier], any()))
      .thenReturn(Future.successful(CacheMap("sessionValue", Map("1234" -> json))))

  }

  "SessionCacheService" should {
    "update session cache with processing status" in {
      val results = List("Nino, firstName, lastName, dob, cyResult, cy+1Result")
      val res = await(SUT.updateRasSession("1234",callbackData,Some(resultsFile)))
      res.data.get("1234").get shouldBe json}

    }
}