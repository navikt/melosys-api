package no.nav.melosys.service.kontroll.feature.godkjennunntak;

import java.util.List;
import java.util.Objects;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.eessi.SedType;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.exception.ValideringException;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.kontroll.feature.godkjennunntak.data.GodkjennUnntakKontrollData;
import no.nav.melosys.service.kontroll.feature.godkjennunntak.kontroll.GodkjennUnntakKontrollsett;
import no.nav.melosys.service.validering.Kontrollfeil;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Service
@Primary
public class GodkjennUnntakKontrollService {

    private final BehandlingService behandlingService;

    public GodkjennUnntakKontrollService(BehandlingService behandlingService) {
        this.behandlingService = behandlingService;
    }

    public void utførKontroller(long behandlingID) {
        Behandling behandling = behandlingService.hentBehandling(behandlingID);

        Behandlingstema behandlingstema = behandling.getTema();
        SedType sedType = behandling.hentSedDokument().getSedType();

        GodkjennUnntakKontrollData kontrollData =
            new GodkjennUnntakKontrollData(behandling.hentSedDokument().getLovvalgsperiode());
        List<Kontrollfeil> feilValideringer = GodkjennUnntakKontrollsett.hentRegelsett(behandlingstema, sedType)
            .stream()
            .map(f -> f.apply(kontrollData))
            .filter(Objects::nonNull)
            .toList();

        if (!feilValideringer.isEmpty()) {
            throw new ValideringException("Feil i godkjenn unntak validering",
                feilValideringer.stream().map(Kontrollfeil::tilDto).toList());
        }
    }
}
