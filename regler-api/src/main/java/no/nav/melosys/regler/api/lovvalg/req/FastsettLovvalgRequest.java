package no.nav.melosys.regler.api.lovvalg.req;

import java.util.List;

import no.nav.melosys.domain.dokument.arbeidsforhold.ArbeidsforholdDokument;
import no.nav.melosys.domain.dokument.inntekt.InntektDokument;
import no.nav.melosys.domain.dokument.medlemskap.MedlemskapDokument;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;

/**
 * Forespørsler til lovvalgtjenesten
 */
public class FastsettLovvalgRequest {
    
    public SoeknadDokument søknadDokument;
    public PersonDokument personopplysningDokument;
    public List<ArbeidsforholdDokument> arbeidsforholdDokumenter;
    public List<InntektDokument> inntektDokumenter;
    public List<MedlemskapDokument> medlemskapDokumenter;
    public List<OrganisasjonDokument> organisasjonDokumenter;
    // public List arbeidUtland; // FIXME
    
}
