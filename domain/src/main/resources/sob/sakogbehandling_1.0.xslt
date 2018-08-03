<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:ns2="http://nav.no/tjeneste/virksomhet/sakOgBehandling/v1">

    <xsl:output method="xml" indent="no"/>

    <xsl:template match="/|ns2:finnSakOgBehandlingskjedeListeResponse|response">
        <xsl:apply-templates/>
    </xsl:template>

    <xsl:template match="sak">
        <sobSakDokument>
            <xsl:apply-templates/>
        </sobSakDokument>
    </xsl:template>

    <xsl:template match="behandlingskjede">
        <behandlingskjede>
            <xsl:apply-templates/>
        </behandlingskjede>
    </xsl:template>

    <xsl:template match="sakstema|behandlingskjedetype|behandlingstema">
        <xsl:element name="{local-name(.)}">
            <xsl:value-of select="@kodeRef"/>
        </xsl:element>
    </xsl:template>

</xsl:stylesheet>