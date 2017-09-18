<?xml version="1.0"?>

<xsl:transform version="2.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:a="http://nav.no/tjeneste/virksomhet/person/v3/informasjon"
    xmlns:b="http://nav.no/tjeneste/virksomhet/person/v3"
    xmlns:c="http://www.w3.org/2001/XMLSchema-instance"
>

    <xsl:template match="/">

    
        <xsl:for-each select="b:hentPersonResponse/response/person">
            <personopplysningDokument>
                <fnr><xsl:value-of select="aktoer/ident/ident"/></fnr>
                <sivilstand><xsl:value-of select="sivilstand/sivilstand"/></sivilstand>
                <statsborgerskap><xsl:value-of select="statsborgerskap/land"/></statsborgerskap>
                <kjønn><xsl:value-of select="kjoenn/kjoenn"/></kjønn>
                <sammensattNavn><xsl:value-of select="personnavn/sammensattNavn"/></sammensattNavn>
                <!-- <fødselsdato><xsl:value-of select="foedselsdato/foedselsdato"/></fødselsdato> -->
                <!-- <dødsdato><xsl:value-of select="doedsdato/doedsdato"/></dødsdato> -->
                <diskresjonskode><xsl:value-of select="diskresjonskode/diskresjonskode"/></diskresjonskode>
                <personstatus><xsl:value-of select="personstatus/personstatus"/></personstatus>
               
           </personopplysningDokument>

        </xsl:for-each>
    

    </xsl:template>

</xsl:transform>