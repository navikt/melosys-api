<?xml version="1.0" encoding="UTF-8"?>

<xsl:transform version="2.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xmlns:tps3="http://nav.no/tjeneste/virksomhet/person/v3">

    <xsl:template match="/tps3:hentPersonResponse/response/person">
        <personDokument>
            <fnr><xsl:value-of select="aktoer/ident/ident"/></fnr>
            <sivilstand>
                <kode><xsl:value-of select="sivilstand/sivilstand"/></kode>
            </sivilstand>
            <statsborgerskap>
                <kode><xsl:value-of select="statsborgerskap/land"/></kode>
            </statsborgerskap>
            <kjønn>
                <kode><xsl:value-of select="kjoenn/kjoenn"/></kode>
            </kjønn>
            <fornavn><xsl:value-of select="personnavn/fornavn"/></fornavn>
            <mellomnavn><xsl:value-of select="personnavn/mellomnavn"/></mellomnavn>
            <etternavn><xsl:value-of select="personnavn/etternavn"/></etternavn>
            <sammensattNavn><xsl:value-of select="personnavn/sammensattNavn"/></sammensattNavn>
            <fødselsdato><xsl:value-of select="foedselsdato/foedselsdato"/></fødselsdato>
            <dødsdato><xsl:value-of select="doedsdato/doedsdato"/></dødsdato>
            <diskresjonskode>
                <kode><xsl:value-of select="diskresjonskode"/></kode>
            </diskresjonskode>
            <personstatus><xsl:value-of select="personstatus/personstatus"/></personstatus>
            <xsl:apply-templates />
        </personDokument>
    </xsl:template>

    <xsl:template match="bostedsadresse">
        <bostedsadresse>
            <gateadresse>
                <gatenavn><xsl:value-of select="strukturertAdresse/gatenavn"/></gatenavn>
                <!-- Dersom vi har et tomt nummer-felt blir den parset som 0, og ikke null -->
                <xsl:if test="not(string(strukturertAdresse/gatenummer) = '')">
                    <gatenummer><xsl:value-of select="strukturertAdresse/gatenummer"/></gatenummer>
                </xsl:if>
                <xsl:if test="not(string(strukturertAdresse/husnummer) = '')">
                    <husnummer><xsl:value-of select="strukturertAdresse/husnummer"/></husnummer>
                </xsl:if>
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

    <xsl:template match="midlertidigPostadresse[@xsi:type='ns3:MidlertidigPostadresseUtland']">
        <midlertidigPostadresse>
            <xsl:attribute name="xsi:type"><xsl:value-of select="substring-after(@xsi:type, ':')" /></xsl:attribute>
            <adresselinje1><xsl:value-of select="ustrukturertAdresse/adresselinje1" /></adresselinje1>
            <adresselinje2><xsl:value-of select="ustrukturertAdresse/adresselinje2" /></adresselinje2>
            <adresselinje3><xsl:value-of select="ustrukturertAdresse/adresselinje3" /></adresselinje3>
            <adresselinje4><xsl:value-of select="ustrukturertAdresse/adresselinje4" /></adresselinje4>
            <land>
                <kode><xsl:value-of select="ustrukturertAdresse/landkode"/></kode>
            </land>
        </midlertidigPostadresse>
    </xsl:template>

    <xsl:template match="midlertidigPostadresse[@xsi:type='ns3:MidlertidigPostadresseNorge']">
        <midlertidigPostadresse>
            <xsl:attribute name="xsi:type"><xsl:value-of select="substring-after(@xsi:type, ':')" /></xsl:attribute>
            <!-- FIXME: MidlertidigPostadresseNorge kan også være en matrikkeladresse - trenger testdata -->
            <tilleggsadresse><xsl:value-of select="strukturertAdresse/tilleggsadresse" /></tilleggsadresse>
            <tilleggsadresseType><xsl:value-of select="strukturertAdresse/tilleggsadresseType" /></tilleggsadresseType>
            <gateadresse>
                <gatenavn><xsl:value-of select="strukturertAdresse/gatenavn"/></gatenavn>
                <xsl:if test="not(string(strukturertAdresse/gatenummer) = '')">
                    <gatenummer><xsl:value-of select="strukturertAdresse/gatenummer"/></gatenummer>
                </xsl:if>
                <xsl:if test="not(string(strukturertAdresse/husnummer) = '')">
                    <husnummer><xsl:value-of select="strukturertAdresse/husnummer"/></husnummer>
                </xsl:if>
                <husbokstav><xsl:value-of select="strukturertAdresse/husbokstav"/></husbokstav>
            </gateadresse>
            <poststed><xsl:value-of select="strukturertAdresse/poststed"/></poststed>
            <land>
                <kode><xsl:value-of select="strukturertAdresse/landkode"/></kode>
            </land>
        </midlertidigPostadresse>
    </xsl:template>

    <xsl:template match="harFraRolleI">
        <familiemedlemmer>
            <fnr><xsl:value-of select="tilPerson/aktoer/ident/ident" /></fnr>
            <navn><xsl:value-of select="tilPerson/personnavn/sammensattNavn" /></navn>
            <familierelasjon><xsl:value-of select="tilRolle" /></familierelasjon>
        </familiemedlemmer>
    </xsl:template>

    <xsl:template match="text()"/>

</xsl:transform>