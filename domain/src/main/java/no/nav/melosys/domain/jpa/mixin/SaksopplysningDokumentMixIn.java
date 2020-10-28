package no.nav.melosys.domain.jpa.mixin;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import no.nav.melosys.domain.dokument.arbeidsforhold.ArbeidsforholdDokument;
import no.nav.melosys.domain.dokument.inntekt.InntektDokument;
import no.nav.melosys.domain.dokument.medlemskap.MedlemskapDokument;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.dokument.person.PersonhistorikkDokument;
import no.nav.melosys.domain.dokument.sakogbehandling.SobSakDokument;
import no.nav.melosys.domain.dokument.sed.SedDokument;
import no.nav.melosys.domain.dokument.utbetaling.UtbetalingDokument;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes( {
    @JsonSubTypes.Type(value = ArbeidsforholdDokument.class),
    @JsonSubTypes.Type(value = InntektDokument.class),
    @JsonSubTypes.Type(value = MedlemskapDokument.class),
    @JsonSubTypes.Type(value = OrganisasjonDokument.class),
    @JsonSubTypes.Type(value = PersonDokument.class),
    @JsonSubTypes.Type(value = PersonhistorikkDokument.class),
    @JsonSubTypes.Type(value = SedDokument.class),
    @JsonSubTypes.Type(value = SobSakDokument.class),
    @JsonSubTypes.Type(value = UtbetalingDokument.class)
})
public interface SaksopplysningDokumentMixIn {}
