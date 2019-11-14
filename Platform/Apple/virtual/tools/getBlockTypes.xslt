<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns="outlaw" xpath-default-namespace="outlaw">

    <xsl:template match="block">
      <xsl:message><xsl:value-of select="@type"/></xsl:message>
      <xsl:apply-templates select="@*|node()"/>
    </xsl:template>

    <xsl:template match="@*|node()">
      <xsl:apply-templates select="@*|node()"/>
    </xsl:template>

</xsl:stylesheet>