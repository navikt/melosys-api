package no.nav.melosys.service.persondata.familie;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.person.Folkeregisteridentifikator;
import no.nav.melosys.domain.person.familie.Familiemedlem;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.integrasjon.pdl.PDLConsumer;
import no.nav.melosys.integrasjon.pdl.dto.HarMetadata;
import no.nav.melosys.integrasjon.pdl.dto.person.ForelderBarnRelasjon;
import no.nav.melosys.integrasjon.pdl.dto.person.Person;
import no.nav.melosys.integrasjon.pdl.dto.person.Sivilstand;
import no.nav.melosys.service.SaksopplysningerService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.persondata.mapping.FamiliemedlemOversetter;
import no.nav.melosys.service.persondata.mapping.FoedselOversetter;
import no.nav.melosys.service.persondata.mapping.FolkeregisteridentOversetter;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.time.temporal.ChronoUnit.YEARS;

@Service
public class FamiliemedlemService {
    private static final Logger log = LoggerFactory.getLogger(FamiliemedlemService.class);
    private final BehandlingService behandlingService;
    private final SaksopplysningerService saksopplysningerService;
    private final PDLConsumer pdlConsumer;

    public FamiliemedlemService(BehandlingService behandlingService,
                                SaksopplysningerService saksopplysningerService,
                                @Qualifier("saksbehandler") PDLConsumer pdlConsumer) {
        this.behandlingService = behandlingService;
        this.saksopplysningerService = saksopplysningerService;
        this.pdlConsumer = pdlConsumer;
    }

    public Set<Familiemedlem> hentFamiliemedlemmerFraBehandlingID(long behandlingID) {
        log.debug("[MELOSYS-{}] Henter familiemedlemmer fra behandlingID", behandlingID);
        Behandling behandling = behandlingService.hentBehandling(behandlingID);
        String ident = behandling.getFagsak().hentBrukersAktørID();

        if (behandling.erAktiv()) {
            log.debug("[MELOSYS-{}] Er en aktiv behandling, og tar i bruk ident", behandlingID);
            return hentFamiliemedlemmerFraIdent(ident);
        }
        log.debug("[MELOSYS-{}] Er ikke en aktiv behandling", behandlingID);

        if (saksopplysningerService.harTpsPersonopplysninger(behandlingID)) {
            log.debug("[MELOSYS-{}] Henter TPS personopplysinger", behandlingID);
            return saksopplysningerService.hentTpsPersonopplysninger(behandlingID).hentFamiliemedlemmer();
        }

        log.debug("[MELOSYS-{}] Henter PDL personopplysinger", behandlingID);
        return saksopplysningerService.hentPdlPersonopplysninger(behandlingID).hentFamiliemedlemmer();
    }

    public Set<Familiemedlem> hentFamiliemedlemmerFraIdent(String ident) {
        Person person = pdlConsumer.hentFamilierelasjoner(ident);
        return hentFamiliemedlemmer(person, ident);
    }

    public Set<Familiemedlem> hentFamiliemedlemmer(Person hovedperson, String identTilHovedperson) {
        Set<Familiemedlem> familiemedlemmer = new HashSet<>();
        if (erPersonUnder18(hovedperson)) {
            familiemedlemmer.addAll(hentForeldre(hovedperson.forelderBarnRelasjon()));
        }

        familiemedlemmer.addAll(hentEktefellerOgPartnere(hovedperson, identTilHovedperson));
        familiemedlemmer.addAll(hentBarn(hovedperson));
        return familiemedlemmer;
    }

    @NotNull
    private Collection<Familiemedlem> hentEktefellerOgPartnere(Person hovedperson, String identTilHovedperson) {
        var unikeIdenter = hentUnikeIdenterFraSivilstandTilHovedperson(hovedperson);
        return unikeIdenter.map(ident -> oversettTilEktefelleEllerPartner(ident, identTilHovedperson))
                .filter(EktefelleEllerPartnerFamiliemedlem::erAktiv)
                .map(ektefelleEllerPartner ->
                        FamiliemedlemOversetter.oversettEktefelleEllerPartner(
                                ektefelleEllerPartner.getPerson(),
                                ektefelleEllerPartner.getSivilstand()))
                .collect(Collectors.toUnmodifiableSet());
    }

