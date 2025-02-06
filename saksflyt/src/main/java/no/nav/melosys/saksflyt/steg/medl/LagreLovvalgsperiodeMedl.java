package no.nav.melosys.saksflyt.steg.medl;

import java.util.Optional;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.kodeverk.Utfallregistreringunntak;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.saksflyt.steg.StegBehandler;
import no.nav.melosys.saksflytapi.domain.ProsessSteg;
import no.nav.melosys.saksflytapi.domain.Prosessinstans;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.medl.MedlPeriodeService;
import no.nav.melosys.service.saksbehandling.SaksbehandlingRegler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.kodeverk.Vilkaar.FTRL_2_12_UNNTAK_TURISTSKIP;

@Component
public class LagreLovvalgsperiodeMedl implements StegBehandler {

    private static final Logger log = LoggerFactory.getLogger(LagreLovvalgsperiodeMedl.class);

    private final BehandlingsresultatService behandlingsresultatService;
    private final MedlPeriodeService medlPeriodeService;
    private final SaksbehandlingRegler saksbehandlingRegler;


    public LagreLovvalgsperiodeMedl(BehandlingsresultatService behandlingsresultatService,
                                    MedlPeriodeService medlPeriodeService,
                                    SaksbehandlingRegler saksbehandlingRegler) {
        this.behandlingsresultatService = behandlingsresultatService;
        this.medlPeriodeService = medlPeriodeService;
        this.saksbehandlingRegler = saksbehandlingRegler;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return ProsessSteg.LAGRE_LOVVALGSPERIODE_MEDL;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) {
        final long behandlingID = prosessinstans.getBehandling().getId();
        final var behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingID);
        final var behandling = prosessinstans.getBehandling();
        final var lovvalgsperiode = behandlingsresultat.hentLovvalgsperiode();

        if (erIkkeGodkjentRegistreringUnntakFraMedlemskap(prosessinstans.getBehandling(), behandlingsresultat.getUtfallRegistreringUnntak()) ||
            erUnntakTuristSkip(behandlingsresultat) && behandling.erFørstegangsvurdering()) {
            return;
        }

        if (behandling.erNyVurdering()) {
            lovvalgsperiode.setMedlPeriodeID(finnOpprinneligMedlPeriodeID(behandling).orElse(null));
        }

        oppdaterLovvalgsperiode(behandling, lovvalgsperiode);
    }

    private boolean erUnntakTuristSkip(Behandlingsresultat behandlingsresultat) {
        return behandlingsresultat.oppfyllerVilkår(FTRL_2_12_UNNTAK_TURISTSKIP);
    }

    private boolean erIkkeGodkjentRegistreringUnntakFraMedlemskap(Behandling behandling, Utfallregistreringunntak utfallregistreringunntak) {
        return saksbehandlingRegler.harRegistreringUnntakFraMedlemskapFlyt(behandling) && Utfallregistreringunntak.IKKE_GODKJENT == utfallregistreringunntak;
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

    private void oppdaterLovvalgsperiode(Behandling behandling, Lovvalgsperiode lovvalgsperiode) {
        if (lovvalgsperiode.erAvslått()) {
            if (lovvalgsperiode.getMedlPeriodeID() != null) {
                medlPeriodeService.avvisPeriode(lovvalgsperiode.getMedlPeriodeID());
            }
        } else if (lovvalgsperiode.erInnvilget()) {
            opprettEllerOppdaterMedlPeriode(behandling, lovvalgsperiode);
        } else {
            throw new FunksjonellException(
                "Ukjent eller ikke-eksisterende innvilgelsesresultat for en lovvalgsperiode: " + lovvalgsperiode.getInnvilgelsesresultat());
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
        if (lovvalgsperiode.erArtikkel13() && !saksbehandlingRegler.harRegistreringUnntakFraMedlemskapFlyt(behandling)) {
            medlPeriodeService.opprettPeriodeForeløpig(lovvalgsperiode, behandling.getId());
        } else {
            medlPeriodeService.opprettPeriodeEndelig(lovvalgsperiode, behandling.getId());
        }
    }

    private void oppdaterMedlPeriode(Behandling behandling, Lovvalgsperiode lovvalgsperiode) {
        if (lovvalgsperiode.erArtikkel13() && !saksbehandlingRegler.harRegistreringUnntakFraMedlemskapFlyt(behandling)) {
            medlPeriodeService.oppdaterPeriodeForeløpig(lovvalgsperiode);
        } else {
            medlPeriodeService.oppdaterPeriodeEndelig(lovvalgsperiode);
        }
    }
}
