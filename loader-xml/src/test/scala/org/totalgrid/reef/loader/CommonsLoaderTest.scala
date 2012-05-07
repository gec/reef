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
package org.totalgrid.reef.loader

import org.totalgrid.reef.util.XMLHelper
import org.totalgrid.reef.loader.configuration.Configuration
import scala.collection.JavaConversions._

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.FunSuite
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import java.io.File

import org.totalgrid.reef.loader.helpers.CachingModelLoader

@RunWith(classOf[JUnitRunner])
class CommonsLoaderTest extends FunSuite with ShouldMatchers {

  def getXmlString(snippet: String) = """<?xml version="1.0" encoding="utf-8" standalone="yes"?>
<configuration version="1.0"
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  targetNamespace="configuration.loader.reef"
  xmlns="configuration.loader.reef.totalgrid.org"
  xmlns:eq="equipment.loader.reef.totalgrid.org"
  xmlns:cm="communications.loader.reef.totalgrid.org"
  xmlns:common="common.loader.reef.totalgrid.org"
  xsi:noNamespaceSchemaLocation="configuration.xsd">
  """ + snippet + "</configuration>"

  def getConfigFile(snip: String) = {
    val c = XMLHelper.read(getXmlString(snip), classOf[Configuration])
    c.getConfigFiles.getConfigFile.toList.get(0)
  }

  def getLoader = {
    val modelLoader = new CachingModelLoader(None)
    val exCollector = new LoadingExceptionCollector
    val path = new File(".")
    new CommonLoader(modelLoader, exCollector, path)
  }

  test("Load ConfigFile as file") {
    val testSnip = """
    <common:configFiles>
      <common:configFile name="test.xml" mimeType="text/xml" fileName="pom.xml"/>
    </common:configFiles>
    """
    val cf = getConfigFile(testSnip)

    cf.getName should equal("test.xml")
    //cf.isSetValue should equal(false) // BUG in JAXB equals true always
    cf.getValue.trim.size should equal(0)

    val loader = getLoader
    val cfProto = loader.loadConfigFile(cf)

    cfProto.getFile.toStringUtf8 should include("</project>")
    cfProto.getName should equal("test.xml")

    loader.getExceptionCollector.hasErrors should equal(false)
  }

  test("Use Filename as name") {
    val testSnip = """
    <common:configFiles>
      <common:configFile mimeType="text/xml" fileName="pom.xml"/>
    </common:configFiles>
    """
    val cf = getConfigFile(testSnip)

    val loader = getLoader
    val cfProto = loader.loadConfigFile(cf)

    cfProto.getFile.toStringUtf8 should include("</project>")
    cfProto.getName should equal("pom.xml")

    loader.getExceptionCollector.hasErrors should equal(false)
  }

  test("Load ConfigFile as CDATA") {
    val testSnip = """
    <common:configFiles>
       <common:configFile name="test.xml" mimeType="text/xml"><![CDATA[
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<ns2:Master xmlns:ns2="org.psi.xml.dnp3" xmlns="org.psi.xml">
<!-- xml file -->
</ns2:Master>
]]></common:configFile>
    </common:configFiles>
    """
    val cf = getConfigFile(testSnip)

    cf.getName should equal("test.xml")
    cf.getValue.trim.size should (be > (0))

    val loader = getLoader
    val cfProto = loader.loadConfigFile(cf)

    cfProto.getFile.toStringUtf8 should include("ns2:Master")

    loader.getExceptionCollector.hasErrors should equal(false)
  }
}