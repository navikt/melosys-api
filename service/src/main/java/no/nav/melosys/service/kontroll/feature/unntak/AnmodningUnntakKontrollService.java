package no.nav.melosys.service.kontroll.feature.unntak;

import java.util.Collection;
import java.util.Objects;

import no.nav.melosys.domain.Anmodningsperiode;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.service.avklartefakta.OppsummerteAvklarteFaktaService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.kontroll.feature.unntak.data.AnmodningUnntakKontrollData;
import no.nav.melosys.service.kontroll.feature.unntak.kontroll.AnmodningUnntakKontrollsett;
import no.nav.melosys.service.persondata.PersondataFasade;
import no.nav.melosys.service.unntak.AnmodningsperiodeService;
import no.nav.melosys.service.validering.Kontrollfeil;
import org.springframework.stereotype.Service;

@Service
public class AnmodningUnntakKontrollService {

    private final AnmodningsperiodeService anmodningsperiodeService;
    private final OppsummerteAvklarteFaktaService oppsummerteAvklarteFaktaService;
    private final BehandlingService behandlingService;
    private final PersondataFasade persondataFasade;

    public AnmodningUnntakKontrollService(AnmodningsperiodeService anmodningsperiodeService,
                                          OppsummerteAvklarteFaktaService oppsummerteAvklarteFaktaService, BehandlingService behandlingService,
                                          PersondataFasade persondataFasade) {
        this.anmodningsperiodeService = anmodningsperiodeService;
        this.oppsummerteAvklarteFaktaService = oppsummerteAvklarteFaktaService;
        this.behandlingService = behandlingService;
        this.persondataFasade = persondataFasade;
    }

    public Collection<Kontrollfeil> utførKontroller(long behandlingID) {
        return utførKontroller(
            behandlingService.hentBehandlingMedSaksopplysninger(behandlingID),
            anmodningsperiodeService.hentFørsteAnmodningsperiode(behandlingID)
        );
    }

    private Collection<Kontrollfeil> utførKontroller(
        Behandling behandling,
        Anmodningsperiode anmodningsperiode
    ) {
        final var persondata = persondataFasade.hentPerson(behandling.getFagsak().hentBrukersAktørID());
        final int antallArbeidsgivere = oppsummerteAvklarteFaktaService.hentAntallAvklarteVirksomheter(behandling);
        AnmodningUnntakKontrollData kontrollData = new AnmodningUnntakKontrollData(persondata,
            behandling.getMottatteOpplysninger().getMottatteOpplysningerData(),
            anmodningsperiode,
            antallArbeidsgivere
        );
        return AnmodningUnntakKontrollsett.hentRegler().stream()
            .map(f -> f.apply(kontrollData))
            .filter(Objects::nonNull)
            .toList();
    }
}
