
def capitalizeAfterUnderScore(s)
  if s.match(/_/)
    parts = s.split('_')
    [
    parts[0],
    parts[1,parts.size].collect{ |e| r = e.dup;  r[0] = r[0].chr.capitalize; r}
    ].join
  else 
    s
  end 
end

def CamelCase(s)
  first = capitalizeAfterUnderScore(s).dup
  first[0] = first[0].chr.capitalize
  first
end
def camelCase(s)
  first = capitalizeAfterUnderScore(s).dup
  first[0] = first[0].chr.downcase
  first
end
def starts_with?(str, prefix)
  prefix = prefix.to_s
  str[0, prefix.length] == prefix
end

class String
   def underscore
       self.gsub(/::/, '/').
       gsub(/([A-Z]+)([A-Z][a-z])/,'\1_\2').
       gsub(/([a-z\d])([A-Z])/,'\1_\2').
       tr("-", "_").
       downcase
   end
end

def add_deserializer(file, name, fullName)
  name = name.strip
  fullName = fullName.strip
  file.puts <<EOF
  def #{camelCase(name)}() = new TypeDescriptor[#{fullName}] {
    def serialize(typ : #{fullName}) : Array[Byte] = typ.toByteArray
    def deserialize(bytes: Array[Byte]) = #{fullName}.parseFrom(bytes)
    def getKlass = classOf[#{fullName}]
    def id = "#{fullName.split('.').last.underscore}"
  }
EOF
end

def addType(file, style, type, name, built_ins, package, original_type)
  class_name = type.strip.split('.')[-1]
  built_in = built_ins.include?(class_name)

  reserved = ['type','class']
  safeName = camelCase(name)
  safeName = "_#{safeName}" if reserved.include?(safeName)
  
  attr_name = CamelCase(name)
  type_name = CamelCase(type)
  unless built_in
    type = package.gsub(".","_")
    type_name = CamelCase(type)
  end
  
  if style == 'optional'
    if built_in
      file.puts "val #{safeName} = optionally(_.has#{attr_name}, _.get#{attr_name})"
    else
      
      file.puts "val #{safeName} = new Opt#{type_name}(optionally(_.has#{attr_name}, _.get#{attr_name}))"
    end
   elsif style == 'required'
     if built_in
       file.puts "val #{safeName} = optionally(_.get#{attr_name})"
     else
     
       file.puts "val #{safeName} = new Opt#{type_name}(optionally(_.get#{attr_name}))"
     end
   elsif style == 'repeated'
     if built_in
       file.puts "val #{safeName} = optionally(_.get#{attr_name}List.toList)"
     else
       file.puts "val #{safeName} = optionally(_.get#{attr_name}List.toList.map{i => new Opt#{type_name}(Some(i))})"
     end
   end
end

def processLines(opt_file, deserializers, lines)
  inEnum = false
  built_ins = ['string','bytes','uint32','bool', 'double', 'sint64', 'uint64','int32', 'int64','Severity', 'Type', 'CommandStatus']
  ns = "org.totalgrid.reef.proto"
  cn = false
  lines.each do |line|
    if starts_with?(line, "//")
      #puts "Comment Line: #{line}" 
    elsif line[/^[\s]*option\s[\s]*java_package\s.*$/]
      #puts "Package: #{line}"
      m = line.match(/^[\s]*option\s[\s]*java_package\s[\s]*=\s[\s]*\"([\S]*)\".*$/)
      #puts m[1]
      ns = m[1]
    elsif line[/^[\s]*option\s[\s]*java_outer_classname\s.*$/]
      #puts "Classname: #{line}"
      m = line.match(/^[\s]*option\s[\s]*java_outer_classname\s[\s]*=\s[\s]*\"([\S]*)\".*$/)
      #puts m[1]
      cn = m[1]
    elsif line[/enum ([^\{]*)[\s]*\{/]
      m = line.match(/enum ([^\{]*)\{/)
      inEnum = true
      built_ins << m[1].strip
    elsif line[/message ([^\{]*)\{/]
      m = line.match(/message ([^\{]*)\{/)
      
      if cn
        fullName = ns + "." + cn + "." + m[1]
        optName = cn + m[1]
      else 
        fullName = m[1]
        optName = m[1]
      end
      
      optName = cn ? CamelCase(cn) + m[1] : m[1]

      opt_file.puts "implicit def proto2Opt#{optName}(a: #{fullName}): Opt#{optName} = new Opt#{optName}(Some(a))"
      opt_file.puts "class Opt#{optName}(real: Option[#{fullName}]) extends OptionalStruct(real){"
      
      add_deserializer(deserializers, m[1], fullName)
    elsif line[/^[\s]*(optional|required|repeated)[\s]*([\S]*)[\s]*([\S]*).*$/]
      m1 = line.match(/^[\s]*(optional|required|repeated)[\s]*([\S]*)[\s]*([\S]*).*$/)
      #opt_file.puts "//#{line}"
      
      typ = m1[2]
      fullName = cn ? cn + typ : typ
      if typ.include? ns
        typ = typ.gsub(ns+".",'')
        fullName = typ
      end

      addType(opt_file, m1[1],typ, m1[3], built_ins, fullName, m1[2])
    elsif line[/\}/]
      if inEnum 
        inEnum = false
      else 
        opt_file.puts line
      end
    else
      #puts "//#{line}"
    end
  end
end

require 'fileutils'
def generatePackageInfo(fname, file_text)
  proto_name = File.basename(fname, ".proto")

  overview = "/**\nProto definition file for #{proto_name}.\n"
  overview += "<pre>"
  overview += file_text.gsub("*/", "*-/")
  overview += "</pre>\n*/\n"
  overview += "package org.totalgrid.reef.client.rpc.protodoc.#{proto_name.downcase};"

  dir_name = "./client-api/src/main/java/org/totalgrid/reef/api/japi/client/rpc/protodoc/#{proto_name.downcase}"
  FileUtils.mkdir_p(dir_name)

  f = File.open(File.join(File.dirname(__FILE__),"#{dir_name}/package-info.java"), 'wb')
  f.puts(overview)
  f.close
end

f = File.open(File.join(File.dirname(__FILE__),"./proto/src/main/scala/org/totalgrid/reef/proto/OptionalProtos.scala"), 'wb')

types = %w[Application Commands FEP Mapping Measurements ProcessStatus Alarms Events Processing Model Auth]

scala_imports = types.collect{|t| "import org.totalgrid.reef.proto.#{t}._"}.join("\n")
java_imports = scala_imports.gsub("_","*")

f.puts <<EOF
package org.totalgrid.reef.client.sapi

#{scala_imports}

import scala.collection.JavaConversions._
import org.totalgrid.reef.api.sapi.types.Optional._

object OptionalProtos {

EOF

deseralizers = File.open(File.join(File.dirname(__FILE__),"./proto/src/main/scala/org/totalgrid/reef/proto/Descriptors.scala"), 'wb')

deseralizers.puts <<EOF
package org.totalgrid.reef.client.sapi

import org.totalgrid.reef.api.japi.TypeDescriptor

#{scala_imports}

object Descriptors {

EOF

all_names = []

Dir[File.join(File.dirname(__FILE__),"./schema/proto/**/*.proto")].each do |files|
  files.each do |fname|
    all_names << fname
  end
end

def fix_dir(a)
  a.gsub("\\","/")
end

unless "".respond_to?(:lines)
  class String
    def lines
      ary = []
      self.each_line{|l| ary << l}
      ary
    end 
  end
end

all_names = all_names.sort{|a,b| fix_dir(a) <=> fix_dir(b)}
#puts all_names
all_names.each do |fname|
  file_text = File.open(fname).read
  processLines(f, deseralizers, file_text.lines)
  generatePackageInfo(fname, file_text)
end

f.puts "}"
deseralizers.puts "}"

f.close
deseralizers.close

puts "Generated OptionalProtos.scala and Descriptors.scala"
