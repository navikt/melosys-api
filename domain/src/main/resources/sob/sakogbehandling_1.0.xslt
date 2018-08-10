<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:ns2="http://nav.no/tjeneste/virksomhet/sakOgBehandling/v1">

    <xsl:output method="xml" indent="no"/>

    <xsl:template match="/|ns2:finnSakOgBehandlingskjedeListeResponse|response">
        <xsl:apply-templates/>
    </xsl:template>

    <xsl:variable name="SAKSTEMA_BARNETRYGD">Barnetrygd</xsl:variable>
    <xsl:variable name="BEHANDLINGSKJEDETYPE_BEHANDLE_SAK">ad0003</xsl:variable>
    <xsl:variable name="BEHANDLINGSTEMA_BARNETRYGD_EØS">ab0058</xsl:variable>

    <xsl:template match="sak">
        <sobSakDokument>
            <xsl:apply-templates/>
        </sobSakDokument>
    </xsl:template>

    <xsl:template match="behandlingskjede">
        <behandlingskjede>
            <xsl:apply-templates/>
        </behandlingskjede>
        <xsl:if test="../sakstema/@kodeRef = $SAKSTEMA_BARNETRYGD
                      and behandlingskjedetype/@kodeRef = $BEHANDLINGSKJEDETYPE_BEHANDLE_SAK
                      and behandlingstema/@kodeRef = $BEHANDLINGSTEMA_BARNETRYGD_EØS">
            <xsl:element name="eøsBarnetrygd">true</xsl:element>
        </xsl:if>
    </xsl:template>

    <xsl:template match="sakstema|behandlingskjedetype|behandlingstema">
        <xsl:element name="{local-name(.)}">
            <xsl:value-of select="@kodeRef"/>
        </xsl:element>
    </xsl:template>

    <xsl:template match="text()|@*"/>

</xsl:stylesheet>