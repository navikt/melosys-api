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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Service
@Primary
public class GodkjennUnntakKontrollService {

    private static final Logger log = LoggerFactory.getLogger(GodkjennUnntakKontrollService.class);
    private final BehandlingService behandlingService;

    public GodkjennUnntakKontrollService(BehandlingService behandlingService) {
        this.behandlingService = behandlingService;
    }

    public void utførKontroll(long behandlingID) {
        Behandling behandling = behandlingService.hentBehandling(behandlingID);
        utførKontroll(behandling);
    }

    public void utførKontroll(Behandling behandling) {
        if (behandling.getTema() == null || behandling.hentSedDokument() == null) {
            log.debug("Utfører ikke GodkjennUnntakKontroll: behandling har ikke tema eller Sed " +
                    "dokument på behandling med ID '{}'",
                behandling.getId());
            return;
        }
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
            throw new ValideringException("Feil i unntak som gjør at vi ikke kan manuelt godkjenne",
                feilValideringer.stream().map(Kontrollfeil::tilDto).toList());
        }
    }
}
