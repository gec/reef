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
package org.totalgrid.reef.models

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

import org.totalgrid.reef.util.Conversion._

@RunWith(classOf[JUnitRunner])
class SaltedPasswordHelperTest extends FunSuite with ShouldMatchers {

  import SaltedPasswordHelper._

  test("DigestHandling") {

    100.count { i =>
      val (digest, salt) = makeDigestAndSalt(i + "password" + i)

      val roundTripDigest = dec64(enc64(digest))
      roundTripDigest should equal(digest)

      val roundTripSalt = dec64(enc64(salt))
      roundTripSalt should equal(salt)

      val calcedDigest = calcDigest(roundTripSalt, i + "password" + i)
      calcedDigest should equal(roundTripDigest)
    }
  }
}