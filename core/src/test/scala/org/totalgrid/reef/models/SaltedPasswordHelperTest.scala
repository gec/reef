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