<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:output method="xml" indent="no"/>

    <xsl:template match="/">
        <utbetalingDokument>
            <utbetalinger>
                <xsl:apply-templates/>
            </utbetalinger>
        </utbetalingDokument>
    </xsl:template>

    <xsl:template match="utbetalingListe">
        <utbetaling>
            <ytelser>
                <xsl:apply-templates select="ytelseListe"/>
            </ytelser>
        </utbetaling>
    </xsl:template>

    <xsl:template match="ytelseListe">
        <ytelse>
            <type>
                <xsl:value-of select="ytelsestype"/>
            </type>
            <periode>
                <xsl:apply-templates select="ytelsesperiode"/>
            </periode>
        </ytelse>
    </xsl:template>

    <xsl:template match="ytelsesperiode">
        <fom>
            <xsl:value-of select="fom"/>
        </fom>
        <tom>
            <xsl:value-of select="tom"/>
        </tom>
    </xsl:template>

    <!-- Fjerner uønsket output (Tekst som ikke befinner seg i en tag) -->
    <xsl:template match="text()"/>
</xsl:stylesheet>