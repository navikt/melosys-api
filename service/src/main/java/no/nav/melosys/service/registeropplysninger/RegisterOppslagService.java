package no.nav.melosys.service.registeropplysninger;

import java.util.HashSet;
import java.util.Set;

import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.integrasjon.ereg.EregFasade;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Primary
@Service
public class RegisterOppslagService {
    private final EregFasade eregFasade;
    private final TpsFasade tpsFasade;

    @Autowired
    public  RegisterOppslagService(EregFasade eregFasade, TpsFasade tpsFasade) {
        this.eregFasade = eregFasade;
        this.tpsFasade = tpsFasade;
    }

    /**
     *  Henter et sett med organisasjonsopplysninger
     */
    public Set<OrganisasjonDokument> hentOrganisasjoner(Set<String> orgnumre) throws IkkeFunnetException, IntegrasjonException {
        Set<OrganisasjonDokument> organisasjoner = new HashSet<>();
        for (String orgnr : orgnumre) {
            OrganisasjonDokument saksopplysning = hentOrganisasjon(orgnr);
            if (saksopplysning != null) {
                organisasjoner.add(saksopplysning);
            }
        }
        return organisasjoner;
    }

    /**
     * Henter organisasjonsopplysninger.
     */
    public OrganisasjonDokument hentOrganisasjon(String orgnummer) throws IkkeFunnetException, IntegrasjonException {
        Saksopplysning saksopplysning = eregFasade.hentOrganisasjon(orgnummer);
        return (OrganisasjonDokument) saksopplysning.getDokument();
    }

    /**
     * Henter personopplysninger.
     */
    public PersonDokument hentPerson(String personnummer) throws IkkeFunnetException, SikkerhetsbegrensningException, IntegrasjonException {
        Saksopplysning saksopplysning = tpsFasade.hentPersonMedAdresse(personnummer);
        return (PersonDokument) saksopplysning.getDokument();
    }
}
