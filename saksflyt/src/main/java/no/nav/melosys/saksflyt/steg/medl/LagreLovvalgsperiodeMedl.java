package no.nav.melosys.saksflyt.steg.medl;

import java.util.Optional;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.saksflyt.steg.StegBehandler;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.medl.MedlPeriodeService;
import org.springframework.stereotype.Component;

@Component
public class LagreLovvalgsperiodeMedl implements StegBehandler {

    private final BehandlingsresultatService behandlingsresultatService;
    private final MedlPeriodeService medlPeriodeService;

    public LagreLovvalgsperiodeMedl(BehandlingsresultatService behandlingsresultatService, MedlPeriodeService medlPeriodeService) {
        this.behandlingsresultatService = behandlingsresultatService;
        this.medlPeriodeService = medlPeriodeService;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return ProsessSteg.LAGRE_LOVVALGSPERIODE_MEDL;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) {
        final Behandling behandling = prosessinstans.getBehandling();
        final long behandlingID = behandling.getId();
        final Behandlingsresultat behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingID);
        final Optional<Lovvalgsperiode> lovvalgsperiode = behandlingsresultat.finnValidertLovvalgsperiode();

        if (lovvalgsperiode.isPresent()) {
            if (behandling.erNyVurdering()) {
                erstattLovvalgsperiode(behandling, lovvalgsperiode.get());
            } else {
                oppdaterLovvalgsperiode(behandling, lovvalgsperiode.get());
            }
        } else if (!behandlingsresultat.erAvslagManglendeOpplysninger()) {
            throw new FunksjonellException("Finner ingen lovvalgsperiode for behandling " + behandlingID);
        }
    }

    private void erstattLovvalgsperiode(Behandling behandling, Lovvalgsperiode lovvalgsperiode) {
        if (lovvalgsperiode.getMedlPeriodeID() != null) {
            medlPeriodeService.avvisPeriode(lovvalgsperiode.getMedlPeriodeID());
        } else {
            throw new FunksjonellException("Finner ikke lovvalgsperiode som skal erstattes for behandling " + behandling.getId());
        }
        opprettMedlPeriode(behandling, lovvalgsperiode);
    }

    private void oppdaterLovvalgsperiode(Behandling behandling, Lovvalgsperiode lovvalgsperiode) {
        if (lovvalgsperiode.erAvslått()) {
            if (lovvalgsperiode.getMedlPeriodeID() != null) {
                medlPeriodeService.avvisPeriode(lovvalgsperiode.getMedlPeriodeID());
            }
        } else if (lovvalgsperiode.erInnvilget()) {
            opprettEllerOppdaterMedlPeriode(behandling, lovvalgsperiode);
        } else {
            throw new FunksjonellException("Ukjent eller ikke-eksisterende innvilgelsesresultat for en lovvalgsperiode: " + lovvalgsperiode.getInnvilgelsesresultat());
        }
    }

    private void opprettEllerOppdaterMedlPeriode(Behandling behandling, Lovvalgsperiode lovvalgsperiode) {
        if (lovvalgsperiode.getMedlPeriodeID() == null) {
            opprettMedlPeriode(behandling, lovvalgsperiode);
        } else {
            oppdaterMedlPeriode(behandling, lovvalgsperiode);
        }
    }

    private void opprettMedlPeriode(Behandling behandling, Lovvalgsperiode lovvalgsperiode) {
        if (lovvalgsperiode.erArtikkel13()) {
            medlPeriodeService.opprettPeriodeForeløpig(lovvalgsperiode, behandling.getId(), !behandling.erBehandlingAvSøknad());
        } else {
            medlPeriodeService.opprettPeriodeEndelig(lovvalgsperiode, behandling.getId(), !behandling.erBehandlingAvSøknad());
        }
    }

    private void oppdaterMedlPeriode(Behandling behandling, Lovvalgsperiode lovvalgsperiode) {
        if (lovvalgsperiode.erArtikkel13()) {
            medlPeriodeService.oppdaterPeriodeForeløpig(lovvalgsperiode, !behandling.erBehandlingAvSøknad());
        } else {
            medlPeriodeService.oppdaterPeriodeEndelig(lovvalgsperiode, !behandling.erBehandlingAvSøknad());
        }
    }
}
