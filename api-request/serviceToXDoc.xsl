<!--

    Copyright 2011 Green Energy Corp.

    Licensed to Green Energy Corp (www.greenenergycorp.com) under one
    or more contributor license agreements. See the NOTICE file
    distributed with this work for additional information
    regarding copyright ownership.  Green Energy Corp licenses this file
    to you under the Apache License, Version 2.0 (the
    "License"); you may not use this file except in compliance
    with the License.  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on an
    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied.  See the License for the
    specific language governing permissions and limitations
    under the License.

-->
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns="http://www.w3.org/1999/xhtml">
    <xsl:output method="xml" indent="yes" encoding="UTF-8"/>

    <xsl:template match="/">
        <document xmlns="http://maven.apache.org/XDOC/2.0"
                  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                  xsi:schemaLocation="http://maven.apache.org/XDOC/2.0 http://maven.apache.org/xsd/xdoc-2.0.xsd">

            <properties>
                <title><xsl:value-of select="servicedoc/title"/></title>
            </properties>
            <head>
                <link rel="stylesheet" type="text/css" href="css/proto_tables.css"/>
            </head>
            <body>
                <xsl:apply-templates select="servicedoc"/>
            </body>
        </document>
    </xsl:template>

    <xsl:template match="servicedoc">
        <section>
            <xsl:attribute name="name">
                <xsl:value-of select="title"/>
            </xsl:attribute>
            <xsl:copy-of select="desc/div"/>
        </section>
        <xsl:apply-templates select="case"/>
    </xsl:template>

    <xsl:template match="case">
        <section>
            <xsl:attribute name="name">
                <xsl:value-of select="title"/>
            </xsl:attribute>
            <xsl:copy-of select="desc/div"/>
            <xsl:apply-templates select="request"/>
            <xsl:apply-templates select="response"/>
        </section>
    </xsl:template>

    <xsl:template match="request">
        <subsection>
            <xsl:attribute name="name">
                Request (<xsl:value-of select="@verb"/>)
            </xsl:attribute>
            <xsl:apply-templates select="message"/>
        </subsection>
    </xsl:template>

    <xsl:template match="response">
        <subsection name="Response">
            <xsl:apply-templates select="message"/>
        </subsection>
    </xsl:template>

    <xsl:template match="message">
        <div class="message">
            <div class="msgTitle">
                <xsl:value-of select="@className"/>
            </div>
            <div class="msgBody">
                <table>
                    <xsl:apply-templates select="field"/>
                </table>
            </div>

        </div>
    </xsl:template>


    <xsl:template match="field">
        <tr>
            <td class="fieldName"><xsl:value-of select="@name"/></td>
            <td class="fieldVal">
                <xsl:apply-templates/>
            </td>
        </tr>
    </xsl:template>


    <xsl:template match="entry">
       <div class="listEntry">
           <xsl:apply-templates/>
       </div>
    </xsl:template>

</xsl:stylesheet>