<?xml version="1.0"?>
<xsl:stylesheet version="2.0"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<xsl:output method="text"/>

<xsl:template match="/">
  <xsl:variable name="prepped">
    <xsl:apply-templates select="*"/>
  </xsl:variable>
  <xsl:apply-templates select="$prepped/documents/*" mode="output"/>
</xsl:template>

<xsl:template match="document" mode="output">
    <xsl:if test="@dtd">
      <xsl:value-of select="@dtd" disable-output-escaping="yes"/>
    </xsl:if>
    <xsl:copy-of select="node()"/>
</xsl:template>

</xsl:stylesheet>

