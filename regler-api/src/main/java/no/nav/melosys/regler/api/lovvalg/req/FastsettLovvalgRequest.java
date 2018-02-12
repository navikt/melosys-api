package no.nav.melosys.regler.api.lovvalg.req;

import java.util.List;
import javax.xml.bind.annotation.*;

import no.nav.melosys.domain.dokument.arbeidsforhold.ArbeidsforholdDokument;
import no.nav.melosys.domain.dokument.inntekt.InntektDokument;
import no.nav.melosys.domain.dokument.medlemskap.MedlemskapDokument;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;

/**
 * Forespørsler til lovvalgtjenesten
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class FastsettLovvalgRequest {
    
    public SoeknadDokument søknadDokument;
    public PersonDokument personopplysningDokument;
    @XmlElementWrapper(name="arbeidsforholdDokumenter")
    @XmlElement(name="arbeidsforholdDokument")
    public List<ArbeidsforholdDokument> arbeidsforholdDokumenter;
    @XmlElementWrapper(name="inntektDokumenter")
    @XmlElement(name="inntektDokument")
    public List<InntektDokument> inntektDokumenter;
    @XmlElementWrapper(name="medlemskapDokumenter")
    @XmlElement(name="medlemskapDokument")
    public List<MedlemskapDokument> medlemskapDokumenter;
    @XmlElementWrapper(name="organisasjonDokumenter")
    @XmlElement(name="organisasjonDokument")
    public List<OrganisasjonDokument> organisasjonDokumenter;
    // public List arbeidUtland; // FIXME
    
}
