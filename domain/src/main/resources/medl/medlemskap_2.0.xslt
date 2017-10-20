<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:ns2="http://nav.no/tjeneste/virksomhet/medlemskap/v2">

    <xsl:output method="xml" indent="no"/>

    <xsl:template match="/|ns2:hentPeriodeListeResponse|response">
        <xsl:apply-templates/>
    </xsl:template>

    <xsl:template name="simple" match="/ns2:hentPeriodeListeResponse/response/periodeListe/*">
        <xsl:element name="{name()}">
            <xsl:value-of select="."/>
        </xsl:element>
    </xsl:template>

    <xsl:template match="/">
        <medlemskapDokument>
            <medlemsperiode>
                <xsl:for-each select="ns2:hentPeriodeListeResponse/response/periodeListe">
                    <medlemsperiode>
                        <periode>
                            <fom><xsl:value-of select ="concat(fraOgMed, 'T00:00:00.000+00:00')" /></fom>
                            <tom><xsl:value-of select ="concat(tilOgMed, 'T00:00:00.000+00:00')" /></tom>
                        </periode>

                        <xsl:apply-templates select="type|status|grunnlagstype|land" />
                        <xsl:apply-templates select="lovvalg|trygdedekning|kildedokumenttype|kilde" />
                    </medlemsperiode>
                </xsl:for-each>
            </medlemsperiode>
        </medlemskapDokument>
    </xsl:template>
</xsl:stylesheet>