package no.nav.melosys.tjenester.gui.dto.brev;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import no.nav.melosys.domain.arkiv.Distribusjonstype;
import no.nav.melosys.domain.brev.utkast.BrevbestillingUtkast;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.service.dokument.brev.BrevbestillingDto;
import no.nav.melosys.service.dokument.brev.FritekstvedleggDto;
import no.nav.melosys.service.dokument.brev.KopiMottakerDto;
import no.nav.melosys.service.dokument.brev.SaksvedleggDto;
import no.nav.melosys.sikkerhet.context.SubjectHandler;

import static java.util.Optional.ofNullable;

public record BrevbestillingRequest(
    Produserbaredokumenter produserbardokument,
    Aktoersroller mottaker,
    String orgNr,
    String institusjonId,
    List<String> orgnrEtater,
    String innledningFritekst,
    String manglerFritekst,
    String begrunnelseFritekst,
    String ektefelleFritekst,
    String barnFritekst,
    String kontaktpersonNavn,
    List<KopiMottakerDto> kopiMottakere,
    String fritekstTittel,
    String fritekst,
    Distribusjonstype distribusjonstype,
    boolean kontaktopplysninger,
    String nyVurderingBakgrunn,
    List<SaksvedleggDto> saksvedlegg,
    List<FritekstvedleggDto> fritekstvedlegg,
    String dokumentTittel,
    @Deprecated(since = "Benyttes i doksys, kommer til å bli erstattet av dokgen-variabel")
    String begrunnelseKode,
    @Deprecated(since = "Benyttes i doksys, kommer til å bli erstattet av dokgen-variabel")
    String ytterligereInformasjon) {

    public BrevbestillingDto tilBrevbestillingDto(String bestillersId) {
        return new BrevbestillingDto(
            this.produserbardokument,
            this.mottaker,
            this.orgNr,
            this.orgnrEtater,
            this.institusjonId,
            this.innledningFritekst,
            this.manglerFritekst,
            this.begrunnelseFritekst,
            this.ektefelleFritekst,
            this.barnFritekst,
            this.kontaktpersonNavn,
            this.kopiMottakere,
            bestillersId,
            this.fritekstTittel,
            this.fritekst,
            this.distribusjonstype,
            this.kontaktopplysninger,
            this.nyVurderingBakgrunn,
            this.saksvedlegg,
            this.fritekstvedlegg,
            this.dokumentTittel,
            this.begrunnelseKode,
            this.ytterligereInformasjon
        );
    }

    public BrevbestillingDto tilBrevbestillingDto() {
        return tilBrevbestillingDto(SubjectHandler.getInstance().getUserID());
    }

    public BrevbestillingUtkast tilUtkast() {
        return new BrevbestillingUtkast(
            this.produserbardokument(),
            this.mottaker(),
            this.orgNr(),
            this.orgnrEtater(),
            this.institusjonId(),
            this.innledningFritekst(),
            this.manglerFritekst(),
            this.begrunnelseFritekst(),
            this.ektefelleFritekst(),
            this.barnFritekst(),
            this.kontaktpersonNavn(),
            konverterListeTil(this.kopiMottakere(), KopiMottakerDto::tilUtkast),
            this.fritekstTittel(),
            this.fritekst(),
            this.distribusjonstype(),
            this.kontaktopplysninger(),
            this.nyVurderingBakgrunn(),
            konverterListeTil(this.saksvedlegg(), SaksvedleggDto::tilUtkast),
            konverterListeTil(this.fritekstvedlegg(), FritekstvedleggDto::tilUtkast),
            this.dokumentTittel()
        );
    }

    private <T, R> List<R> konverterListeTil(List<T> liste, Function<T, R> mapper) {
        return ofNullable(liste)
            .orElseGet(Collections::emptyList)
            .stream()
            .map(mapper)
            .toList();
    }
}
