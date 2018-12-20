<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                xmlns:ns2="http://nav.no/tjeneste/virksomhet/arbeidsforhold/v3">

    <xsl:output method="xml" indent="no"/>

    <xsl:template match="/">
        <arbeidsforholdDokument>
            <arbeidsforhold>
                <xsl:for-each select="(ns2:finnArbeidsforholdPrArbeidstakerResponse|ns2:hentArbeidsforholdHistorikkResponse)/parameters/arbeidsforhold">
                    <arbeidsforhold>
                        <arbeidsforholdID><xsl:value-of select="arbeidsforholdID" /></arbeidsforholdID>
                        <arbeidsforholdIDnav><xsl:value-of select="arbeidsforholdIDnav" /></arbeidsforholdIDnav>
                        <ansettelsesPeriode>
                            <fom><xsl:value-of select="ansettelsesPeriode/periode/fom" /></fom>
                            <tom><xsl:value-of select="ansettelsesPeriode/periode/tom" /></tom>
                        </ansettelsesPeriode>
                        <arbeidsforholdstype><xsl:value-of select="arbeidsforholdstype" /></arbeidsforholdstype>
                        <arbeidsavtaler>
                        <xsl:for-each select="arbeidsavtale">
                            <avtale>
                                <gyldighetsperiode>
                                    <fom><xsl:value-of select="@fomGyldighetsperiode"/></fom>
                                    <tom><xsl:value-of select="@tomGyldighetsperiode"/></tom>
                                </gyldighetsperiode>
                                <arbeidstidsordning>
                                    <kode><xsl:value-of select="arbeidstidsordning/@kodeRef" /></kode>
                                </arbeidstidsordning>
                                <avloenningstype><xsl:value-of select="avloenningstype" /></avloenningstype>
                                <yrke>
                                    <kode><xsl:value-of select="yrke/@kodeRef" /></kode>
                                    <term><xsl:value-of select="yrke" /></term>
                                </yrke>
                                <avtaltArbeidstimerPerUke><xsl:value-of select="avtaltArbeidstimerPerUke" /></avtaltArbeidstimerPerUke>
                                <stillingsprosent><xsl:value-of select="stillingsprosent" /></stillingsprosent>
                                <sisteLoennsendringsdato><xsl:value-of select="sisteLoennsendringsdato" /></sisteLoennsendringsdato>
                                <beregnetAntallTimerPrUke><xsl:value-of select="beregnetAntallTimerPrUke" /></beregnetAntallTimerPrUke>
                                <endringsdatoStillingsprosent><xsl:value-of select="endringsdatoStillingsprosent" /></endringsdatoStillingsprosent>
                                <xsl:choose>
                                    <xsl:when test="@xsi:type='ns4:MaritimArbeidsavtale'">
                                        <maritimArbeidsavtale>true</maritimArbeidsavtale>
                                        <fartsområde>
                                            <xsl:value-of select="fartsomraade/@kodeRef" />
                                        </fartsområde>
                                        <skipsregister>
                                            <kode><xsl:value-of select="skipsregister/@kodeRef" /></kode>
                                        </skipsregister>
                                        <skipstype>
                                            <kode><xsl:value-of select="skipstype/@kodeRef" /></kode>
                                        </skipstype>
                                    </xsl:when>
                                    <xsl:otherwise>
                                        <maritimArbeidsavtale>false</maritimArbeidsavtale>
                                    </xsl:otherwise>
                                </xsl:choose>
                                <beregnetStillingsprosent><xsl:value-of select="beregnetStillingsprosent" /></beregnetStillingsprosent>
                                <antallTimerGammeltAa><xsl:value-of select="antallTimerGammeltAa" /></antallTimerGammeltAa>
                            </avtale>
                        </xsl:for-each>
                        </arbeidsavtaler>
                        <permisjonOgPermittering>
                        <xsl:for-each select="permisjonOgPermittering">
                            <permisjonOgPermittering>
                                <permisjonsId><xsl:value-of select="permisjonsId" /></permisjonsId>
                                <permisjonsPeriode>
                                    <fom><xsl:value-of select="permisjonsPeriode/fom" /></fom>
                                    <tom><xsl:value-of select="permisjonsPeriode/tom" /></tom>
                                </permisjonsPeriode>
                                <permisjonsprosent><xsl:value-of select="permisjonsprosent" /></permisjonsprosent>
                                <permisjonOgPermittering><xsl:value-of select="permisjonOgPermittering" /></permisjonOgPermittering>
                            </permisjonOgPermittering>
                        </xsl:for-each>
                        </permisjonOgPermittering>
                        <arbeidstakerID><xsl:value-of select="arbeidstaker/ident/ident" /></arbeidstakerID>
                        <arbeidsforholdInnrapportertEtterAOrdningen><xsl:value-of select="arbeidsforholdInnrapportertEtterAOrdningen" /></arbeidsforholdInnrapportertEtterAOrdningen>
                        <opprettelsestidspunkt><xsl:value-of select="@opprettelsestidspunkt"/></opprettelsestidspunkt>
                        <sistBekreftet><xsl:value-of select="@sistBekreftet"/></sistBekreftet>
                        <xsl:apply-templates />
                    </arbeidsforhold>
                </xsl:for-each>
            </arbeidsforhold>
        </arbeidsforholdDokument>
    </xsl:template>

    <xsl:template match="arbeidsgiver|opplysningspliktig">
        <xsl:element name="{concat(name(), 'type')}">
            <xsl:value-of select="substring-after(@xsi:type, 'ns4:')" />
        </xsl:element>
        <xsl:element name="{concat(name(), 'ID')}">
            <xsl:choose>
                <xsl:when test="@xsi:type='ns4:Organisasjon'">
                    <xsl:value-of select="orgnummer" />
                </xsl:when>
                <xsl:when test="@xsi:type='ns4:Person'">
                    <xsl:value-of select="ident/ident" />
                </xsl:when>
                <xsl:when test="@xsi:type='ns4:HistoriskArbeidsgiverMedArbeidsgivernummer'">
                    <xsl:value-of select="arbeidsgivernummer" />
                </xsl:when>
            </xsl:choose>
        </xsl:element>
    </xsl:template>

    <xsl:template match="utenlandsopphold|antallTimerForTimeloennet">
        <xsl:for-each select=".">
            <xsl:element name="{name()}">
                <periode>
                    <fom><xsl:value-of select="periode/fom" /></fom>
                    <tom><xsl:value-of select="periode/tom" /></tom>
                </periode>
                <xsl:if test="name()='utenlandsopphold'">
                    <land><xsl:value-of select="land" /></land>
                </xsl:if>
                <xsl:if test="name()='antallTimerForTimeloennet'">
                    <antallTimer><xsl:value-of select="antallTimer" /></antallTimer>
                </xsl:if>
                <rapporteringsperiode><xsl:value-of select="rapporteringsperiode" /></rapporteringsperiode>
            </xsl:element>
        </xsl:for-each>
    </xsl:template>
</xsl:stylesheet>