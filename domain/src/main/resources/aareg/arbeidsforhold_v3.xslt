<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:ns2="http://nav.no/tjeneste/virksomhet/arbeidsforhold/v3">

    <xsl:output method="xml" indent="no"/>

    <xsl:template match="/">
        <arbeidsforholdDokument>
            <arbeidsforhold>
                <xsl:for-each select="ns2:finnArbeidsforholdPrArbeidstakerResponse/parameters/arbeidsforhold">
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
                                <arbeidstidsordning><xsl:value-of select="arbeidstidsordning" /></arbeidstidsordning>
                                <avloenningstype><xsl:value-of select="avloenningstype" /></avloenningstype>
                                <yrke><xsl:value-of select="yrke" /></yrke>
                                <avtaltArbeidstimerPerUke><xsl:value-of select="avtaltArbeidstimerPerUke" /></avtaltArbeidstimerPerUke>
                                <stillingsprosent><xsl:value-of select="stillingsprosent" /></stillingsprosent>
                                <sisteLoennsendringsdato><xsl:value-of select="sisteLoennsendringsdato" /></sisteLoennsendringsdato>
                                <beregnetAntallTimerPrUke><xsl:value-of select="beregnetAntallTimerPrUke" /></beregnetAntallTimerPrUke>
                                <endringsdatoStillingsprosent><xsl:value-of select="endringsdatoStillingsprosent" /></endringsdatoStillingsprosent>
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
                        <utenlandsopphold>
                        <xsl:for-each select="utenlandsopphold">
                            <opphold>
                                <periode>
                                    <fom><xsl:value-of select="periode/fom" /></fom>
                                    <tom><xsl:value-of select="periode/tom" /></tom>
                                </periode>
                                <land><xsl:value-of select="land" /></land>
                            </opphold>
                        </xsl:for-each>
                        </utenlandsopphold>
                        <arbeidsgiverID><xsl:value-of select="arbeidsgiver/orgnummer" /></arbeidsgiverID>
                        <arbeidstakerID><xsl:value-of select="arbeidstaker/ident/ident" /></arbeidstakerID>
                        <opplysningspliktigID><xsl:value-of select="opplysningspliktig/orgnummer" /></opplysningspliktigID>
                        <arbeidsforholdInnrapportertEtterAOrdningen><xsl:value-of select="arbeidsforholdInnrapportertEtterAOrdningen" /></arbeidsforholdInnrapportertEtterAOrdningen>
                    </arbeidsforhold>
                </xsl:for-each>
            </arbeidsforhold>
        </arbeidsforholdDokument>
    </xsl:template>
</xsl:stylesheet>