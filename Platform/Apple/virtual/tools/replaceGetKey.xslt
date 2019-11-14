<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns="outlaw" xpath-default-namespace="outlaw">

    <xsl:template match="block[matches(@type, 'text_print')][matches(value/block/field/text(), '\(press any key\)', 'i')]">

      <xsl:variable name="context" select="concat(ancestor::map/@name, ': ', ancestor::script/@name)"/>

      <xsl:choose>

        <xsl:when test="next/block[@type='text_getanykey']/next/block[@type='text_clear_window']">
          <!--<xsl:message>Got clear</xsl:message>-->
          <block type="text_promptanykey" id="@id">
            <field name="CLEAR">1</field>
            <xsl:copy-of select="next/block[@type='text_getanykey']/next/block[@type='text_clear_window']/next"/>
          </block>
        </xsl:when>

        <xsl:when test="next/block[@type='text_getanykey']">
          <!--<xsl:message>No clear, next-next is: <xsl:value-of select="next/block[@type='text_getanykey']/next/block/@type"/></xsl:message>-->
          <block type="text_promptanykey" id="@id">
            <field name="CLEAR">0</field>
            <xsl:copy-of select="next/block[@type='text_getanykey']/next"/>
          </block>
        </xsl:when>

        <xsl:when test="next/block/next/block[@type='text_getanykey']">
          <!--<xsl:message><xsl:value-of select="$context"/>...Got intervening: <xsl:value-of select="next/block/@type"/></xsl:message>-->
          <block type="text_promptanykey" id="@id">
            <field name="CLEAR">0</field>
            <xsl:element name="{next/block/name()}">
              <xsl:copy-of select="next/block/@*"/>
              <xsl:copy-of select="next/block/*[not(matches(name(), 'next'))]"/>
            </xsl:element>
            <xsl:copy-of select="next/block/next/block/next"/>
          </block>
        </xsl:when>

        <xsl:otherwise>
          <xsl:message><xsl:value-of select="$context"/>... Hmm, next is: <xsl:value-of select="next/block/@type"/></xsl:message>
          <xsl:copy>
              <xsl:apply-templates select="@*|node()"/>
          </xsl:copy>
        </xsl:otherwise>

      </xsl:choose>

    </xsl:template>

    <!-- Leave everything else intact -->
    <xsl:template match="@*|node()">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()"/>
        </xsl:copy>
    </xsl:template>

</xsl:stylesheet>