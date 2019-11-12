<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:ns2="http://nav.no/tjeneste/virksomhet/person/v3"
                xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">

    <xsl:output method="xml" indent="no"/>

    <xsl:template match="/ns2:hentPersonhistorikkResponse">
        <xsl:apply-templates/>
    </xsl:template>

    <xsl:template match="response">
        <personhistorikkDokument>
            <xsl:apply-templates>
                <xsl:sort select="@endringstidspunkt" order="descending"/>
            </xsl:apply-templates>
        </personhistorikkDokument>
    </xsl:template>

    <xsl:template match="aktoer|personstatusListe"/>

    <xsl:template match="statsborgerskapListe">
        <xsl:element name="{local-name(.)}">
            <xsl:apply-templates select="*|@endretAv|@endringstidspunkt"/>
        </xsl:element>
    </xsl:template>

    <xsl:template match="bostedsadressePeriodeListe|postadressePeriodeListe">
        <xsl:element name="{local-name(.)}">
            <xsl:apply-templates select="*|@endringstidspunkt"/>
        </xsl:element>
    </xsl:template>

    <xsl:template match="midlertidigAdressePeriodeListe[@xsi:type='ns3:MidlertidigPostadresseUtland']">
        <midlertidigAdressePeriodeListe>
            <xsl:attribute name="xsi:type">MidlertidigPostadresseUtland</xsl:attribute>
            <xsl:apply-templates select="*|@endringstidspunkt"/>
        </midlertidigAdressePeriodeListe>
    </xsl:template>

    <xsl:template match="midlertidigAdressePeriodeListe[@xsi:type='ns3:MidlertidigPostadresseNorge']">
        <midlertidigAdressePeriodeListe>
            <xsl:attribute name="xsi:type">MidlertidigPostadresseNorge</xsl:attribute>
            <xsl:apply-templates select="strukturertAdresse/*"/>
            <xsl:apply-templates select="*|@endringstidspunkt"/>
        </midlertidigAdressePeriodeListe>
    </xsl:template>

    <xsl:template match="bostedsadresse">
        <bostedsadresse>
            <postnr><xsl:value-of select="strukturertAdresse/poststed"/></postnr>
            <xsl:apply-templates select="strukturertAdresse/*"/>
            <xsl:apply-templates/>
        </bostedsadresse>
    </xsl:template>

    <xsl:template match="strukturertAdresse[@xsi:type='ns3:Gateadresse']">
        <gateadresse>
            <gatenavn><xsl:value-of select="gatenavn"/></gatenavn>
            <gatenummer><xsl:value-of select="gatenummer"/></gatenummer>
            <husnummer><xsl:value-of select="husnummer"/></husnummer>
            <husbokstav><xsl:value-of select="husbokstav"/></husbokstav>
        </gateadresse>
    </xsl:template>

    <xsl:template match="postadresse">
        <postadresse>
            <xsl:apply-templates/>
        </postadresse>
    </xsl:template>

    <xsl:template match="strukturertAdresse/tilleggsadresse|tilleggsadresseType">
        <xsl:element name="{local-name(.)}">
            <xsl:value-of select="."/>
        </xsl:element>
    </xsl:template>

    <xsl:template match="ustrukturertAdresse/adresselinje1|adresselinje2|adresselinje3|adresselinje4|postnr|poststed">
        <xsl:element name="{local-name(.)}">
            <xsl:value-of select="."/>
        </xsl:element>
    </xsl:template>

    <xsl:template match="strukturertAdresse/landkode|ustrukturertAdresse/landkode">
        <land>
            <kode><xsl:value-of select="."/></kode>
        </land>
    </xsl:template>

    <xsl:template match="statsborgerskap/land">
        <statsborgerskap>
            <kode><xsl:value-of select="."/></kode>
        </statsborgerskap>
    </xsl:template>

    <xsl:template match="@endretAv">
        <endretAv>
            <xsl:value-of select="."/>
        </endretAv>
    </xsl:template>

    <xsl:template match="@endringstidspunkt">
        <endringstidspunkt>
            <xsl:value-of select="."/>
        </endringstidspunkt>
    </xsl:template>

    <xsl:template match="periode">
        <periode>
            <fom><xsl:value-of select ="fom" /></fom>
            <tom><xsl:value-of select ="tom" /></tom>
        </periode>
    </xsl:template>

    <xsl:template match="postleveringsPeriode" >
        <postleveringsPeriode>
            <fom><xsl:value-of select ="fom" /></fom>
            <tom><xsl:value-of select ="tom" /></tom>
        </postleveringsPeriode>
    </xsl:template>

</xsl:stylesheet>