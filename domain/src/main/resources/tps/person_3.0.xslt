<?xml version="1.0" encoding="UTF-8"?>

<xsl:transform version="2.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:tps3="http://nav.no/tjeneste/virksomhet/person/v3"
>

    <xsl:template match="/tps3:hentPersonResponse/response/person">
        <personopplysningDokument>
            <fnr><xsl:value-of select="aktoer/ident/ident"/></fnr>
            <sivilstand><xsl:value-of select="sivilstand/sivilstand"/></sivilstand>
            <statsborgerskap><xsl:value-of select="statsborgerskap/land"/></statsborgerskap>
            <kjønn><xsl:value-of select="kjoenn/kjoenn"/></kjønn>
            <sammensattNavn><xsl:value-of select="personnavn/sammensattNavn"/></sammensattNavn>
            <fødselsdato><xsl:value-of select="foedselsdato/foedselsdato"/></fødselsdato>
            <dødsdato><xsl:value-of select="doedsdato/doedsdato"/></dødsdato>
            <diskresjonskode><xsl:value-of select="diskresjonskode/diskresjonskode"/></diskresjonskode>
            <personstatus><xsl:value-of select="personstatus/personstatus"/></personstatus>
        </personopplysningDokument>
    </xsl:template>

</xsl:transform>