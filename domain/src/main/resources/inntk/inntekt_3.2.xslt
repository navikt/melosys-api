<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:ns2="http://nav.no/tjeneste/virksomhet/inntekt/v3"
                xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
    <xsl:output method="xml" indent="no"/>

    <xsl:template match="/|ns2:hentInntektListeResponse|response">
        <xsl:apply-templates/>
    </xsl:template>

    <xsl:template match="/ns2:hentInntektListeResponse/response/arbeidsInntektIdent">
        <inntektDokument>
            <arbeidsInntektMaanedListe>
                <xsl:apply-templates/>
            </arbeidsInntektMaanedListe>
        </inntektDokument>
    </xsl:template>

    <xsl:template match="*">
        <xsl:element name="{local-name(.)}">
            <xsl:apply-templates/>
        </xsl:element>
    </xsl:template>

    <xsl:template match="@xsi:*">
        <xsl:value-of select="substring-after(., 'ns4:')"/>
    </xsl:template>

    <xsl:template match="@*"/>

    <xsl:template match="inntektListe">
        <xsl:element name="{name()}">
            <xsl:attribute name="xsi:type">
                <xsl:apply-templates select="@*"/>
            </xsl:attribute>
            <xsl:apply-templates/>
        </xsl:element>
    </xsl:template>

    <xsl:template match="opplysningspliktig|virksomhet">
        <xsl:element name="{concat(name(), 'ID')}">
            <xsl:value-of select="./orgnummer"/>
        </xsl:element>
    </xsl:template>

    <xsl:template match="inntektsmottaker|inntektsinnsender">
        <xsl:element name="{concat(name(), 'ID')}">
            <xsl:value-of select="./personIdent"/>
        </xsl:element>
    </xsl:template>

    <xsl:template match="tilleggsinformasjon">
        <!--FIXME-->
    </xsl:template>

    <xsl:template match="forskuddstrekkListe">
        <!--FIXME-->
    </xsl:template>

    <xsl:template match="fradragListe">
        <!--FIXME-->
    </xsl:template>

    <xsl:template match="ident"/>

</xsl:stylesheet>