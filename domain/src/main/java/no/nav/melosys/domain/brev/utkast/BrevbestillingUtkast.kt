package no.nav.melosys.domain.brev.utkast;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import no.nav.melosys.domain.arkiv.Distribusjonstype;
import no.nav.melosys.domain.brev.StandardvedleggType;
import no.nav.melosys.domain.kodeverk.Mottakerroller;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonSerialize
public record BrevbestillingUtkast(
    Produserbaredokumenter produserbardokument,
    Mottakerroller mottaker,
    String orgnr,
    List<String> orgnrNorskMyndighet,
    String institusjonID,
    String innledningFritekst,
    String manglerFritekst,
    String begrunnelseFritekst,
    String ektefelleFritekst,
    String barnFritekst,
    String trygdeavgiftFritekst,
    String kontaktpersonNavn,
    List<KopiMottakerUtkast> kopiMottakere,
    String fritekstTittel,
    String fritekst,
    Distribusjonstype distribusjonstype,
    boolean kontaktopplysninger,
    String nyVurderingBakgrunn,
    List<Utkast.Saksvedlegg> saksVedlegg,
    StandardvedleggType standardvedleggType,
    List<Utkast.FritekstVedlegg> fritekstVedlegg,
    String dokumentTittel,
    String saksbehandlerNrToIdent,
    boolean skalViseStandardTekstOmOpplysninger
) {
    @JsonIgnore
    public String getTittel() {
        if (dokumentTittel() == null || dokumentTittel().isEmpty()) {
            return produserbardokument().getBeskrivelse();
        }
        return dokumentTittel();
    }
}
