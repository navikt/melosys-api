<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
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

    <!-- Inntekt -->
    <xsl:template match="tilleggsinformasjon">
        <tilleggsinformasjon>
            <kategori><xsl:value-of select="kategori" /></kategori>
            <tilleggsinformasjonDetaljer>
                <xsl:attribute name="xsi:type">
                    <xsl:apply-templates select="tilleggsinformasjonDetaljer/@xsi:type"/>
                </xsl:attribute>
                <xsl:apply-templates select="tilleggsinformasjonDetaljer" />
            </tilleggsinformasjonDetaljer>
        </tilleggsinformasjon>
    </xsl:template>

    <xsl:template match="tilleggsinformasjonDetaljer[*]">
        <!-- Kopier verdier under tilleggsinformasjonDetaljer -->
        <xsl:apply-templates />
    </xsl:template>

    <xsl:template match="opptjeningsperiode|tidsrom|etterbetalingsperiode">
        <xsl:element name="{name()}">
            <fom><xsl:value-of select="fom" /></fom>
            <tom><xsl:value-of select="tom" /></tom>
        </xsl:element>
    </xsl:template>
    <!-- /Inntekt -->

    <xsl:template match="*">
        <xsl:element name="{local-name(.)}">
            <xsl:apply-templates/>
        </xsl:element>
    </xsl:template>

</xsl:stylesheet>