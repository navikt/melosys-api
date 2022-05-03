package no.nav.melosys.service.persondata.familie;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.person.familie.Familiemedlem;
import no.nav.melosys.integrasjon.pdl.PDLConsumer;
import no.nav.melosys.integrasjon.pdl.dto.person.ForelderBarnRelasjon;
import no.nav.melosys.integrasjon.pdl.dto.person.Person;
import no.nav.melosys.integrasjon.pdl.dto.person.Sivilstand;
import no.nav.melosys.service.SaksopplysningerService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.persondata.mapping.FamiliemedlemOversetter;
import no.nav.melosys.service.persondata.mapping.FoedselOversetter;
import no.nav.melosys.service.persondata.mapping.FolkeregisteridentOversetter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import static java.time.temporal.ChronoUnit.YEARS;

@Service
public class FamiliemedlemService {

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
        final Behandling behandling = behandlingService.hentBehandling(behandlingID);
        final String ident = behandling.getFagsak().hentBrukersAktørID();
        if (behandling.erAktiv()) {
            return hentFamiliemedlemmerFraIdent(ident);
        }

        if (saksopplysningerService.harTpsPersonopplysninger(behandlingID)) {
            return saksopplysningerService.hentTpsPersonopplysninger(behandlingID).hentFamiliemedlemmer();
        }

        return saksopplysningerService.hentPdlPersonopplysninger(behandlingID).hentFamiliemedlemmer();
    }

    public Set<Familiemedlem> hentFamiliemedlemmerFraIdent(String ident) {
        Person person = pdlConsumer.hentFamilierelasjoner(ident);
        return hentFamiliemedlemmer(person);
    }

    public Set<Familiemedlem> hentFamiliemedlemmer(Person person) {
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
        return forelderBarnRelasjoner.stream()
            .filter(ForelderBarnRelasjon::erForelder)
            .map(forelderBarnRelasjon -> {
                final var person = pdlConsumer.hentForelder(forelderBarnRelasjon.relatertPersonsIdent());
                return lagFamilieMedlemForelder(forelderBarnRelasjon, person);
            })
            .collect(Collectors.toUnmodifiableSet());
    }

    private Familiemedlem lagFamilieMedlemForelder(ForelderBarnRelasjon forelderBarnRelasjon, Person person) {
        return FamiliemedlemOversetter.oversettForelder(person,
            forelderBarnRelasjon.relatertPersonsRolle());
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
}
