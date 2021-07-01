package no.nav.melosys.service.persondata;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.Set;
import java.util.stream.Collectors;

import no.finn.unleash.Unleash;
import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.person.Informasjonsbehov;
import no.nav.melosys.domain.person.Persondata;
import no.nav.melosys.domain.person.Statsborgerskap;
import no.nav.melosys.exception.*;
import no.nav.melosys.integrasjon.pdl.PDLConsumer;
import no.nav.melosys.integrasjon.pdl.dto.identer.Ident;
import no.nav.melosys.integrasjon.pdl.dto.person.Adressebeskyttelse;
import no.nav.melosys.integrasjon.tps.TpsService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.kodeverk.KodeverkService;
import no.nav.melosys.service.persondata.mapping.NavnOversetter;
import no.nav.melosys.service.persondata.mapping.PersondataOversetter;
import no.nav.melosys.service.persondata.mapping.StasborgerskapOversetter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Service
@Primary
public class PersondataService implements PersondataFasade {
    private final BehandlingService behandlingService;
    private final KodeverkService kodeverkService;
    private final PDLConsumer pdlConsumer;
    private final TpsService tpsService;
    private final Unleash unleash;

    @Autowired
    public PersondataService(BehandlingService behandlingService,
                             KodeverkService kodeverkService,
                             @Qualifier("saksbehandler") PDLConsumer pdlConsumer,
                             TpsService tpsService,
                             Unleash unleash) {
        this.behandlingService = behandlingService;
        this.kodeverkService = kodeverkService;
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
    public Saksopplysning hentPersonFraTps(String fnr, Informasjonsbehov behov) {
        return tpsService.hentPerson(fnr, behov);
    }

    @Override
    public Persondata hentPerson(String ident) {
        return PersondataOversetter.oversett(pdlConsumer.hentPerson(ident), kodeverkService);
    }

    public PersonMedHistorikk hentPersonMedHistorikk(long behandlingID) {
        final String ident = behandlingService.hentBehandlingUtenSaksopplysninger(behandlingID)
            .getFagsak().hentBruker().getAktørId();
        return null;
    }

    @Override
    public Saksopplysning hentPersonhistorikk(String fnr, LocalDate dato) {
        return tpsService.hentPersonhistorikk(fnr, dato);
    }

    @Override
    public String hentSammensattNavn(String fnr) {
        if (unleash.isEnabled("melosys.pdl.sammensatt-navn")) {
            return pdlConsumer.hentNavn(fnr).stream()
                .max(Comparator.comparing(n -> n.metadata().datoSistRegistrert()))
                .map(NavnOversetter::tilSammensattNavn)
                .orElse(NavnOversetter.UKJENT);
        }
        return tpsService.hentSammensattNavn(fnr);
    }

    @Override
    public Set<Statsborgerskap> hentStatsborgerskap(String ident) {
        return pdlConsumer.hentStatsborgerskap(ident).stream()
            .filter(s -> !s.erOpphørt())
            .map(StasborgerskapOversetter::oversett)
            .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public boolean harStrengtFortroligAdresse(String fnr) {
        if (unleash.isEnabled("melosys.pdl.adressebeskyttelse")) {
            return pdlConsumer.hentAdressebeskyttelser(fnr).stream().anyMatch(Adressebeskyttelse::erStrengtFortrolig);
        }
        return tpsService.harStrengtFortroligAdresse(fnr);
    }
}
