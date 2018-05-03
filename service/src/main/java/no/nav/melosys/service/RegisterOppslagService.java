package no.nav.melosys.service;

import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.integrasjon.ereg.EregFasade;
import no.nav.melosys.integrasjon.felles.exception.IkkeFunnetException;
import no.nav.melosys.integrasjon.felles.exception.SikkerhetsbegrensningException;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RegisterOppslagService {

    private EregFasade eregFasade;

    private TpsFasade tpsFasade;

    @Autowired
    public  RegisterOppslagService(EregFasade eregFasade, TpsFasade tpsFasade) {
        this.eregFasade = eregFasade;
        this.tpsFasade = tpsFasade;
    }

    public OrganisasjonDokument hentOrganisasjon(String orgnummer) throws IkkeFunnetException, SikkerhetsbegrensningException {
        Saksopplysning saksopplysning = eregFasade.hentOrganisasjon(orgnummer);
        OrganisasjonDokument organisasjon = (OrganisasjonDokument) saksopplysning.getDokument();
        return organisasjon;
    }

    public PersonDokument hentPerson(String personnummer) throws IkkeFunnetException, SikkerhetsbegrensningException {
        Saksopplysning saksopplysning = tpsFasade.hentPerson(personnummer);
        PersonDokument personDokument = (PersonDokument) saksopplysning.getDokument();
        return personDokument;
    }
}
