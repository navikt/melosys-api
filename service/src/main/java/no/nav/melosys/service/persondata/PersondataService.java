package no.nav.melosys.service.persondata;

import java.time.LocalDate;

import no.finn.unleash.Unleash;
import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.person.Informasjonsbehov;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.integrasjon.pdl.PDLConsumer;
import no.nav.melosys.integrasjon.pdl.dto.identer.Ident;
import no.nav.melosys.integrasjon.tps.TpsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Service
@Primary
public class PersondataService implements PersondataFasade {
    private final PDLConsumer pdlConsumer;
    private final TpsService tpsService;
    private final Unleash unleash;

    @Autowired
    public PersondataService(@Qualifier("saksbehandler") PDLConsumer pdlConsumer,
                             TpsService tpsService,
                             Unleash unleash) {
        this.pdlConsumer = pdlConsumer;
        this.tpsService = tpsService;
        this.unleash = unleash;
    }

    @Override
    @Cacheable("aktoerID")
    public String hentAktørIdForIdent(String ident) {
        return pdlConsumer.hentIdenter(ident).identer()
            .stream().filter(Ident::erAktørID)
            .findFirst().map(Ident::ident)
            .orElseThrow(() -> new IkkeFunnetException("Finner ikke aktørID!"));
    }

    @Override
    @Cacheable("folkeregisterIdent")
    public String hentFolkeregisterIdent(String ident) {
        return pdlConsumer.hentIdenter(ident).identer()
            .stream().filter(Ident::erFolkeregisterIdent)
            .findFirst().map(Ident::ident)
            .orElseThrow(() -> new IkkeFunnetException("Finner ikke folkeregisterident!"));
    }

    @Override
    public Saksopplysning hentPerson(String ident, Informasjonsbehov behov) {
        return tpsService.hentPerson(ident, behov);
    }

    @Override
    public Saksopplysning hentPersonhistorikk(String ident, LocalDate dato) {
        return tpsService.hentPersonhistorikk(ident, dato);
    }

    @Override
    public String hentSammensattNavn(String fnr) {
        return tpsService.hentSammensattNavn(fnr);
    }

    @Override
    public boolean harStrengtFortroligAdresse(String fnr) {
        return tpsService.harStrengtFortroligAdresse(fnr);
    }
}
