package org.totalgrid.reef.api.sapi.config

// TODO: get rid of config reader and defaults
trait ConfigReader {

  def getProp(key: String): Option[String]

  // TODO: remove defaults from most configuration options
  def getString(key: String, default: String) = getProp(key) getOrElse (default)
  def getInt(key: String, default: Int) = getProp(key).map(s => s.toInt) getOrElse (default)
  def getLong(key: String, default: Long) = getProp(key).map(s => s.toLong) getOrElse (default)
  def getBoolean(key: String, default: Boolean) = getProp(key).map(s => s.toBoolean) getOrElse (default)
}

