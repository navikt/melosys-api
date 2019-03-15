<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:ns2="http://nav.no/tjeneste/virksomhet/medlemskap/v2">

    <xsl:output method="xml" indent="no"/>
    <xsl:variable name="smallCase" select="'abcdefghijklmnopqrstuvwxyzåæø'"/>
    <xsl:variable name="upperCase" select="'ABCDEFGHIJKLMNOPQRSTUVWXYZÅÆØ'"/>

    <xsl:template match="/|ns2:hentPeriodeListeResponse|response">
        <xsl:apply-templates/>
    </xsl:template>

    <xsl:template match="response">
        <medlemskapDokument>
            <medlemsperiode>
                <xsl:apply-templates />
            </medlemsperiode>
        </medlemskapDokument>
    </xsl:template>

    <xsl:template match="periodeListe">
        <medlemsperiode>
            <periode>
                <fom><xsl:value-of select ="fraOgMed" /></fom>
                <tom><xsl:value-of select ="tilOgMed" /></tom>
            </periode>
            <xsl:apply-templates/>
        </medlemsperiode>
    </xsl:template>

    <xsl:template match="grunnlagstype">
        <xsl:element name="{name()}">
            <xsl:value-of select="translate(., $smallCase, $upperCase)"/>
        </xsl:element>
    </xsl:template>

    <xsl:template match="id|type|status|land|lovvalg|kildedokumenttype|kilde|trygdedekning">
        <xsl:element name="{name()}">
            <xsl:value-of select="."/>
        </xsl:element>
    </xsl:template>

    <!-- Fjerner uønsket output (Tekst som ikke befinner seg i en tag) -->
    <xsl:template match="text()"/>

</xsl:stylesheet>
