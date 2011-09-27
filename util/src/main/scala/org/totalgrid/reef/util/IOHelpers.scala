/**
 * Copyright 2011 Green Energy Corp.
 *
 * Licensed to Green Energy Corp (www.greenenergycorp.com) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. Green Energy
 * Corp licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.totalgrid.reef.util

import java.io.{ FileOutputStream, IOException, FileInputStream, File }

/**
 * helpers to ease the reading and writing of files using the java stream constructs
 * TODO: add IOHelpers unit tests
 */
object IOHelpers {
  // TODO: replace xml loader image loading with this
  def readBinary(file: File): Array[Byte] = {

    checkFileExistence(file)
    checkFileReadable(file)

    val fis = new FileInputStream(file)
    try {
      val lengthLong = file.length()
      if (lengthLong > Integer.MAX_VALUE) {
        throw new IOException("File: " + file.getAbsolutePath + " is larger than " + Integer.MAX_VALUE + " bytes")
      }
      val length = lengthLong.toInt
      val buffer = new Array[Byte](length)
      var read: Int = 0
      while (read < length) {
        read += fis.read(buffer, read, length - read)
      }
      buffer
    } finally {
      fis.close()
    }
  }

  def readString(file: File): String = {

    checkFileExistence(file)
    checkFileReadable(file)

    // TODO: replace Source.fromFile with standard java way of reading in text file
    scala.io.Source.fromFile(file).mkString
  }

  def writeBinary(file: File, data: Array[Byte]) {

    checkFileExistence(file)
    checkFileWritable(file)

    val fis = new FileOutputStream(file)
    try {
      fis.write(data)
    } finally {
      fis.close
    }
  }

  def writeString(file: File, string: String) {
    writeBinary(file, string.getBytes)
  }

  def checkFileExistence(file: File) {
    if (!file.exists()) {
      throw new IOException("File: " + file.getAbsolutePath + " doesn't exist or isn't accesible.")
    }
  }

  def checkFileReadable(file: File) {
    if (!file.canRead()) {
      throw new IOException("File: " + file.getAbsolutePath + " isn't readable.")
    }
  }

  def checkFileWritable(file: File) {
    if (!file.canWrite()) {
      throw new IOException("File: " + file.getAbsolutePath + " isn't writable.")
    }
  }
}