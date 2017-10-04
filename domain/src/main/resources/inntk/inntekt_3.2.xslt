<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:ns2="http://nav.no/tjeneste/virksomhet/inntekt/v3">
    <xsl:output method="xml" indent="no"/>

    <xsl:template match="/|ns2:hentInntektListeResponse|response">
        <xsl:apply-templates/>
    </xsl:template>

    <xsl:template match="/ns2:hentInntektListeResponse/response/arbeidsInntektIdent">
        <inntektDokument>
            <xsl:apply-templates/>
        </inntektDokument>
    </xsl:template>

    <xsl:template match="node()">
        <xsl:copy>
            <xsl:apply-templates select="node()" />
        </xsl:copy>
    </xsl:template>

    <xsl:template match="@*"/>


<!--    <xsl:template match="/|*">
        <xsl:element name="{translate(., ' ', '_')}">
            <xsl:apply-templates select="*" />
        </xsl:element>
    </xsl:template>-->


</xsl:stylesheet>