package no.nav.melosys.service.persondata.familie;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.person.Folkeregisteridentifikator;
import no.nav.melosys.domain.person.familie.Familiemedlem;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.integrasjon.pdl.PDLConsumer;
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
        return hentFamiliemedlemmer(person);
    }

    public Set<Familiemedlem> hentFamiliemedlemmer(Person hovedperson) {
        Set<Familiemedlem> familiemedlemmer = new HashSet<>();
        if (erPersonUnder18(hovedperson)) {
            familiemedlemmer.addAll(hentForeldre(hovedperson.forelderBarnRelasjon()));
        }

        familiemedlemmer.addAll(hentEktefellerOgPartnere(hovedperson));
        familiemedlemmer.addAll(hentBarn(hovedperson));
        return familiemedlemmer;
    }

    @NotNull
    private Collection<Familiemedlem> hentEktefellerOgPartnere(Person hovedperson) {
        List<Sivilstand> aktiveSivilstander = hovedperson.sivilstand().stream().toList();

        if (aktiveSivilstander.isEmpty()) {
            log.info("Har ingen ektefelle/partner");
            return Collections.emptySet();
        }

        if (aktiveSivilstander.size() > 1) {
            log.info("Fant {} aktiv sivilstand, begynner å lete etter siste registrerte aktiv sivilstand",
                aktiveSivilstander.size());
            Optional<Sivilstand> sisteSivilstand = aktiveSivilstander.stream().min(this::sammenlignSisteDatoRegistrert);
            return hentEktefelleEllerPartner(sisteSivilstand.get());
        }

        return hentEktefelleEllerPartner(aktiveSivilstander.get(0));
    }

    @NotNull
    private Set<Familiemedlem> hentEktefelleEllerPartner(Sivilstand sivilstand) {
        log.info("Henter ektefelle/partner registrert {} av typen {}",
            sivilstand.hentDatoSistRegistrert(), sivilstand.type());

        if (sivilstand.erGyldigSomEktefelleEllerPartner()) {
            log.info("Fant bare ett aktiv familiemedlem, lager et familiemedlem med sivilstand: {}", sivilstand);
            return lagFamiliemedlemFraSivilstand(sivilstand);
        } else {
            log.info("Fant ingen gyldige sivilstand som kan være familiemedlem");
            return Collections.emptySet();
        }
    }

    @NotNull
    private Set<Familiemedlem> lagFamiliemedlemFraSivilstand(Sivilstand sivilstand) {
        String ident = sivilstand.relatertVedSivilstand();
        Person person = pdlConsumer.hentEktefelleEllerPartner(ident);
        return Set.of(FamiliemedlemOversetter.oversettEktefelleEllerPartner(person, sivilstand));
    }

    @NotNull
    private EktefelleEllerPartner lagEktefelleEllerPartner(String ident, String fødselsNrTilHovedperson) {
        Person person = pdlConsumer.hentEktefelleEllerPartner(ident);
        Sivilstand sivilstand = hentSisteSivilstandKnyttetTilHovedperson(person, fødselsNrTilHovedperson);
        EktefelleEllerPartner ektefelleEllerPartner = new EktefelleEllerPartner(ident, person, sivilstand);
        log.info("Opprettet EktefelleEllerPartner med data: {}", ektefelleEllerPartner);
        return ektefelleEllerPartner;
    }

    @NotNull
    Sivilstand hentSisteSivilstandKnyttetTilHovedperson(Person person, String fødselsNrTilHovedperson) {
        Sivilstand sisteSivilstand = person.sivilstand().stream()
            .filter(Objects::nonNull)
            .filter(sivilstand -> erRelatertTilHovedperson(sivilstand, fødselsNrTilHovedperson))
            .min(this::sammenlignSisteDatoRegistrert)
            .orElseThrow(() -> new IkkeFunnetException("I Ektefelle/Partner fant vi ikke forventet Sivilstand " +
                "knyttet til hovedperson"));

        // TODO: Ta vekk pga. GDPR
        log.info("Siste sivilstand til {} var: {}", fødselsNrTilHovedperson, sisteSivilstand);
        return sisteSivilstand;
    }

    private boolean erRelatertTilHovedperson(Sivilstand sivilstand, String fødselsNrTilHovedperson) {
        return fødselsNrTilHovedperson.equals(sivilstand.relatertVedSivilstand());
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

    record EktefelleEllerPartner(String ident, Person person, Sivilstand sivilstand) {
        public boolean erAktiv() {
            return !sivilstand.metadata().historisk();
        }
    }
}
