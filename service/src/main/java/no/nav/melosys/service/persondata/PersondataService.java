package no.nav.melosys.service.persondata;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

import no.finn.unleash.Unleash;
import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.person.Informasjonsbehov;
import no.nav.melosys.domain.person.Persondata;
import no.nav.melosys.domain.person.Statsborgerskap;
import no.nav.melosys.domain.person.familie.Familiemedlem;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.integrasjon.pdl.PDLConsumer;
import no.nav.melosys.integrasjon.pdl.dto.identer.Ident;
import no.nav.melosys.integrasjon.pdl.dto.person.Adressebeskyttelse;
import no.nav.melosys.integrasjon.pdl.dto.person.ForelderBarnRelasjon;
import no.nav.melosys.integrasjon.pdl.dto.person.Person;
import no.nav.melosys.integrasjon.pdl.dto.person.Sivilstand;
import no.nav.melosys.integrasjon.tps.TpsService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.kodeverk.KodeverkService;
import no.nav.melosys.service.persondata.mapping.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import static java.time.temporal.ChronoUnit.YEARS;

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
    public String hentFolkeregisterident(String ident) {
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
        final var person = pdlConsumer.hentPerson(ident);
        return PersonopplysningerOversetter.oversettMedFamilie(person, hentFamiliemedlemmer(person), kodeverkService);
    }

    private Set<Familiemedlem> hentFamiliemedlemmer(Person person) {
        final Set<Familiemedlem> familiemedlemmer = new HashSet<>();
        if (erPersonUnder18(person)) {
            familiemedlemmer.addAll(hentForeldre(person.forelderBarnRelasjon()));
        }
        familiemedlemmer.addAll(hentRelatertVedSivilstand(person.sivilstand()));
        familiemedlemmer.addAll(hentBarn(person));
        return familiemedlemmer;
    }

    private boolean erPersonUnder18(Person person) {
        return YEARS.between(FoedselOversetter.oversett(person.foedsel()).fødselsdato(), LocalDate.now()) < 18;
    }

    private Set<Familiemedlem> hentForeldre(Collection<ForelderBarnRelasjon> forelderBarnRelasjoner) {
        Set<Familiemedlem> set = new HashSet<>();
        for (ForelderBarnRelasjon forelderBarnRelasjon : forelderBarnRelasjoner) {
            if (forelderBarnRelasjon.erForelder()) {
                Person person = pdlConsumer.hentForelder(forelderBarnRelasjon.relatertPersonsIdent());
                Familiemedlem forelder = FamiliemedlemOversetter.oversettForelder(person,
                    forelderBarnRelasjon.relatertPersonsRolle());
                set.add(forelder);
            }
        }
        return Collections.unmodifiableSet(set);
    }

    private Set<Familiemedlem> hentRelatertVedSivilstand(Collection<Sivilstand> sivilstandRelasjoner) {
        return sivilstandRelasjoner.stream()
            .map(Sivilstand::relatertVedSivilstand)
            .filter(Objects::nonNull)
            .map(pdlConsumer::hentRelatertVedSivilstand)
            .map(FamiliemedlemOversetter::oversettRelatertVedSivilstand)
            .collect(Collectors.toUnmodifiableSet());
    }

    private Set<Familiemedlem> hentBarn(Person person) {
        final var folkeregisteridentifikator = FolkeregisteridentOversetter.oversett(
            person.folkeregisteridentifikator());
        return person.forelderBarnRelasjon().stream()
            .filter(ForelderBarnRelasjon::erBarn)
            .map(ForelderBarnRelasjon::relatertPersonsIdent)
            .map(pdlConsumer::hentBarn)
            .map(barn -> FamiliemedlemOversetter.oversettBarn(barn, folkeregisteridentifikator))
            .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public PersonMedHistorikk hentPersonMedHistorikk(long behandlingID) {
        final var behandling = behandlingService.hentBehandlingUtenSaksopplysninger(behandlingID);
        final String ident = behandling.getFagsak().hentAktørID();
        if (behandling.erInaktiv()) {
            /*TODO MELOSYS-4466
               - Mapping fra TPS dato for gamle behandlinger opprettet før PDL
               - Det ville være mest riktig å se på vedtakstidspunktet om det finnes et vedtak (default behandling.getEndretDato()
               fordi behandling kan endres automatisk etter vedtak (art. 13)
             */
            return PersonMedHistorikkOversetter.oversett(gjennskapPersonPåDatoTilInnsyn(pdlConsumer.hentPersonMedHistorikk(ident, true), behandling.getEndretDato()), kodeverkService);
        } else {
            return PersonMedHistorikkOversetter.oversett(pdlConsumer.hentPersonMedHistorikk(ident, false), kodeverkService);
        }
    }

    public static Person gjennskapPersonPåDatoTilInnsyn(Person person, Instant instant) {
        LocalDateTime localDateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
        return new Person(
            Collections.emptyList(),
            person.bostedsadresse().stream().filter(x -> x.erGyldigFør(localDateTime)).toList(),
            person.doedsfall().stream().filter(x -> x.erGyldigFør(localDateTime)).toList(),
            person.foedsel().stream().filter(x -> x.erGyldigFør(localDateTime)).toList(),
            person.folkeregisteridentifikator().stream().filter(x -> x.erGyldigFør(localDateTime)).toList(),
            person.folkeregisterpersonstatus().stream().toList(),
            Collections.emptyList(),
            Collections.emptyList(),
            person.kjoenn().stream().filter(x -> x.erGyldigFør(localDateTime)).toList(),
            person.kontaktadresse().stream().filter(x -> x.erGyldigFør(localDateTime)).toList(),
            person.navn().stream().filter(x -> x.erGyldigFør(localDateTime)).toList(),
            person.oppholdsadresse().stream().filter(x -> x.erGyldigFør(localDateTime)).toList(),
            person.sivilstand().stream().filter(x -> x.erGyldigFør(localDateTime)).toList(),
            person.statsborgerskap().stream().filter(x -> x.erGyldigFør(localDateTime)).toList(),
            Collections.emptyList()
        );
    }

    @Override
    public Set<Familiemedlem> hentFamiliemedlemmerMedHistorikk(long behandlingID) {
        final var behandling = behandlingService.hentBehandlingUtenSaksopplysninger(behandlingID);
        final String ident = behandling.getFagsak().hentAktørID();
        if (behandling.erInaktiv()) {
            //TODO MELOSYS-4466
            return Collections.emptySet();
        } else {
            return hentFamiliemedlemmer(pdlConsumer.hentFamilierelasjoner(ident));
        }
    }

    @Override
    public Saksopplysning hentPersonhistorikk(String fnr, LocalDate dato) {
        return tpsService.hentPersonhistorikk(fnr, dato);
    }

    @Override
    public String hentSammensattNavn(String ident) {
        if (unleash.isEnabled("melosys.pdl.sammensatt-navn")) {
            return pdlConsumer.hentNavn(ident).stream()
                .max(Comparator.comparing(n -> n.metadata().datoSistRegistrert()))
                .map(NavnOversetter::tilSammensattNavn)
                .orElse(NavnOversetter.UKJENT);
        }
        return tpsService.hentSammensattNavn(ident);
    }

    @Override
    public Set<Statsborgerskap> hentStatsborgerskap(String ident) {
        return pdlConsumer.hentStatsborgerskap(ident).stream()
            .filter(s -> !s.erOpphørt())
            .map(StatsborgerskapOversetter::oversett)
            .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public boolean harStrengtFortroligAdresse(String ident) {
        if (unleash.isEnabled("melosys.pdl.adressebeskyttelse")) {
            return pdlConsumer.hentAdressebeskyttelser(ident).stream().anyMatch(Adressebeskyttelse::erStrengtFortrolig);
        }
        return tpsService.harStrengtFortroligAdresse(ident);
    }
}
