package no.nav.melosys.service.kontroll.feature.unntaksperiode;

import java.util.List;
import java.util.Objects;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.ErPeriode;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.ValideringException;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.kontroll.feature.unntaksperiode.data.UnntaksperiodeKontrollData;
import no.nav.melosys.service.kontroll.feature.unntaksperiode.kontroll.UnntaksperiodeKontrollsett;
import no.nav.melosys.service.validering.Kontrollfeil;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Primary
public class UnntaksperiodeKontrollService {

    private static final Logger log = LoggerFactory.getLogger(UnntaksperiodeKontrollService.class);
    private final BehandlingService behandlingService;

    public UnntaksperiodeKontrollService(BehandlingService behandlingService) {
        this.behandlingService = behandlingService;
    }

    @Transactional(readOnly = true)
    public void kontrollPeriode(Long behandlingID, ErPeriode periode) {
        Behandling behandling = hentBehandling(behandlingID);
        kontrollPeriode(behandling, periode);
    }

    @Transactional(readOnly = true)
    public void kontrollPeriode(Behandling behandling, ErPeriode periode) {
        List<Kontrollfeil> feilmeldinger = utførKontroll(behandling, periode);
        sjekkFeilmeldinger(feilmeldinger);
    }

    @NotNull
    private Behandling hentBehandling(Long behandlingID) {
        Behandling behandling = behandlingService.hentBehandling(behandlingID);
        if (behandling.hentSedDokument() == null) {
            String feilmelding = "Ugyldig bruk av API for behandling" +
                " med behandlingID '%s'. Mangler SED Dokument.".formatted(behandling.getId());
            log.warn(feilmelding);
            throw new FunksjonellException(feilmelding);
        }
        return behandling;
    }

    private List<Kontrollfeil> utførKontroll(Behandling behandling, ErPeriode periode) {
        UnntaksperiodeKontrollData kontrollData = new UnntaksperiodeKontrollData(periode.getFom(), periode.getTom());
        return UnntaksperiodeKontrollsett.hentRegelsett(behandling)
            .stream()
            .map(f -> f.apply(kontrollData))
            .filter(Objects::nonNull)
            .toList();
    }

    private void sjekkFeilmeldinger(List<Kontrollfeil> feilmeldinger) throws ValideringException {
        if (!feilmeldinger.isEmpty()) {
            throw new ValideringException("Validering av unntaksperiode feilet",
                feilmeldinger.stream().map(Kontrollfeil::tilDto).toList());
        }
    }
}
