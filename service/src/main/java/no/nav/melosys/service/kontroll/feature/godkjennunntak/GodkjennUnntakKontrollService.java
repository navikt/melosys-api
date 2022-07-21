package no.nav.melosys.service.kontroll.feature.godkjennunntak;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.dokument.medlemskap.Periode;
import no.nav.melosys.domain.dokument.sed.SedDokument;
import no.nav.melosys.exception.ValideringException;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.kontroll.feature.godkjennunntak.data.GodkjennUnntakKontrollData;
import no.nav.melosys.service.kontroll.feature.godkjennunntak.kontroll.GodkjennUnntakKontrollsett;
import no.nav.melosys.service.validering.Kontrollfeil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Primary
public class GodkjennUnntakKontrollService {

    private static final Logger log = LoggerFactory.getLogger(GodkjennUnntakKontrollService.class);
    private final BehandlingService behandlingService;

    public GodkjennUnntakKontrollService(BehandlingService behandlingService) {
        this.behandlingService = behandlingService;
    }

    @Transactional(readOnly = true)
    public void utførKontroll(long behandlingID) {
        Behandling behandling = behandlingService.hentBehandling(behandlingID);
        utførKontroll(behandling);
    }

    @Transactional(readOnly = true)
    public void utførKontroll(Behandling behandling) {
        SedDokument sedDokument = behandling.hentSedDokument();
        if (sedDokument == null) {
            log.debug("Ikke relevant for behandling med behandlingID '{}'", behandling.getId());
            return;
        }

        Periode lovvalgsperiode = sedDokument.getLovvalgsperiode();
        GodkjennUnntakKontrollData kontrollData = new GodkjennUnntakKontrollData(
            lovvalgsperiode.getFom(),
            lovvalgsperiode.getTom());

        utførKontrollOgSjekkFeilmeldinger(behandling, kontrollData);
    }

    @Transactional(readOnly = true)
    public void kontrollPeriode(Long behandlingID, LocalDate periodeFom, LocalDate periodeTom) {
        Behandling behandling = behandlingService.hentBehandling(behandlingID);
        if (behandling.hentSedDokument() == null) {
            log.debug("Ikke relevant for behandling uten tema eller sed med behandlingID '{}'", behandling.getId());
            return;
        }

        GodkjennUnntakKontrollData kontrollData = new GodkjennUnntakKontrollData(periodeFom, periodeTom);
        utførKontrollOgSjekkFeilmeldinger(behandling, kontrollData);
    }

    private void utførKontrollOgSjekkFeilmeldinger(Behandling behandling, GodkjennUnntakKontrollData kontrollData) {
        List<Kontrollfeil> feilValideringer = GodkjennUnntakKontrollsett.hentRegelsett(behandling)
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
