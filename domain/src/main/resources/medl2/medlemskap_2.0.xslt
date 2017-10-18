<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:ns2="http://nav.no/tjeneste/virksomhet/medlemskap/v2">

    <xsl:output method="xml" indent="no"/>

    <xsl:template match="/|ns2:hentPeriodeListeResponse|response">
        <xsl:apply-templates/>
    </xsl:template>


    <xsl:template match="/">
        <medlemskapDokument>
            <medlemsperiode>
                <xsl:for-each select="ns2:hentPeriodeListeResponse/response/periodeListe">
                    <medlemsperiode>
                        <!-- Inneholder ikke klokkeslett og offset
                        <periode>
                            <fom><xsl:value-of select="fraOgMed" /></fom>
                            <tom><xsl:value-of select="tilOgMed" /></tom>
                        </periode>
                        -->
                        <fom><xsl:value-of select="fraOgMed" /></fom>
                        <tom><xsl:value-of select="tilOgMed" /></tom>
                        <type><xsl:value-of select="type" /></type>

                        <status><xsl:value-of select="status" /></status>
                        <grunnlagstype><xsl:value-of select="grunnlagstype" /></grunnlagstype>

                    </medlemsperiode>
                </xsl:for-each>
            </medlemsperiode>
        </medlemskapDokument>
    </xsl:template>
</xsl:stylesheet>