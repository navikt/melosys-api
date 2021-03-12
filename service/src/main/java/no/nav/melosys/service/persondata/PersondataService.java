package no.nav.melosys.service.persondata;

import java.time.LocalDate;

import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.person.Informasjonsbehov;
import no.nav.melosys.exception.*;
import no.nav.melosys.integrasjon.tps.TpsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Service
@Primary
public class PersondataService implements PersondataFasade {
    private final TpsService tpsService;

    @Autowired
    public PersondataService(TpsService tpsService) {
        this.tpsService = tpsService;
    }

    @Override
    public String hentAktørIdForIdent(String fnr) throws IkkeFunnetException {
        return tpsService.hentAktørIdForIdent(fnr);
    }

    @Override
    public String hentIdentForAktørId(String aktørID) throws IkkeFunnetException {
        return tpsService.hentIdentForAktørId(aktørID);
    }

    @Override
    public Saksopplysning hentPerson(String ident, Informasjonsbehov behov) throws IkkeFunnetException,
        IntegrasjonException, SikkerhetsbegrensningException {
        return tpsService.hentPerson(ident, behov);
    }

    @Override
    public Saksopplysning hentPersonhistorikk(String ident, LocalDate dato) throws IkkeFunnetException,
        SikkerhetsbegrensningException, TekniskException {
        return tpsService.hentPersonhistorikk(ident, dato);
    }

    @Override
    public String hentSammensattNavn(String fnr) throws FunksjonellException, IntegrasjonException {
        return tpsService.hentSammensattNavn(fnr);
    }

    @Override
    public boolean harStrengtFortroligAdresse(String fnr) throws IkkeFunnetException, IntegrasjonException,
        SikkerhetsbegrensningException {
        return tpsService.harStrengtFortroligAdresse(fnr);
    }
}
