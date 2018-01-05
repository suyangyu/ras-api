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

package uk.gov.hmrc.rasapi.services

import play.api.Logger
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.rasapi.connectors.{DesConnector, FileUploadConnector}
import uk.gov.hmrc.rasapi.models.{CallbackData, ResultsFileMetaData}
import uk.gov.hmrc.rasapi.repository.RasRepository

import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

object FileProcessingService extends FileProcessingService {

  override val fileUploadConnector: FileUploadConnector = FileUploadConnector
  override val desConnector: DesConnector = DesConnector

}

trait FileProcessingService extends RasFileReader with RasFileWriter with ResultsGenerator with SessionCacheService {

  def processFile(userId: String, callbackData: CallbackData)(implicit hc: HeaderCarrier): Unit = {

    lazy val results: ListBuffer[String] = ListBuffer.empty
    // val inputFileData = Await.result(readFile(callbackData.envelopeId,callbackData.fileId), 30 second)
    readFile(callbackData.envelopeId, callbackData.fileId).onComplete {
      inputFileData =>
        val data = inputFileData.get.foreach(row => if (!row.isEmpty) results += fetchResult1(row))

        val tempFilePath = generateFile1(results)
        RasRepository.filerepo.saveFile(userId, callbackData.envelopeId, tempFilePath, callbackData.fileId).onComplete {
          result =>
            clearFile(tempFilePath)
            result match {
              case Success(file) => SessionCacheService.updateFileSession(userId, callbackData,
                Some(ResultsFileMetaData(file.id.toString, file.filename, file.uploadDate, file.chunkSize, file.length)))

                fileUploadConnector.deleteUploadedFile(callbackData.envelopeId, callbackData.fileId)
              case Failure(ex) => Logger.error("results file generation/saving failed with Exception " + ex.getMessage)
              //delete result  a future ind
            }
            fileUploadConnector.deleteUploadedFile(callbackData.envelopeId, callbackData.fileId)
        }
    }
  }

  /*  def processFile(userId: String, callbackData: CallbackData)(implicit hc: HeaderCarrier) = {

      lazy val results: ListBuffer[String] = ListBuffer.empty

      createResultsFile(readFile(callbackData.envelopeId, callbackData.fileId).map { res =>
        res.map(row => if (!row.isEmpty) {
          fetchResult(row).map(results += _)
        })
      }).onComplete {
        case res => RasRepository.filerepo.saveFile(userId, callbackData.envelopeId, res.get, callbackData.fileId).map { file =>
          clearFile(res.get)
          //update status as success for the envelope in session-cache to confirm it is processed
          //if exception mark status as error and save into session
          SessionCacheService.updateFileSession(userId, callbackData,
            Some(ResultsFileMetaData(file.id.toString, file.filename, file.uploadDate, file.chunkSize, file.length)))
          //delete file a future ind
          fileUploadConnector.deleteUploadedFile(callbackData.envelopeId, callbackData.fileId)
        }
      }
    }*/
}


