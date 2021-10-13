package no.nav.melosys.service.persondata;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

import no.finn.unleash.Unleash;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.person.Informasjonsbehov;
import no.nav.melosys.domain.person.Persondata;
import no.nav.melosys.domain.person.Statsborgerskap;
import no.nav.melosys.domain.person.familie.Familiemedlem;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.pdl.PDLConsumer;
import no.nav.melosys.integrasjon.pdl.dto.HarMetadata;
import no.nav.melosys.integrasjon.pdl.dto.identer.Ident;
import no.nav.melosys.integrasjon.pdl.dto.person.Adressebeskyttelse;
import no.nav.melosys.integrasjon.pdl.dto.person.ForelderBarnRelasjon;
import no.nav.melosys.integrasjon.pdl.dto.person.Person;
import no.nav.melosys.integrasjon.pdl.dto.person.Sivilstand;
import no.nav.melosys.integrasjon.tps.TpsService;
import no.nav.melosys.service.SaksopplysningerService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
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
    private final BehandlingsresultatService behandlingsresultatService;
    private final KodeverkService kodeverkService;
    private final PDLConsumer pdlConsumer;
    private final SaksopplysningerService saksopplysningerService;
    private final TpsService tpsService;
    private final Unleash unleash;

    private static final LocalDate PDL_STARTDATO = LocalDate.parse("2020-07-01");

    @Autowired
    public PersondataService(BehandlingService behandlingService,
                             BehandlingsresultatService behandlingsresultatService,
                             KodeverkService kodeverkService,
                             @Qualifier("saksbehandler") PDLConsumer pdlConsumer,
                             SaksopplysningerService saksopplysningerService,
                             TpsService tpsService,
                             Unleash unleash) {
        this.behandlingService = behandlingService;
        this.behandlingsresultatService = behandlingsresultatService;
        this.kodeverkService = kodeverkService;
        this.pdlConsumer = pdlConsumer;
        this.saksopplysningerService = saksopplysningerService;
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
        return hentForeldreInntil(forelderBarnRelasjoner, null);
    }

    private Set<Familiemedlem> hentForeldreInntil(Collection<ForelderBarnRelasjon> forelderBarnRelasjoner,
                                                  Instant skjæringstidspunkt) {
        return forelderBarnRelasjoner.stream()
            .filter(ForelderBarnRelasjon::erForelder)
            .map(forelderBarnRelasjon -> {
                final var person = hentForelderInntil(forelderBarnRelasjon, skjæringstidspunkt);
                return lagFamilieMedlemForelder(forelderBarnRelasjon, person);
            })
            .collect(Collectors.toUnmodifiableSet());
    }

    private Person hentForelderInntil(ForelderBarnRelasjon forelderBarnRelasjon, Instant skjæringstidspunkt) {
        return skjæringstidspunkt == null ? pdlConsumer.hentForelder(forelderBarnRelasjon.relatertPersonsIdent())
            : filtrerPersondataFørDato(pdlConsumer.hentForelderMedHistorikk(forelderBarnRelasjon.relatertPersonsIdent()), skjæringstidspunkt);
    }

    private Familiemedlem lagFamilieMedlemForelder(ForelderBarnRelasjon forelderBarnRelasjon, Person person) {
        return FamiliemedlemOversetter.oversettForelder(person,
            forelderBarnRelasjon.relatertPersonsRolle());
    }

    private Set<Familiemedlem> hentRelatertVedSivilstand(Collection<Sivilstand> sivilstandRelasjoner) {
        return hentRelatertVedSivilstandInntil(sivilstandRelasjoner, null);
    }

    private Set<Familiemedlem> hentRelatertVedSivilstandInntil(Collection<Sivilstand> sivilstandRelasjoner,
                                                               Instant skjæringstidspunkt) {
        return sivilstandRelasjoner.stream()
            .map(Sivilstand::relatertVedSivilstand)
            .filter(Objects::nonNull)
            .map(ident -> hentRelatertVedSivilstandInntil(ident, skjæringstidspunkt))
            .map(FamiliemedlemOversetter::oversettRelatertVedSivilstand)
            .collect(Collectors.toUnmodifiableSet());
    }

    private Person hentRelatertVedSivilstandInntil(String ident, Instant skjæringstidspunkt) {
        return skjæringstidspunkt == null ? pdlConsumer.hentRelatertVedSivilstand(ident)
            : filtrerPersondataFørDato(pdlConsumer.hentRelatertVedSivilstandMedHistorikk(ident), skjæringstidspunkt);
    }

    private Set<Familiemedlem> hentBarn(Person person) {
        return hentBarnInntil(person, null);
    }

    private Set<Familiemedlem> hentBarnInntil(Person person, Instant skjæringstidspunkt) {
        final var folkeregisteridentifikator = FolkeregisteridentOversetter.oversett(
            person.folkeregisteridentifikator());
        return person.forelderBarnRelasjon().stream()
            .filter(ForelderBarnRelasjon::erBarn)
            .map(ForelderBarnRelasjon::relatertPersonsIdent)
            .map(ident -> hentBarnInntil(ident, skjæringstidspunkt))
            .map(barn -> FamiliemedlemOversetter.oversettBarn(barn, folkeregisteridentifikator))
            .collect(Collectors.toUnmodifiableSet());
    }

    private Person hentBarnInntil(String ident, Instant skjæringstidspunkt) {
        return skjæringstidspunkt == null ? pdlConsumer.hentBarn(ident)
            : filtrerPersondataFørDato(pdlConsumer.hentBarnMedHistorikk(ident), skjæringstidspunkt);
    }

    @Override
    public PersonMedHistorikk hentPersonMedHistorikk(long behandlingID) {
        final var behandling = behandlingService.hentBehandlingUtenSaksopplysninger(behandlingID);
        final String ident = behandling.getFagsak().hentAktørID();
        if (behandling.erAktiv()) {
            return PersonMedHistorikkOversetter.oversett(pdlConsumer.hentPersonMedHistorikk(ident), kodeverkService);
        }

        if (LocalDate.ofInstant(behandling.getRegistrertDato(), ZoneId.systemDefault()).isBefore(PDL_STARTDATO)) {
            return PersonMedHistorikkOversetter.lagHistorikkFraTpsData(saksopplysningerService.hentTpsPersonopplysninger(behandlingID), kodeverkService);
        }

        final Instant skjæringstidspunkt = avgjørSkjæringstidspunktTilInnsyn(behandling);
        final var persondataTilInnsyn = filtrerPersondataFørDato(pdlConsumer.hentPersonMedHistorikk(ident), skjæringstidspunkt);
        return PersonMedHistorikkOversetter.oversett(persondataTilInnsyn, kodeverkService);
    }

    private Instant avgjørSkjæringstidspunktTilInnsyn(Behandling behandling) {
        if (!behandling.kanResultereIVedtak()) {
            return behandling.getEndretDato();
        }
        Behandlingsresultat behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandling.getId());
        if (behandlingsresultat.harVedtak()) {
            return behandlingsresultat.getVedtakMetadata().getVedtaksdato();
        } else {
            return behandling.getEndretDato();
        }
    }

    private static Person filtrerPersondataFørDato(Person personPDL, Instant skjæringstidspunkt) {
        final LocalDateTime localDateTime = LocalDateTime.ofInstant(skjæringstidspunkt, ZoneId.systemDefault());
        return new Person(
            Collections.emptySet(),
            filtrerFørDato(personPDL.bostedsadresse(), localDateTime),
            filtrerFørDato(personPDL.doedsfall(), localDateTime),
            filtrerFørDato(personPDL.foedsel(), localDateTime),
            filtrerFørDato(personPDL.folkeregisteridentifikator(), localDateTime),
            filtrerFørDato(personPDL.folkeregisterpersonstatus(), localDateTime),
            filtrerFørDato(personPDL.forelderBarnRelasjon(), localDateTime),
            filtrerFørDato(personPDL.foreldreansvar(), localDateTime),
            filtrerFørDato(personPDL.kjoenn(), localDateTime),
            filtrerFørDato(personPDL.kontaktadresse(), localDateTime),
            filtrerFørDato(personPDL.navn(), localDateTime),
            filtrerFørDato(personPDL.oppholdsadresse(), localDateTime),
            filtrerFørDato(personPDL.sivilstand(), localDateTime),
            filtrerFørDato(personPDL.statsborgerskap(), localDateTime),
            Collections.emptySet()
        );
    }

    private static <T extends HarMetadata> Set<T> filtrerFørDato(Collection<T> pdlOpplysning,
                                                                 LocalDateTime localDateTime) {
        return pdlOpplysning == null ? Collections.emptySet()
            : pdlOpplysning.stream().filter(x -> x.erGyldigFør(localDateTime)).collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public Set<Familiemedlem> hentFamiliemedlemmerMedHistorikk(long behandlingID) {
        final var behandling = behandlingService.hentBehandlingUtenSaksopplysninger(behandlingID);
        final String ident = behandling.getFagsak().hentAktørID();
        if (behandling.erAktiv()) {
            return hentFamiliemedlemmer(pdlConsumer.hentFamilierelasjoner(ident));
        }

        if (LocalDate.ofInstant(behandling.getRegistrertDato(), ZoneId.systemDefault()).isBefore(PDL_STARTDATO)) {
            throw new TekniskException("Henting av TPS data for behandlinger opprettet før PDL mangler.");
        }

        final Instant skjæringstidspunkt = avgjørSkjæringstidspunktTilInnsyn(behandling);
        return hentFamiliemedlemmerTilInnsyn(ident, skjæringstidspunkt);
    }

    private Set<Familiemedlem> hentFamiliemedlemmerTilInnsyn(String ident, Instant skjæringstidspunkt) {
        final Set<Familiemedlem> familiemedlemmer = new HashSet<>();
        final var persondataTilInnsyn = filtrerPersondataFørDato(pdlConsumer.hentFamilierelasjoner(ident), skjæringstidspunkt);
        if (erPersonUnder18(persondataTilInnsyn)) {
            familiemedlemmer.addAll(hentForeldreInntil(persondataTilInnsyn.forelderBarnRelasjon(), skjæringstidspunkt));
        }
        familiemedlemmer.addAll(hentRelatertVedSivilstandInntil(persondataTilInnsyn.sivilstand(), skjæringstidspunkt));
        familiemedlemmer.addAll(hentBarnInntil(persondataTilInnsyn, skjæringstidspunkt));
        return familiemedlemmer;
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
            .filter(HarMetadata::erGyldig)
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
