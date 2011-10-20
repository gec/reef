package org.totalgrid.reef.api.sapi.config.impl

import java.util.{ Properties }
import java.io.FileInputStream
import org.totalgrid.reef.api.sapi.config.ConfigReader

class FileConfigReader(file: String) extends ConfigReader {
  val fis = new FileInputStream(file)
  val props = new Properties
  try {
    props.load(fis)
  } finally {
    fis.close
  }

  def getProp(key: String): Option[String] = Option(props.getProperty(key))
}

