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
        <html>
            <head>
            <title>
                <xsl:value-of select="servicedoc/title"/>
            </title>
                <link rel="stylesheet" type="text/css" href="style.css"/>
            </head>
            <body>
            <xsl:apply-templates select="servicedoc"/>
            </body>
        </html>

    </xsl:template>

    <xsl:template match="servicedoc">
        <h2>
            <xsl:value-of select="title"/>
        </h2>
        <p><xsl:value-of select="desc"/></p>
        <xsl:apply-templates select="case"/>
    </xsl:template>

    <xsl:template match="case">
        <h3>
            <xsl:value-of select="title"/>
        </h3>
        <p><xsl:value-of select="desc"/></p>
        <xsl:apply-templates select="request"/>
        <xsl:apply-templates select="response"/>
    </xsl:template>

    <xsl:template match="request">
        <h4>Request</h4>
        <xsl:apply-templates select="message"/>

    </xsl:template>
    <xsl:template match="response">
        <h4>Response</h4>
        <xsl:apply-templates select="message"/>
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