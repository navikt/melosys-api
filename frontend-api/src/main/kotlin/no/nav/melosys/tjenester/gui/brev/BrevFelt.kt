package no.nav.melosys.tjenester.gui.brev

import no.nav.melosys.domain.kodeverk.brev.Distribusjonstype
import no.nav.melosys.tjenester.gui.dto.brev.*

internal object BrevFelt {
    val FELT_INNHENTINGBREVFORMTITTEL = BrevmalFeltDto.Builder()
        .medKodeOgBeskrivelse(BrevmalFeltKode.STANDARDTEKST_INNHENTINGBREVFORMTITTEL)
        .medFeltType(FeltType.FORMTITTEL)
        .build()

    val FELT_MANGLER_FRITEKST = BrevmalFeltDto.Builder()
        .medKodeOgBeskrivelse(BrevmalFeltKode.MANGLER_FRITEKST)
        .medFeltType(FeltType.FRITEKST)
        .erPåkrevd()
        .build()

    val FELT_STANDARDTEKST_SJEKKBOKS = BrevmalFeltDto.Builder()
        .medKodeOgBeskrivelse(BrevmalFeltKode.STANDARDTEKST_KONTAKTINFORMASJON)
        .medFeltType(FeltType.SJEKKBOKS)
        .build()

    val FELT_DISTRIBUSJONSTYPE = BrevmalFeltDto.Builder()
        .medKodeOgBeskrivelse(BrevmalFeltKode.DISTRIBUSJONSTYPE)
        .medHjelpetekst("Type brev må angis slik at bruker får riktig varseltekst om brevet som sendes. Gjelder det et vedtak eller en forespørsel, vil bruker få en påminnelse hvis brevet ikke har blitt lest innen 7 dager.")
        .medValg(hentDistribusjonstyper())
        .erPåkrevd()
        .build()

    val FELT_DOKUMENT_TITTEL = BrevmalFeltDto.Builder()
        .medKodeOgBeskrivelse(BrevmalFeltKode.DOKUMENT_TITTEL)
        .medFeltType(FeltType.TEKST)
        .medHjelpetekst("Tittelen du skriver inn her vil bli journalføringstittel.")
        .medTegnBegrensning(60)
        .build()

    val FELT_FRITEKST = BrevmalFeltDto.Builder()
        .medKodeOgBeskrivelse(BrevmalFeltKode.FRITEKST)
        .medHjelpetekst("Teksten du skriver inn her vil være hovedteksten i brevet du lager.")
        .medFeltType(FeltType.FRITEKST)
        .erPåkrevd()
        .build()

    val FELT_VEDLEGG = BrevmalFeltDto.Builder()
        .medKodeOgBeskrivelse(BrevmalFeltKode.VEDLEGG)
        .medFeltType(FeltType.VEDLEGG)
        .build()

    val FELT_FRITEKSTVEDLEGG = BrevmalFeltDto.Builder()
        .medKodeOgBeskrivelse(BrevmalFeltKode.FRITEKSTVEDLEGG)
        .medFeltType(FeltType.FRITEKSTVEDLEGG)
        .build()

    fun lagUtenlandskTrygdemyndighetMottakerFelt(valg: FeltValgDto?): BrevmalFeltDto {
        return BrevmalFeltDto.Builder()
            .medKodeOgBeskrivelse(BrevmalFeltKode.UTENLANDSK_TRYGDEMYNDIGHET_MOTTAKER)
            .medValg(valg)
            .erPåkrevd()
            .build()
    }

    fun lagBrevTittelFelt(valg: FeltValgDto?): BrevmalFeltDto {
        return BrevmalFeltDto.Builder()
            .medKodeOgBeskrivelse(BrevmalFeltKode.BREV_TITTEL)
            .medFeltType(FeltType.TEKST)
            .medValg(valg)
            .medTegnBegrensning(60)
            .erPåkrevd()
            .build()
    }

    fun lagErstatterStandardtekstRadioFritekst(vararg feltvalgAlternativDtos: FeltvalgAlternativDto?): BrevmalFeltDto {
        val feltvalgAlternativ = mutableListOf(
            *feltvalgAlternativDtos,
            FeltvalgAlternativDto(
                FeltvalgAlternativKode.FRITEKST.kode,
                "Fritekst (erstatter standardtekst)",
                true
            )
        )
        return BrevmalFeltDto.Builder()
            .medKodeOgBeskrivelse(BrevmalFeltKode.INNLEDNING_FRITEKST)
            .medFeltType(FeltType.FRITEKST)
            .erPåkrevd()
            .medValg(FeltValgDto(feltvalgAlternativ, FeltValgType.RADIO))
            .build()
    }

    private fun hentDistribusjonstyper(): FeltValgDto {
        val distribusjonstyper = listOf(
            FeltvalgAlternativDto(Distribusjonstype.VEDTAK.kode, Distribusjonstype.VEDTAK.beskrivelse, false),
            FeltvalgAlternativDto(Distribusjonstype.VIKTIG.kode, Distribusjonstype.VIKTIG.beskrivelse, false),
            FeltvalgAlternativDto(Distribusjonstype.ANNET.kode, Distribusjonstype.ANNET.beskrivelse, false)
        )
        return FeltValgDto(distribusjonstyper, FeltValgType.RADIO)
    }
}
