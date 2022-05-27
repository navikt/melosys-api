package no.nav.melosys.service.persondata;

import java.util.Comparator;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.person.Informasjonsbehov;
import no.nav.melosys.domain.person.PersonMedHistorikk;
import no.nav.melosys.domain.person.Persondata;
import no.nav.melosys.domain.person.Statsborgerskap;
import no.nav.melosys.domain.person.familie.Familiemedlem;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.integrasjon.pdl.PDLConsumer;
import no.nav.melosys.integrasjon.pdl.dto.HarMetadata;
import no.nav.melosys.integrasjon.pdl.dto.identer.Ident;
import no.nav.melosys.integrasjon.pdl.dto.person.Adressebeskyttelse;
import no.nav.melosys.integrasjon.pdl.dto.person.Person;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.kodeverk.KodeverkService;
import no.nav.melosys.service.persondata.familie.FamiliemedlemService;
import no.nav.melosys.service.persondata.mapping.NavnOversetter;
import no.nav.melosys.service.persondata.mapping.PersonMedHistorikkOversetter;
import no.nav.melosys.service.persondata.mapping.PersonopplysningerOversetter;
import no.nav.melosys.service.persondata.mapping.StatsborgerskapOversetter;
import no.nav.melosys.service.saksopplysninger.SaksopplysningerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Service
@Primary
public class PersondataService implements PersondataFasade {

    private static final Logger log = LoggerFactory.getLogger(PersondataService.class);
    private final BehandlingService behandlingService;
    private final KodeverkService kodeverkService;
    private final PDLConsumer pdlConsumer;
    private final SaksopplysningerService saksopplysningerService;
    private final FamiliemedlemService familiemedlemService;

    public static final String PDL_PERSOPL_VERSJON = "1.0";
    public static final String PDL_PERS_SAKS_VERSJON = "1.0";

    public PersondataService(BehandlingService behandlingService,
                             KodeverkService kodeverkService,
                             @Qualifier("saksbehandler") PDLConsumer pdlConsumer,
                             SaksopplysningerService saksopplysningerService,
                             FamiliemedlemService familiemedlemService) {
        this.behandlingService = behandlingService;
        this.kodeverkService = kodeverkService;
        this.pdlConsumer = pdlConsumer;
        this.saksopplysningerService = saksopplysningerService;
        this.familiemedlemService = familiemedlemService;
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
    public Optional<String> finnFolkeregisterident(String ident) {
        return pdlConsumer.hentIdenter(ident).identer()
            .stream().filter(Ident::erFolkeregisterIdent)
            .findFirst().map(Ident::ident);
    }

    @Override
    @Cacheable("folkeregisterIdent")
    public String hentFolkeregisterident(String ident) {
        return finnFolkeregisterident(ident)
            .orElseThrow(() -> new IkkeFunnetException("Finner ikke folkeregisterident!"));
    }

    @Override
    public Persondata hentPerson(String ident) {
        return hentPerson(ident, Informasjonsbehov.STANDARD);
    }

    @Override
    public Persondata hentPerson(String ident, Informasjonsbehov informasjonsbehov) {
        return switch (informasjonsbehov) {
            case INGEN, STANDARD -> PersonopplysningerOversetter.oversett(pdlConsumer.hentPerson(ident),
                kodeverkService);
            case MED_FAMILIERELASJONER -> lagPersondataMedFamilie(ident);
        };
    }

    private Persondata lagPersondataMedFamilie(String ident) {
        final Person person = pdlConsumer.hentPerson(ident);
        return PersonopplysningerOversetter.oversettMedFamilie(person,
            familiemedlemService.hentFamiliemedlemmer(person),
            kodeverkService);
    }

    @Override
    public PersonMedHistorikk hentPersonMedHistorikk(long behandlingID) {
        final Behandling behandling = behandlingService.hentBehandling(behandlingID);
        final String ident = behandling.getFagsak().hentBrukersAktørID();
        if (behandling.erAktiv()) {
            return hentPersonMedHistorikk(ident);
        }

        return saksopplysningerService.finnPdlPersonhistorikkTilSaksbehandler(behandlingID)
            .orElseGet(() -> PersonMedHistorikkOversetter.lagHistorikkFraTpsData(saksopplysningerService.hentTpsPersonopplysninger(behandlingID), kodeverkService));
    }

    @Override
    public PersonMedHistorikk hentPersonMedHistorikk(String ident) {
        log.debug("Henter person med historikk ved bruk av ident");
        return PersonMedHistorikkOversetter.oversett(pdlConsumer.hentPersonMedHistorikk(ident), kodeverkService);
    }

    @Override
    public Set<Familiemedlem> hentFamiliemedlemmerFraBehandlingID(long behandlingID) {
        return familiemedlemService.hentFamiliemedlemmerFraBehandlingID(behandlingID);
    }

    @Override
    public Set<Familiemedlem> hentFamiliemedlemmerFraIdent(String ident) {
        return familiemedlemService.hentFamiliemedlemmerFraIdent(ident);
    }

    @Override
    public String hentSammensattNavn(String ident) {
        return pdlConsumer.hentNavn(ident).stream()
            .max(Comparator.comparing(n -> n.metadata().datoSistRegistrert()))
            .map(NavnOversetter::tilSammensattNavn)
            .orElse(NavnOversetter.UKJENT);
    }

    @Override
    public Set<Statsborgerskap> hentStatsborgerskap(String ident) {
        return pdlConsumer.hentStatsborgerskap(ident).stream()
            .filter(HarMetadata::erGyldig)
            .map(StatsborgerskapOversetter::oversett)
            .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public boolean harStrengtFortroligAdresse(String ident) {
        return pdlConsumer.hentAdressebeskyttelser(ident).stream().anyMatch(Adressebeskyttelse::erStrengtFortrolig);
    }
}
