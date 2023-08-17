package no.nav.melosys.tjenester.gui.dto.brev;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import no.nav.melosys.domain.arkiv.Distribusjonstype;
import no.nav.melosys.domain.brev.utkast.BrevbestillingUtkast;
import no.nav.melosys.domain.kodeverk.Mottakerroller;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.service.dokument.brev.BrevbestillingDto;
import no.nav.melosys.service.dokument.brev.FritekstvedleggDto;
import no.nav.melosys.service.dokument.brev.KopiMottakerDto;
import no.nav.melosys.service.dokument.brev.SaksvedleggDto;
import no.nav.melosys.sikkerhet.context.SubjectHandler;

import static java.util.Optional.ofNullable;

public record BrevbestillingRequest(
    Produserbaredokumenter produserbardokument,
    Mottakerroller mottaker,
    String orgNr,
    String institusjonId,
    List<String> orgnrNorskMyndighet,
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
    String saksbehandlerNrToIdent,
    String begrunnelseKode,
    String ytterligereInformasjon) {

    public BrevbestillingDto tilBrevbestillingDto(String bestillersId) {
        return new BrevbestillingDto(
            this.produserbardokument,
            this.mottaker,
            this.orgNr,
            this.orgnrNorskMyndighet,
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
            this.saksbehandlerNrToIdent,
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
            this.orgnrNorskMyndighet(),
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
            this.dokumentTittel(),
            this.saksbehandlerNrToIdent()
        );
    }

    public static BrevbestillingRequest av(BrevbestillingUtkast utkast) {
        return new BrevbestillingRequest(
            utkast.produserbardokument(),
            utkast.mottaker(),
            utkast.orgnr(),
            utkast.institusjonID(),
            utkast.orgnrNorskMyndighet(),
            utkast.innledningFritekst(),
            utkast.manglerFritekst(),
            utkast.begrunnelseFritekst(),
            utkast.ektefelleFritekst(),
            utkast.barnFritekst(),
            utkast.kontaktpersonNavn(),
            utkast.kopiMottakere().stream().map(KopiMottakerDto::av).toList(),
            utkast.fritekstTittel(),
            utkast.fritekst(),
            utkast.distribusjonstype(),
            utkast.kontaktopplysninger(),
            utkast.nyVurderingBakgrunn(),
            utkast.saksVedlegg().stream().map(SaksvedleggDto::av).toList(),
            utkast.fritekstVedlegg().stream().map(FritekstvedleggDto::av).toList(),
            utkast.dokumentTittel(),
            utkast.saksbehandlerNrToIdent(),
            null,
            null
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
