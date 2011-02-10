<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns="http://www.w3.org/1999/xhtml">
  <xsl:output method="xml" indent="yes" encoding="UTF-8"/>
    <!--<xsl:template match="/">
        <html>
            <title><xsl:value-of select="servicedoc/title"/></title>
            <h2><xsl:value-of select="servicedoc/title"/></h2>
            wtf
            <xsl:apply-templates select="servicedoc/case"/>
        </html>
    </xsl:template>-->

    <xsl:template match="/">
        <html>
            <title><xsl:value-of select="servicedoc/title"/></title>
            <xsl:apply-templates select="servicedoc"/>
        </html>

    </xsl:template>

    <xsl:template match="servicedoc">
        <h2><xsl:value-of select="title"/></h2>
        <xsl:apply-templates select="case"/>
    </xsl:template>

    <xsl:template match="case">
        <h3><xsl:value-of select="title"/></h3>
        <xsl:apply-templates select="request/message"/>
    </xsl:template>

    <xsl:template match="request">
       asdf
    </xsl:template>

    <xsl:template match="message">
        <xsl:value-of select="@className"/>

    </xsl:template>


    <xsl:template match="field">

    </xsl:template>


    <xsl:template match="entry">

    </xsl:template>

</xsl:stylesheet>