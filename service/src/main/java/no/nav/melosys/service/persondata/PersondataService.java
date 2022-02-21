package no.nav.melosys.service.persondata;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import no.finn.unleash.Unleash;
import no.nav.melosys.domain.Saksopplysning;
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
import no.nav.melosys.integrasjon.pdl.dto.person.ForelderBarnRelasjon;
import no.nav.melosys.integrasjon.pdl.dto.person.Person;
import no.nav.melosys.integrasjon.pdl.dto.person.Sivilstand;
import no.nav.melosys.integrasjon.tps.TpsService;
import no.nav.melosys.service.SaksopplysningerService;
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
    private final SaksopplysningerService saksopplysningerService;
    private final TpsService tpsService;
    private final Unleash unleash;

    public static final String PDL_PERSOPL_VERSJON = "1.0";
    public static final String PDL_PERS_SAKS_VERSJON = "1.0";

    @Autowired
    public PersondataService(BehandlingService behandlingService, KodeverkService kodeverkService, @Qualifier("saksbehandler") PDLConsumer pdlConsumer,
                             SaksopplysningerService saksopplysningerService, TpsService tpsService, Unleash unleash) {
        this.behandlingService = behandlingService;
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
        return hentForeldreInntil(forelderBarnRelasjoner);
    }

    private Set<Familiemedlem> hentForeldreInntil(Collection<ForelderBarnRelasjon> forelderBarnRelasjoner) {
        return forelderBarnRelasjoner.stream()
            .filter(ForelderBarnRelasjon::erForelder)
            .map(forelderBarnRelasjon -> {
                final var person = hentForelderInntil(forelderBarnRelasjon);
                return lagFamilieMedlemForelder(forelderBarnRelasjon, person);
            })
            .collect(Collectors.toUnmodifiableSet());
    }

    private Person hentForelderInntil(ForelderBarnRelasjon forelderBarnRelasjon) {
        return pdlConsumer.hentForelder(forelderBarnRelasjon.relatertPersonsIdent());
    }

    private Familiemedlem lagFamilieMedlemForelder(ForelderBarnRelasjon forelderBarnRelasjon, Person person) {
        return FamiliemedlemOversetter.oversettForelder(person,
            forelderBarnRelasjon.relatertPersonsRolle());
    }

    private Set<Familiemedlem> hentRelatertVedSivilstand(Collection<Sivilstand> sivilstandRelasjoner) {
        return hentRelatertVedSivilstandInntil(sivilstandRelasjoner);
    }

    private Set<Familiemedlem> hentRelatertVedSivilstandInntil(Collection<Sivilstand> sivilstandRelasjoner) {
        return sivilstandRelasjoner.stream()
            .map(Sivilstand::relatertVedSivilstand)
            .filter(Objects::nonNull)
            .map(pdlConsumer::hentRelatertVedSivilstand)
            .map(FamiliemedlemOversetter::oversettRelatertVedSivilstand)
            .collect(Collectors.toUnmodifiableSet());
    }

    private Set<Familiemedlem> hentBarn(Person person) {
        return hentBarnInntil(person);
    }

    private Set<Familiemedlem> hentBarnInntil(Person person) {
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
        final var behandling = behandlingService.hentBehandling(behandlingID);
        final String ident = behandling.getFagsak().hentAktørID();
        if (behandling.erAktiv()) {
            return hentPersonMedHistorikk(ident);
        }

        return saksopplysningerService.finnPdlPersonhistorikkTilSaksbehandler(behandlingID)
            .orElseGet(() -> PersonMedHistorikkOversetter.lagHistorikkFraTpsData(saksopplysningerService.hentTpsPersonopplysninger(behandlingID), kodeverkService));
    }

    @Override
    public PersonMedHistorikk hentPersonMedHistorikk(String ident) {
        return PersonMedHistorikkOversetter.oversett(pdlConsumer.hentPersonMedHistorikk(ident), kodeverkService);
    }

    @Override
    public Set<Familiemedlem> hentFamiliemedlemmerMedHistorikk(long behandlingID) {
        final var behandling = behandlingService.hentBehandling(behandlingID);
        final String ident = behandling.getFagsak().hentAktørID();
        if (behandling.erAktiv()) {
            return hentFamiliemedlemmerMedHistorikk(ident);
        }

        if (saksopplysningerService.harTpsPersonopplysninger(behandlingID)) {
            return saksopplysningerService.hentTpsPersonopplysninger(behandlingID).hentFamiliemedlemmer();
        }

        return saksopplysningerService.hentPdlPersonopplysninger(behandlingID).hentFamiliemedlemmer();
    }

    @Override
    public Set<Familiemedlem> hentFamiliemedlemmerMedHistorikk(String ident) {
        return hentFamiliemedlemmer(pdlConsumer.hentFamilierelasjoner(ident));
    }

    @Override
    public Saksopplysning hentPersonhistorikk(String fnr, LocalDate dato) {
        return tpsService.hentPersonhistorikk(fnr, dato);
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
        if (unleash.isEnabled("melosys.pdl.aktiv")) {
            return pdlConsumer.hentAdressebeskyttelser(ident).stream().anyMatch(Adressebeskyttelse::erStrengtFortrolig);
        }
        return tpsService.harStrengtFortroligAdresse(ident);
    }
}