    @NotNull
    private EktefelleEllerPartnerFamiliemedlem oversettTilEktefelleEllerPartner(String ident, String identTilHovedperson) {
        Person person = pdlConsumer.hentEktefelleEllerPartner(ident);
        Sivilstand sivilstand = hentSisteSivilstandKnyttetTilHovedperson(person, identTilHovedperson);
        return new EktefelleEllerPartnerFamiliemedlem(ident, person, sivilstand);
    }

    @NotNull
    private Sivilstand hentSisteSivilstandKnyttetTilHovedperson(Person person, String identTilHovedperson) {
        return person.sivilstand().stream()
                .filter(Objects::nonNull)
                .filter(sivilstand -> identTilHovedperson.equals(sivilstand.relatertVedSivilstand()))
                .min(this::sammenlignSisteDatoRegistrert)
                .orElseThrow(() -> new IkkeFunnetException("I Ektefelle/Partner fant vi ikke forventet Sivilstand " +
                        "knyttet til hovedperson"));
    }

    @NotNull
    private Stream<String> hentUnikeIdenterFraSivilstandTilHovedperson(Person hovedperson) {
        return hovedperson.sivilstand().stream()
                .filter(HarMetadata::erIkkeHistorisk)
                .map(Sivilstand::relatertVedSivilstand)
                .filter(Objects::nonNull)
                .distinct();
    }

    private boolean erPersonUnder18(Person person) {
        return YEARS.between(FoedselOversetter.oversett(person.foedsel()).fødselsdato(), LocalDate.now()) < 18;
    }

    private Set<Familiemedlem> hentForeldre(Collection<ForelderBarnRelasjon> forelderBarnRelasjoner) {
        return forelderBarnRelasjoner.stream()
                .filter(ForelderBarnRelasjon::erForelder)
                .map(forelderBarnRelasjon -> {
                    final Person person = pdlConsumer.hentForelder(forelderBarnRelasjon.relatertPersonsIdent());
                    return lagFamilieMedlemForelder(forelderBarnRelasjon, person);
                })
                .collect(Collectors.toUnmodifiableSet());
    }

    private Familiemedlem lagFamilieMedlemForelder(ForelderBarnRelasjon forelderBarnRelasjon, Person forelder) {
        return FamiliemedlemOversetter.oversettForelder(forelder,
                forelderBarnRelasjon.relatertPersonsRolle());
    }

    private int sammenlignSisteDatoRegistrert(Sivilstand sivilstand1, Sivilstand sivilstand2) {
        return sivilstand2.hentDatoSistRegistrert().compareTo(sivilstand1.hentDatoSistRegistrert());
    }

    private Set<Familiemedlem> hentBarn(Person person) {
        Folkeregisteridentifikator folkeregisteridentifikator = FolkeregisteridentOversetter
                .oversett(person.folkeregisteridentifikator());
        return person.forelderBarnRelasjon().stream()
                .filter(ForelderBarnRelasjon::erBarn)
                .map(ForelderBarnRelasjon::relatertPersonsIdent)
                .map(pdlConsumer::hentBarn)
                .map(barn -> FamiliemedlemOversetter.oversettBarn(barn, folkeregisteridentifikator))
                .collect(Collectors.toUnmodifiableSet());
    }

    class EktefelleEllerPartnerFamiliemedlem {
        private final String ident;
        private final Person person;
        private final Sivilstand sivilstand;

        public EktefelleEllerPartnerFamiliemedlem(String ident, Person person, Sivilstand sivilstand) {
            this.ident = ident;
            this.person = person;
            this.sivilstand = sivilstand;
        }

        public boolean erAktiv() {
            return !sivilstand.metadata().historisk();
        }

        public String getIdent() {
            return ident;
        }

        public Person getPerson() {
            return person;
        }

        public Sivilstand getSivilstand() {
            return sivilstand;
        }
    }
}
