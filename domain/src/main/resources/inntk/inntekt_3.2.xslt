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

    <xsl:template match="arbeidsforholdListe">
        <arbeidsforholdListe>
            <frilansPeriode>
                <fom><xsl:value-of select="frilansPeriode/fom" /></fom>
                <tom><xsl:value-of select="frilansPeriode/tom" /></tom>
            </frilansPeriode>
            <yrke><xsl:value-of select="yrke" /></yrke>
        </arbeidsforholdListe>
    </xsl:template>

    <xsl:template match="opptjeningsperiode">
        <opptjeningsperiode>
            <fom><xsl:value-of select="startDato" /></fom>
            <tom><xsl:value-of select="sluttDato" /></tom>
        </opptjeningsperiode>
    </xsl:template>

    <xsl:template match="tilleggsinformasjon">
        <!-- TODO: Bør forbedres -->
        <tilleggsinformasjon>
            <kategori><xsl:value-of select="kategori" /></kategori>
            <xsl:apply-templates/>
        </tilleggsinformasjon>
    </xsl:template>

    <!-- TODO: Bør forbedres -->
    <xsl:template match="tilleggsinformasjonDetaljer[@xsi:type='ns4:Etterbetalingsperiode']">
        <tilleggsinformasjonDetaljer xsi:type="{substring-after(@xsi:type, 'ns4:')}">
            <etterbetalingsperiode>
                <fom><xsl:value-of select="etterbetalingsperiode/startDato" /></fom>
                <tom><xsl:value-of select="etterbetalingsperiode/sluttDato" /></tom>
            </etterbetalingsperiode>
        </tilleggsinformasjonDetaljer>
    </xsl:template>

    <!-- TODO: Bør forbedres -->
    <xsl:template match="tilleggsinformasjonDetaljer[@xsi:type='ns4:AldersUfoereEtterlatteAvtalefestetOgKrigspensjon']">
        <tilleggsinformasjonDetaljer xsi:type="{substring-after(@xsi:type, 'ns4:')}">
            <tidsrom>
                <fom><xsl:value-of select="tidsrom/startDato" /></fom>
                <tom><xsl:value-of select="tidsrom/sluttDato" /></tom>
            </tidsrom>
        </tilleggsinformasjonDetaljer>
    </xsl:template>

    <xsl:template match="forskuddstrekkListe">
        <!--FIXME-->
    </xsl:template>

    <xsl:template match="fradragListe">
        <!--FIXME-->
    </xsl:template>

    <xsl:template match="ident"/>

</xsl:stylesheet>