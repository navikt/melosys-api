<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                xmlns:nsx="http://nav.no/tjeneste/virksomhet/organisasjon/v4">

    <xsl:output method="xml" indent="no"/>

    <xsl:template match="/|nsx:hentOrganisasjonResponse|response">
        <xsl:apply-templates/>
    </xsl:template>

    <xsl:template match="organisasjon">
        <organisasjonDokument>
            <orgnummer><xsl:value-of select="orgnummer"/></orgnummer>
            <navn>
                <xsl:for-each select="navn/navnelinje">
                    <xsl:apply-templates select="."/>
                </xsl:for-each>
            </navn>
            <xsl:for-each select="organisasjonDetaljer">
            <organisasjonDetaljer>
                <xsl:copy-of select="orgnummer"/>
                <xsl:for-each select="forretningsadresse">
                    <xsl:apply-templates select="."/>
                </xsl:for-each>
                <xsl:for-each select="postadresse">
                    <xsl:apply-templates select="."/>
                </xsl:for-each>
                <xsl:for-each select="navn">
                <organisasjonsnavn>
                    <xsl:call-template name="Perioder"/>
                    <navn>
                        <xsl:apply-templates select="navn/*"/>
                    </navn>
                    <redigertNavn><xsl:value-of select="redigertNavn"/></redigertNavn>
                </organisasjonsnavn>
                </xsl:for-each>
                <xsl:apply-templates select="telefon|epostadresse"/>
                <xsl:for-each select="naering">
                    <naering><xsl:value-of select="naeringskode/@kodeRef"/></naering>
                </xsl:for-each>
            </organisasjonDetaljer>
            </xsl:for-each>
            <sektorkode><xsl:value-of select="(juridiskEnhetDetaljer|orgleddDetaljer)/sektorkode/@kodeRef"/></sektorkode>
            <enhetstype><xsl:value-of select="juridiskEnhetDetaljer/enhetstype/@kodeRef"/></enhetstype>
            <oppstartsdato><xsl:value-of select="virksomhetDetaljer/oppstartsdato"/></oppstartsdato>
        </organisasjonDokument>
    </xsl:template>

    <xsl:template match="telefon|epostadresse">
        <xsl:element name="{name()}">
            <identifikator><xsl:value-of select="normalize-space(.)"/></identifikator>
            <xsl:if test="name()='telefon'">
                <type><xsl:value-of select="./type"/></type>
                <retningsnummer><xsl:value-of select="./type"/></retningsnummer>
            </xsl:if>
            <xsl:call-template name="Perioder" />
        </xsl:element>
    </xsl:template>

    <xsl:template match="navnelinje">
        <xsl:if test="normalize-space(.) != ''">
            <xsl:element name="{name()}">
                <xsl:value-of select="."/>
            </xsl:element>
        </xsl:if>
    </xsl:template>

    <xsl:template match="forretningsadresse|postadresse">
        <xsl:choose>
            <xsl:when test="contains(@xsi:type,'SemistrukturertAdresse')">
                <xsl:element name="{name()}">
                    <xsl:attribute name="xsi:type">SemistrukturertAdresse</xsl:attribute>
                    <xsl:call-template name="Perioder"/>
                    <xsl:apply-templates/>
                </xsl:element>
            </xsl:when>
            <xsl:when test="contains(@xsi:type,'Gateadresse')">
                <xsl:element name="{name()}">
                    <xsl:attribute name="xsi:type">Gateadresse</xsl:attribute>
                        <xsl:call-template name="Perioder"/>
                    <xsl:apply-templates select="landkode"/>
                    <xsl:for-each select="./*[name() != 'landkode']">
                        <xsl:call-template name="Gateadresse"/>
                    </xsl:for-each>
                </xsl:element>
            </xsl:when>
            <xsl:otherwise>
                <failed/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template name="Gateadresse">
        <xsl:element name="{name()}">
            <xsl:value-of select="."/>
        </xsl:element>
    </xsl:template>

    <xsl:template match="landkode">
        <xsl:element name="{name()}"><xsl:value-of select="@kodeRef" /></xsl:element>
    </xsl:template>

    <xsl:template match="adresseledd">
        <xsl:element name="{noekkel/@kodeRef}">
            <xsl:value-of select="verdi"/>
        </xsl:element>
    </xsl:template>
    
    <!-- Bruks- og gyldighetsperioder -->
    <xsl:template name="Perioder">
        <xsl:if test="@fomBruksperiode|@tomBruksperiode">
            <bruksperiode>
                <xsl:if test="@fomBruksperiode">
                    <fom><xsl:value-of select="@fomBruksperiode"/></fom>
                </xsl:if>
                <xsl:if test="@tomBruksperiode">
                    <tom><xsl:value-of select="@tomBruksperiode"/></tom>
                </xsl:if>
            </bruksperiode>
        </xsl:if>
        <xsl:if test="@fomGyldighetsperiode|@tomGyldighetsperiode">
            <gyldighetsperiode>
                <xsl:if test="@fomGyldighetsperiode">
                    <fom><xsl:value-of select="@fomGyldighetsperiode"/></fom>
                </xsl:if>
                <xsl:if test="@tomGyldighetsperiode">
                    <tom><xsl:value-of select="@tomGyldighetsperiode"/></tom>
                </xsl:if>
            </gyldighetsperiode>
        </xsl:if>
    </xsl:template>
</xsl:stylesheet>