package no.nav.melosys.service.persondata.familie;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.person.Folkeregisteridentifikator;
import no.nav.melosys.domain.person.familie.Familiemedlem;
import no.nav.melosys.integrasjon.pdl.PDLConsumer;
import no.nav.melosys.integrasjon.pdl.dto.person.ForelderBarnRelasjon;
import no.nav.melosys.integrasjon.pdl.dto.person.Person;
import no.nav.melosys.service.SaksopplysningerService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.persondata.familie.medlem.EktefelleEllerPartnerFamiliemedlemFilter;
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
    private final EktefelleEllerPartnerFamiliemedlemFilter ektefelleEllerPartnerFamiliemedlemFilter;
    private final PDLConsumer pdlConsumer;

    public FamiliemedlemService(BehandlingService behandlingService,
                                SaksopplysningerService saksopplysningerService,
                                EktefelleEllerPartnerFamiliemedlemFilter ektefelleEllerPartnerFamiliemedlemFilter,
                                @Qualifier("saksbehandler") PDLConsumer pdlConsumer) {
        this.behandlingService = behandlingService;
        this.saksopplysningerService = saksopplysningerService;
        this.ektefelleEllerPartnerFamiliemedlemFilter = ektefelleEllerPartnerFamiliemedlemFilter;
        this.pdlConsumer = pdlConsumer;
    }

    public Set<Familiemedlem> hentFamiliemedlemmerFraBehandlingID(long behandlingID) {
        Behandling behandling = behandlingService.hentBehandling(behandlingID);
        String ident = behandling.getFagsak().hentBrukersAktørID();

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

    public Set<Familiemedlem> hentFamiliemedlemmer(Person hovedperson) {
        Set<Familiemedlem> familiemedlemmer = new HashSet<>();
        if (erPersonUnder18(hovedperson)) {
            familiemedlemmer.addAll(hentForeldre(hovedperson.forelderBarnRelasjon()));
        }
        familiemedlemmer.addAll(ektefelleEllerPartnerFamiliemedlemFilter.hentEktefelleEllerPartnerFraSivilstander(
            hovedperson.sivilstand()));
        familiemedlemmer.addAll(hentBarn(hovedperson));
        return familiemedlemmer;
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
}
