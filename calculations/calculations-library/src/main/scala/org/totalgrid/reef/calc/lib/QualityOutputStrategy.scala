/**
 * Copyright 2011 Green Energy Corp.
 *
 * Licensed to Green Energy Corp (www.greenenergycorp.com) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. Green Energy
 * Corp licenses this file to you under the GNU Affero General Public License
 * Version 3.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.gnu.org/licenses/agpl.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.totalgrid.reef.calc.lib

import org.totalgrid.reef.client.service.proto.Calculations.OutputQuality
import org.totalgrid.reef.client.service.proto.Measurements.{ DetailQual, Quality, Measurement }
import org.totalgrid.reef.client.service.proto.OptionalProtos._

trait QualityOutputStrategy {
  def getQuality(inputs: Map[String, List[Measurement]]): Quality
}

object QualityOutputStrategy {
  def build(config: OutputQuality.Strategy) = config match {
    case _ => new AlwaysOk
    /*case OutputQuality.Strategy.ALWAYS_OK => new AlwaysOk
    case OutputQuality.Strategy.WORST_QUALITY => new WorstQuality
    case _ => throw new Exception("Unknown quality output strategy")*/
  }

  class AlwaysOk extends QualityOutputStrategy {
    def getQuality(inputs: Map[String, List[Measurement]]): Quality = {
      Quality.newBuilder().setValidity(Quality.Validity.GOOD).setSource(Quality.Source.PROCESS).build()
    }
  }

  class WorstQuality extends QualityOutputStrategy {
    def getQuality(inputs: Map[String, List[Measurement]]): Quality = {
      inputs.values.flatten.map(_.getQuality).reduceLeft((l, r) => merge(l, r))
    }

    private def merge(l: Quality, r: Quality): Quality = {
      val ldetail = l.detailQual.getOption.getOrElse(DetailQual.newBuilder().build())
      val rdetail = r.detailQual.getOption.getOrElse(DetailQual.newBuilder().build())

      Quality.newBuilder()
        .setDetailQual(merge(ldetail, rdetail))
        .setSource(merge(l.getSource, r.getSource))
        .setValidity(merge(l.getValidity, r.getValidity))
        .setTest(mergeTest(l.getTest, r.getTest))
        .setOperatorBlocked(mergeBlocked(l.getOperatorBlocked, r.getOperatorBlocked))
        .build()
    }

    private def rank(v: Quality.Validity) = v match {
      case Quality.Validity.GOOD => 1
      case Quality.Validity.INVALID => 2
      case Quality.Validity.QUESTIONABLE => 3
    }

    private def merge(left: Quality.Validity, right: Quality.Validity) = {
      if (rank(left) >= rank(right)) left else right
    }

    private def merge(left: Quality.Source, right: Quality.Source) = {
      if (left == Quality.Source.SUBSTITUTED || right == Quality.Source.SUBSTITUTED) {
        Quality.Source.SUBSTITUTED
      } else {
        Quality.Source.PROCESS
      }
    }

    private def mergeTest(left: Boolean, right: Boolean) = {
      left || right
    }

    private def mergeBlocked(left: Boolean, right: Boolean) = {
      left || right
    }

    private def merge(left: DetailQual, right: DetailQual) = {
      DetailQual.newBuilder()
        .setBadReference(left.getBadReference || right.getBadReference)
        .setOverflow(left.getOverflow || right.getOverflow)
        .setOutOfRange(left.getOutOfRange || right.getOutOfRange)
        .setOscillatory(left.getOscillatory || right.getOscillatory)
        .setFailure(left.getFailure || right.getFailure)
        .setOldData(left.getOldData || right.getOldData)
        .setInconsistent(left.getInconsistent || right.getInconsistent)
        .setInaccurate(left.getInaccurate || right.getInaccurate)
        .build()
    }

  }
}
