package no.nav.melosys.domain.brev.utkast;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import no.nav.melosys.domain.arkiv.Distribusjonstype;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonSerialize
public record BrevbestillingUtkast(
    Produserbaredokumenter produserbardokument,

    Aktoersroller mottaker,

    String orgnr,

    @JsonProperty("orgnr_etater")
    List<String> orgnrEtater,

    @JsonProperty("innledning_fritekst")
    String innledningFritekst,

    @JsonProperty("mangler_fritekst")
    String manglerFritekst,

    @JsonProperty("begrunnelse_fritekst")
    String begrunnelseFritekst,

    @JsonProperty("ektefelle_fritekst")
    String ektefelleFritekst,

    @JsonProperty("barn_fritekst")
    String barnFritekst,

    @JsonProperty("kontaktperson_navn")
    String kontaktpersonNavn,

    @JsonProperty("kopi_mottakere")
    List<KopiMottakerUtkast> kopiMottakere,

    @JsonProperty("fritekst_tittel")
    String fritekstTittel,

    String fritekst,

    Distribusjonstype distribusjonstype,

    boolean kontaktopplysninger,

    @JsonProperty("ny_vurdering_bakgrunn")
    String nyVurderingBakgrunn,

    List<VedleggUtkast.Saksvedlegg> saksvedlegg,

    @JsonProperty("fritekst_vedlegg")
    List<VedleggUtkast.FritekstVedlegg> fritekstVedlegg,

    @JsonProperty("dokument_tittel")
    String dokumentTittel
) {
}
