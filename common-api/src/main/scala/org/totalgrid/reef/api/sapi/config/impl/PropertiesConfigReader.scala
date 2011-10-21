package org.totalgrid.reef.api.sapi.config.impl

import org.totalgrid.reef.api.sapi.config.ConfigReader
import java.util.Properties
import org.totalgrid.reef.api.japi.settings.util.PropertyReader

class PropertiesConfigReader(prop: Properties) extends ConfigReader {

  def this(file: String) = this(PropertyReader.readFromFile(file))

  def getProp(key: String) = Option(prop.get(key)).map(_.asInstanceOf[String])
}