package no.nav.melosys.domain.brev.utkast;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
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
    List<String> orgnrEtater,
    String innledningFritekst,
    String manglerFritekst,
    String begrunnelseFritekst,
    String ektefelleFritekst,
    String barnFritekst,
    String kontaktpersonNavn,
    List<KopiMottakerUtkast> kopiMottakere,
    String fritekstTittel,
    String fritekst,
    Distribusjonstype distribusjonstype,
    boolean kontaktopplysninger,
    String nyVurderingBakgrunn,
    List<VedleggUtkast.Saksvedlegg> saksVedlegg,
    List<VedleggUtkast.FritekstVedlegg> fritekstVedlegg,
    String dokumentTittel
) {
}
