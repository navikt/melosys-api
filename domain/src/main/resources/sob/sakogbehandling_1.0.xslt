<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:sob="http://nav.no/tjeneste/virksomhet/sakOgBehandling/v1">

    <xsl:output method="xml" indent="no"/>

    <xsl:template match="/sob:finnSakOgBehandlingskjedeListeResponse">
        <xsl:apply-templates/>
    </xsl:template>

    <xsl:template match="response">
        <sobSakDokument>
            <xsl:apply-templates/>
        </sobSakDokument>
    </xsl:template>

    <xsl:variable name="SAKSTEMA_BARNETRYGD">BAR</xsl:variable>
    <xsl:variable name="BEHANDLINGSKJEDETYPE_BEHANDLE_SAK">ad0003</xsl:variable>
    <xsl:variable name="BEHANDLINGSTEMA_BARNETRYGD_EØS">ab0058</xsl:variable>

    <xsl:template match="behandlingskjede">
        <behandlingskjede>
            <xsl:apply-templates/>
        </behandlingskjede>
        <xsl:if test="../sakstema = $SAKSTEMA_BARNETRYGD
                      and behandlingskjedetype = $BEHANDLINGSKJEDETYPE_BEHANDLE_SAK
                      and behandlingstema = $BEHANDLINGSTEMA_BARNETRYGD_EØS">
            <xsl:element name="eøsBarnetrygd">true</xsl:element>
        </xsl:if>
    </xsl:template>

    <xsl:template match="sakstema|behandlingskjedetype|behandlingstema">
        <xsl:element name="{local-name(.)}">
            <xsl:value-of select="text()"/>
        </xsl:element>
    </xsl:template>

    <xsl:template match="text()|@*"/>

</xsl:stylesheet>