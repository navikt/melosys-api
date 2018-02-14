<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:output method="xml" indent="no"/>

    <xsl:template match="/dokumenter">
        <fastsettLovvalgRequest>
            <xsl:apply-templates/>
        </fastsettLovvalgRequest>
    </xsl:template>

    <xsl:template match="soeknadDokument">
        <søknadDokument>
            <xsl:apply-templates/>
        </søknadDokument>
    </xsl:template>

    <xsl:template match="personDokument">
        <personopplysningDokument>
            <xsl:apply-templates/>
        </personopplysningDokument>
    </xsl:template>

    <xsl:template match="*">
        <xsl:element name="{local-name(.)}">
            <xsl:apply-templates/>
        </xsl:element>
    </xsl:template>

</xsl:stylesheet>