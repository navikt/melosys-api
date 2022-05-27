package no.nav.melosys.service.kontroll.unntak;

import java.util.Collection;
import java.util.Objects;

import no.nav.melosys.domain.Anmodningsperiode;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.persondata.PersondataFasade;
import no.nav.melosys.service.unntak.AnmodningsperiodeService;
import no.nav.melosys.service.validering.Kontrollfeil;
import org.springframework.stereotype.Component;

@Component
public class AnmodningUnntakKontrollService {

    private final AnmodningsperiodeService anmodningsperiodeService;
    private final AvklarteVirksomheterService avklarteVirksomheterService;
    private final BehandlingService behandlingService;
    private final PersondataFasade persondataFasade;

    public AnmodningUnntakKontrollService(AnmodningsperiodeService anmodningsperiodeService,
                                          AvklarteVirksomheterService avklarteVirksomheterService, BehandlingService behandlingService,
                                          PersondataFasade persondataFasade) {
        this.anmodningsperiodeService = anmodningsperiodeService;
        this.avklarteVirksomheterService = avklarteVirksomheterService;
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
        final int antallArbeidsgivere = avklarteVirksomheterService.hentAntallAvklarteVirksomheter(behandling);
        AnmodningUnntakKontrollData kontrollData = new AnmodningUnntakKontrollData(persondata,
            behandling.getBehandlingsgrunnlag().getBehandlingsgrunnlagdata(),
            anmodningsperiode,
            antallArbeidsgivere
        );
        return AnmodningUnntakRegelsett.hentRegler().stream()
            .map(f -> f.apply(kontrollData))
            .filter(Objects::nonNull)
            .toList();
    }
}
