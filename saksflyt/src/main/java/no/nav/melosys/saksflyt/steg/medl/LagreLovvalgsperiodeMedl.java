package no.nav.melosys.saksflyt.steg.medl;

import java.util.Optional;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.saksflyt.steg.StegBehandler;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.medl.MedlPeriodeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LagreLovvalgsperiodeMedl implements StegBehandler {

    private static final Logger log = LoggerFactory.getLogger(LagreLovvalgsperiodeMedl.class);

    private final BehandlingsresultatService behandlingsresultatService;
    private final MedlPeriodeService medlPeriodeService;


    public LagreLovvalgsperiodeMedl(BehandlingsresultatService behandlingsresultatService,
                                    MedlPeriodeService medlPeriodeService) {
        this.behandlingsresultatService = behandlingsresultatService;
        this.medlPeriodeService = medlPeriodeService;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return ProsessSteg.LAGRE_LOVVALGSPERIODE_MEDL;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) {
        final long behandlingID = prosessinstans.getBehandling().getId();
        final var behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingID);
        if (behandlingsresultat.erAvslagManglendeOpplysninger()) {
            return;
        }

        final var lovvalgsperiode = behandlingsresultat.hentLovvalgsperiode();
        final var behandling = prosessinstans.getBehandling();
        if (behandling.erNyVurdering()) {
            lovvalgsperiode.setMedlPeriodeID(finnOpprinneligMedlPeriodeID(behandling).orElse(null));
        }
        oppdaterLovvalgsperiode(behandling.getId(), lovvalgsperiode);
    }

    private Optional<Long> finnOpprinneligMedlPeriodeID(Behandling behandling) {
        if (behandling.getOpprinneligBehandling() == null) {
            log.warn("opprinneligBehandling er null for behandling {}", behandling.getId());
            return Optional.empty();
        }

        var opprinnelingResultat = behandlingsresultatService.hentBehandlingsresultat(
            behandling.getOpprinneligBehandling().getId());
        return opprinnelingResultat.finnLovvalgsperiode().map(Lovvalgsperiode::getMedlPeriodeID);
    }

    private void oppdaterLovvalgsperiode(Long behandlingID, Lovvalgsperiode lovvalgsperiode) {
        if (lovvalgsperiode.erAvslått()) {
            if (lovvalgsperiode.getMedlPeriodeID() != null) {
                medlPeriodeService.avvisPeriode(lovvalgsperiode.getMedlPeriodeID());
            }
        } else if (lovvalgsperiode.erInnvilget()) {
            opprettEllerOppdaterMedlPeriode(behandlingID, lovvalgsperiode);
        } else {
            throw new FunksjonellException(
                "Ukjent eller ikke-eksisterende innvilgelsesresultat for en lovvalgsperiode: " + lovvalgsperiode.getInnvilgelsesresultat());
        }
    }

    private void opprettEllerOppdaterMedlPeriode(Long behandlingID, Lovvalgsperiode lovvalgsperiode) {
        if (lovvalgsperiode.getMedlPeriodeID() == null) {
            opprettMedlPeriode(behandlingID, lovvalgsperiode);
        } else {
            oppdaterMedlPeriode(lovvalgsperiode);
        }
    }

    private void opprettMedlPeriode(Long behandlingID, Lovvalgsperiode lovvalgsperiode) {
        if (lovvalgsperiode.erArtikkel13()) {
            medlPeriodeService.opprettPeriodeForeløpig(lovvalgsperiode, behandlingID);
        } else {
            medlPeriodeService.opprettPeriodeEndelig(lovvalgsperiode, behandlingID);
        }
    }

    private void oppdaterMedlPeriode(Lovvalgsperiode lovvalgsperiode) {
        if (lovvalgsperiode.erArtikkel13()) {
            medlPeriodeService.oppdaterPeriodeForeløpig(lovvalgsperiode);
        } else {
            medlPeriodeService.oppdaterPeriodeEndelig(lovvalgsperiode);
        }
    }
}
