package no.nav.melosys.service.registeropplysninger;

import java.util.HashSet;
import java.util.Set;

import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.person.Informasjonsbehov;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.integrasjon.ereg.EregFasade;
import no.nav.melosys.service.persondata.PersondataFasade;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Primary
@Service
public class RegisterOppslagService {
    private final EregFasade eregFasade;
    private final PersondataFasade persondataFasade;

    public RegisterOppslagService(EregFasade eregFasade, PersondataFasade persondataFasade) {
        this.eregFasade = eregFasade;
        this.persondataFasade = persondataFasade;
    }

    public Set<OrganisasjonDokument> hentOrganisasjoner(Set<String> orgnumre) {
        Set<OrganisasjonDokument> organisasjoner = new HashSet<>();
        for (String orgnr : orgnumre) {
            OrganisasjonDokument saksopplysning = hentOrganisasjon(orgnr);
            if (saksopplysning != null) {
                organisasjoner.add(saksopplysning);
            }
        }
        return organisasjoner;
    }

    public OrganisasjonDokument hentOrganisasjon(String orgnummer) {
        Saksopplysning saksopplysning = eregFasade.hentOrganisasjon(validerOgVaskOrgnr(orgnummer));
        return (OrganisasjonDokument) saksopplysning.getDokument();
    }

    /**
     * @deprecated /personer forsvinner ifm. overgang til PDL.
     */
    @Deprecated(forRemoval = true)
    public PersonDokument hentPerson(String personnummer) {
        Saksopplysning saksopplysning = persondataFasade.hentPersonFraTps(personnummer, Informasjonsbehov.STANDARD);
        return (PersonDokument) saksopplysning.getDokument();
    }
    
    private String validerOgVaskOrgnr(String orgnr) {
        orgnr = orgnr.replace(" ", "");

        if (orgnr.length() != 9) {
            throw new FunksjonellException("Ugyldig orgnr " + orgnr);
        }

        return orgnr;
    }
}
