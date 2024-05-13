package no.nav.melosys.tjenester.gui.brev;

import no.nav.melosys.domain.kodeverk.brev.Distribusjonstype;
import no.nav.melosys.tjenester.gui.dto.brev.*;

import java.util.List;

class BrevFelt {

    private BrevFelt() {}

    static final BrevmalFeltDto FELT_MANGLER_FRITEKST = new BrevmalFeltDto.Builder()
        .medKodeOgBeskrivelse(BrevmalFeltKode.MANGLER_FRITEKST)
        .medFeltType(FeltType.FRITEKST)
        .erPåkrevd()
        .build();
    static final BrevmalFeltDto FELT_STANDARDTEKST_SJEKKBOKS = new BrevmalFeltDto.Builder()
        .medKodeOgBeskrivelse(BrevmalFeltKode.STANDARDTEKST_KONTAKTINFORMASJON)
        .medFeltType(FeltType.SJEKKBOKS)
        .build();
    static final BrevmalFeltDto FELT_DISTRIBUSJONSTYPE = new BrevmalFeltDto.Builder()
        .medKodeOgBeskrivelse(BrevmalFeltKode.DISTRIBUSJONSTYPE)
        .medHjelpetekst("Type brev må angis slik at bruker får riktig varseltekst om brevet som sendes. Gjelder det et vedtak eller en forespørsel, vil bruker få en påminnelse hvis brevet ikke har blitt lest innen 7 dager.")
        .medValg(hentDistribusjonstyper())
        .erPåkrevd()
        .build();
    static final BrevmalFeltDto FELT_DOKUMENT_TITTEL = new BrevmalFeltDto.Builder()
        .medKodeOgBeskrivelse(BrevmalFeltKode.DOKUMENT_TITTEL)
        .medFeltType(FeltType.TEKST)
        .medHjelpetekst("Tittelen du skriver inn her vil bli journalføringstittel.")
        .medTegnBegrensning(60)
        .build();
    static final BrevmalFeltDto FELT_FRITEKST = new BrevmalFeltDto.Builder()
        .medKodeOgBeskrivelse(BrevmalFeltKode.FRITEKST)
        .medHjelpetekst("Teksten du skriver inn her vil være hovedteksten i brevet du lager.")
        .medFeltType(FeltType.FRITEKST)
        .erPåkrevd()
        .build();
    static final BrevmalFeltDto FELT_VEDLEGG = new BrevmalFeltDto.Builder()
        .medKodeOgBeskrivelse(BrevmalFeltKode.VEDLEGG)
        .medFeltType(FeltType.VEDLEGG)
        .build();
    static final BrevmalFeltDto FELT_FRITEKSTVEDLEGG = new BrevmalFeltDto.Builder()
        .medKodeOgBeskrivelse(BrevmalFeltKode.FRITEKSTVEDLEGG)
        .medFeltType(FeltType.FRITEKSTVEDLEGG)
        .build();

    static BrevmalFeltDto lagUtenlandskTrygdemyndighetMottakerFelt(FeltValgDto valg) {
        return new BrevmalFeltDto.Builder()
            .medKodeOgBeskrivelse(BrevmalFeltKode.UTENLANDSK_TRYGDEMYNDIGHET_MOTTAKER)
            .medValg(valg)
            .erPåkrevd()
            .build();
    }

    static BrevmalFeltDto lagBrevTittelFelt(FeltValgDto valg) {
        return new BrevmalFeltDto.Builder()
            .medKodeOgBeskrivelse(BrevmalFeltKode.BREV_TITTEL)
            .medFeltType(FeltType.TEKST)
            .medValg(valg)
            .medTegnBegrensning(60)
            .erPåkrevd()
            .build();
    }

    private static FeltValgDto hentDistribusjonstyper() {
        List<FeltvalgAlternativDto> distribusjonstyper = List.of(
            new FeltvalgAlternativDto(Distribusjonstype.VEDTAK.getKode(), Distribusjonstype.VEDTAK.getBeskrivelse(), false),
            new FeltvalgAlternativDto(Distribusjonstype.VIKTIG.getKode(), Distribusjonstype.VIKTIG.getBeskrivelse(), false),
            new FeltvalgAlternativDto(Distribusjonstype.ANNET.getKode(), Distribusjonstype.ANNET.getBeskrivelse(), false)
        );
        return new FeltValgDto(distribusjonstyper, FeltValgType.RADIO);
    }
}
