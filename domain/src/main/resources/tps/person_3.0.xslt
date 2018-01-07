<?xml version="1.0" encoding="UTF-8"?>

<xsl:transform version="2.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xmlns:a="http://nav.no/tjeneste/virksomhet/person/v3/informasjon"
    xmlns:tps3="http://nav.no/tjeneste/virksomhet/person/v3">

    <xsl:template match="/tps3:hentPersonResponse/response/person">
        <personDokument>
            <fnr><xsl:value-of select="aktoer/ident/ident"/></fnr>
            <sivilstand><xsl:value-of select="sivilstand/sivilstand"/></sivilstand>
            <statsborgerskap>
                <kode><xsl:value-of select="statsborgerskap/land"/></kode>
            </statsborgerskap>
            <kjønn><xsl:value-of select="kjoenn/kjoenn"/></kjønn>
            <sammensattNavn><xsl:value-of select="personnavn/sammensattNavn"/></sammensattNavn>
            <fødselsdato><xsl:value-of select="foedselsdato/foedselsdato"/></fødselsdato>
            <dødsdato><xsl:value-of select="doedsdato/doedsdato"/></dødsdato>
            <diskresjonskode><xsl:value-of select="diskresjonskode/diskresjonskode"/></diskresjonskode>
            <personstatus><xsl:value-of select="personstatus/personstatus"/></personstatus>
            <xsl:apply-templates />
        </personDokument>
    </xsl:template>

    <xsl:template match="bostedsadresse">
        <bostedsadresse>
            <gateadresse>
                <gatenavn><xsl:value-of select="strukturertAdresse/gatenavn"/></gatenavn>
                <gatenummer><xsl:value-of select="strukturertAdresse/gatenummer"/></gatenummer>
                <husnummer><xsl:value-of select="strukturertAdresse/husnummer"/></husnummer>
                <husbokstav><xsl:value-of select="strukturertAdresse/husbokstav"/></husbokstav>
            </gateadresse>
            <postnr><xsl:value-of select="strukturertAdresse/poststed"/></postnr>
            <land>
                <kode><xsl:value-of select="strukturertAdresse/landkode"/></kode>
            </land>
        </bostedsadresse>
    </xsl:template>

    <xsl:template match="postadresse">
        <postadresse>
            <adresselinje1><xsl:value-of select="ustrukturertAdresse/adresselinje1" /></adresselinje1>
            <adresselinje2><xsl:value-of select="ustrukturertAdresse/adresselinje2" /></adresselinje2>
            <adresselinje3><xsl:value-of select="ustrukturertAdresse/adresselinje3" /></adresselinje3>
            <adresselinje4><xsl:value-of select="ustrukturertAdresse/adresselinje4" /></adresselinje4>
            <land>
                <kode><xsl:value-of select="ustrukturertAdresse/landkode"/></kode>
            </land>
        </postadresse>
    </xsl:template>

    <!--<xsl:template match="midlertidigPostadresse[@xsi:type=a:MidlertidigPostadresseUtland]">-->
    <xsl:template match="midlertidigPostadresse">
        <midlertidigPostadresse>
            <!--<xsl:attribute name="xsi:type">
                <xsl:apply-templates select="@xsi:type"/>
            </xsl:attribute>-->
            <xsl:attribute name="xsi:type">MidlertidigPostadresseUtland</xsl:attribute>
            <!--<test><xsl:value-of select="substring-after(':', @xsi:type)" /></test>-->
            <ustrukturertAdresse>
                <!--<test><xsl:value-of select="substring-after(':', @xsi:type)" /></test>-->
                <adresselinje1><xsl:value-of select="ustrukturertAdresse/adresselinje1" /></adresselinje1>
                <adresselinje2><xsl:value-of select="ustrukturertAdresse/adresselinje2" /></adresselinje2>
                <adresselinje3><xsl:value-of select="ustrukturertAdresse/adresselinje3" /></adresselinje3>
                <adresselinje4><xsl:value-of select="substring-after(':', @xsi:type)" /></adresselinje4>
                <land>
                    <kode><xsl:value-of select="ustrukturertAdresse/landkode"/></kode>
                </land>
            </ustrukturertAdresse>
        </midlertidigPostadresse>
    </xsl:template>

    <xsl:template match="harFraRolleI">
        <familierelasjoner>
            <fnr><xsl:value-of select="tilPerson/aktoer/ident/ident" /></fnr>
            <navn><xsl:value-of select="tilPerson/personnavn/sammensattNavn" /></navn>
            <relasjon><xsl:value-of select="tilRolle" /></relasjon>
        </familierelasjoner>
    </xsl:template>

</xsl:transform>