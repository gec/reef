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