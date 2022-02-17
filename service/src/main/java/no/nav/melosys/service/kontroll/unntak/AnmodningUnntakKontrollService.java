package no.nav.melosys.service.kontroll.unntak;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import no.finn.unleash.Unleash;
import no.nav.melosys.domain.Anmodningsperiode;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.kodeverk.begrunnelser.Kontroll_begrunnelser;
import no.nav.melosys.domain.person.Persondata;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.kontroll.AdresseUtlandKontroller;
import no.nav.melosys.service.kontroll.PersonKontroller;
import no.nav.melosys.service.persondata.PersondataFasade;
import no.nav.melosys.service.unntak.AnmodningsperiodeService;
import no.nav.melosys.service.validering.Kontrollfeil;
import org.springframework.stereotype.Component;

@Component
public class AnmodningUnntakKontrollService implements AdresseUtlandKontroller {

    private final AnmodningsperiodeService anmodningsperiodeService;
    private final BehandlingService behandlingService;
    private final PersondataFasade persondataFasade;
    private final Unleash unleash;

    public AnmodningUnntakKontrollService(AnmodningsperiodeService anmodningsperiodeService,
                                          BehandlingService behandlingService,
                                          PersondataFasade persondataFasade,
                                          Unleash unleash) {
        this.anmodningsperiodeService = anmodningsperiodeService;
        this.behandlingService = behandlingService;
        this.persondataFasade = persondataFasade;
        this.unleash = unleash;
    }

    public Collection<Kontrollfeil> utførKontroller(long behandlingID) {
        return utførKontroller(
            behandlingService.hentBehandlingMedSaksopplysninger(behandlingID),
            anmodningsperiodeService.hentFørsteAnmodningsperiode(behandlingID),
            anmodningUnntakKontroller()
        );
    }

    private static Set<Function<AnmodningUnntakKontrollData, Kontrollfeil>> anmodningUnntakKontroller() {
        return Set.of(
            AnmodningUnntakKontrollService::bostedsadresseForOrienteringAnmodningUnntak,
            AnmodningUnntakKontrollService::anmodningsperiodeManglerSluttdato,
            kontrollData -> AdresseUtlandKontroller.arbeidsstedManglerFelter(kontrollData.getBehandlingsgrunnlagData()),
            kontrollData -> AdresseUtlandKontroller.foretakUtlandManglerFelter(kontrollData.getBehandlingsgrunnlagData())
        );
    }

    private Collection<Kontrollfeil> utførKontroller(
        Behandling behandling,
        Anmodningsperiode anmodningsperiode,
        Set<Function<AnmodningUnntakKontrollData, Kontrollfeil>> kontroller
    ) {
        final var persondata = hentPersondata(behandling);
        AnmodningUnntakKontrollData kontrollData = new AnmodningUnntakKontrollData(persondata,
            behandling.getBehandlingsgrunnlag().getBehandlingsgrunnlagdata(),
            anmodningsperiode
        );
        return kontroller.stream()
            .map(f -> f.apply(kontrollData))
            .filter(Objects::nonNull)
            .toList();
    }

    private Persondata hentPersondata(Behandling behandling) {
        if (unleash.isEnabled("melosys.pdl.aktiv")) {
            return persondataFasade.hentPerson(behandling.getFagsak().hentAktørID());
        }
        return behandling.hentPersonDokument();
    }

    static Kontrollfeil bostedsadresseForOrienteringAnmodningUnntak(AnmodningUnntakKontrollData kontrollData) {
        return PersonKontroller.harRegistrertBostedsadresse(kontrollData.getPersonDokument(), kontrollData.getBehandlingsgrunnlagData())
            ? null : new Kontrollfeil(Kontroll_begrunnelser.MANGLENDE_BOSTEDSADRESSE);
    }

    static Kontrollfeil anmodningsperiodeManglerSluttdato(AnmodningUnntakKontrollData kontrollData) {
        return kontrollData.getAnmodningsperiode().getTom() == null
            ? new Kontrollfeil(Kontroll_begrunnelser.INGEN_SLUTTDATO) : null;
    }
}
