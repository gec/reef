package org.totalgrid.reef.api.sapi.config.impl

import org.totalgrid.reef.api.sapi.config.ConfigReader

class SystemPropertyConfigReader extends ConfigReader {

  def getProp(key: String): Option[String] = Option(System.getProperty(key))
}
