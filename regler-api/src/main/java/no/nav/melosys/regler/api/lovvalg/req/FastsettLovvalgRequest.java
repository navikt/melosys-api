package no.nav.melosys.regler.api.lovvalg.req;

import java.util.List;

import no.nav.melosys.domain.dokument.arbeidsforhold.ArbeidsforholdDokument;
import no.nav.melosys.domain.dokument.inntekt.InntektDokument;
import no.nav.melosys.domain.dokument.medlemskap.MedlemskapDokument;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.dokument.person.PersonopplysningDokument;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;

/**
 * Forespørsler til lovvalgtjenesten
 * 
 * FIXME: Trenger revisjon
 */
public class FastsettLovvalgRequest {
    
    public SoeknadDokument søknadDokument;
    public PersonopplysningDokument personopplysningDokument;
    public List<ArbeidsforholdDokument> arbeidsforholdDokumenter;
    public List<InntektDokument> inntektDokumenter;
    public List<MedlemskapDokument> medlemskapDokumenter;
    public List<OrganisasjonDokument> organisasjonDokumenter;
    
}
