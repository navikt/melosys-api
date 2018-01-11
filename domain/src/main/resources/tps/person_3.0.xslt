<?xml version="1.0" encoding="UTF-8"?>

<xsl:transform version="2.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:tps3="http://nav.no/tjeneste/virksomhet/person/v3"
>

    <xsl:template match="/tps3:hentPersonResponse/response/person">
        <personDokument>
            <bostedsadresse>
                <gateadresse>
                    <gatenavn><xsl:value-of select="bostedsadresse/strukturertAdresse/gatenavn"/></gatenavn>
                    <gatenummer><xsl:value-of select="bostedsadresse/strukturertAdresse/gatenummer"/></gatenummer>
                    <husnummer><xsl:value-of select="bostedsadresse/strukturertAdresse/husnummer"/></husnummer>
                    <husbokstav><xsl:value-of select="bostedsadresse/strukturertAdresse/husbokstav"/></husbokstav>
                </gateadresse>
                <postnr><xsl:value-of select="bostedsadresse/strukturertAdresse/poststed"/></postnr>
                <land>
                    <kode><xsl:value-of select="bostedsadresse/strukturertAdresse/landkode"/></kode>
                </land>
            </bostedsadresse>
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
        </personDokument>
    </xsl:template>

</xsl:transform>