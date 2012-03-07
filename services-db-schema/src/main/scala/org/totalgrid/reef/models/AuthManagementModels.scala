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

import org.squeryl.PrimitiveTypeMode._
import org.totalgrid.reef.util.LazyVar
import java.util.UUID

/**
 * Helpers for handling the implementation of salted password and encoded passwords
 * http://www.jasypt.org/howtoencryptuserpasswords.html
 */
object SaltedPasswordHelper {
  import org.apache.commons.codec.binary.Base64
  import java.security.{ SecureRandom, MessageDigest }

  def enc64(original: String): String = {
    new String(Base64.encodeBase64(original.getBytes("UTF-8")), "US-ASCII")
  }

  def dec64(original: String): String = {
    new String(Base64.decodeBase64(original.getBytes("US-ASCII")), "UTF-8")
  }

  /**
   * @returns tuple of (digest, salt)
   */
  def makeDigestAndSalt(password: String): (String, String) = {
    val b = new Array[Byte](20)
    new SecureRandom().nextBytes(b)
    val salt = dec64(enc64(new String(b, "UTF-8")))
    (calcDigest(salt, password), salt)
  }

  def calcDigest(salt: String, pass: String) = {
    val combinedSaltPassword = (salt + pass).getBytes("UTF-8")
    val digestBytes = MessageDigest.getInstance("SHA-256").digest(combinedSaltPassword)
    // TODO: figure out how to roundtrip bytes through UTF-8
    dec64(enc64(new String(digestBytes, "UTF-8")))
  }
}

// not a case class because we don't want to accidentally print the digest+salt in the logs
class Agent(
    _entityId: UUID,
    val digest: String,
    val salt: String) extends EntityBasedModel(_entityId) {

  val permissionSets = LazyVar(ApplicationSchema.permissionSets.where(ps => ps.id in from(ApplicationSchema.agentSetJoins)(p => where(p.agentId === id) select (&(p.permissionSetId)))))
  val authTokens = LazyVar(ApplicationSchema.authTokens.where(au => au.agentId === id))

  def checkPassword(password: String): Boolean = {
    import SaltedPasswordHelper._
    dec64(digest) == calcDigest(dec64(salt), password)
  }

  def copyWithUpdatedPassword(password: String): Agent = {
    import SaltedPasswordHelper._
    val (digest, saltText) = makeDigestAndSalt(password)
    val agent = new Agent(entity.value.id, enc64(digest), enc64(saltText))
    agent.id = id
    agent.entity.value = entity.value
    agent
  }
}

case class AuthPermission(
    val allow: Boolean,
    val resource: String,
    val verb: String) extends ModelWithId {
}

case class PermissionSet(
    _entityId: UUID,
    val defaultExpirationTime: Long) extends EntityBasedModel(_entityId) {

  val permissions = LazyVar(ApplicationSchema.permissions.where(ps => ps.id in from(ApplicationSchema.permissionSetJoins)(p => where(p.permissionSetId === id) select (&(p.permissionId)))))
}

case class AuthToken(
    val token: String,
    val agentId: Long,
    val loginLocation: String,
    val clientVersion: String,
    var expirationTime: Long) extends ModelWithId {

  val agent = LazyVar(hasOne(ApplicationSchema.agents, agentId))
  val permissionSets = LazyVar(ApplicationSchema.permissionSets.where(ps => ps.id in from(ApplicationSchema.tokenSetJoins)(p => where(p.authTokenId === id) select (&(p.permissionSetId)))))
}

case class AgentPermissionSetJoin(val permissionSetId: Long, val agentId: Long)
case class PermissionSetJoin(val permissionSetId: Long, val permissionId: Long)
case class AuthTokenPermissionSetJoin(val permissionSetId: Long, val authTokenId: Long)
