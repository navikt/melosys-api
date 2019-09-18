<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

    <xsl:template match="@*|*">
        <xsl:copy>
            <xsl:apply-templates select="@*|*|text()"/>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="forutgaendeBostedINorge"/>
    <xsl:template match="sammeAdresseSomArbeidsgiver"/>
    <xsl:template match="familiesBostedLandkode"/>
    <xsl:template match="adresseIUtlandet"/>

    <xsl:template match="maritimtArbeid/navn">
        <enhetNavn>
            <xsl:apply-templates />
        </enhetNavn>
    </xsl:template>

</xsl:stylesheet>