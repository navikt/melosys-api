package no.nav.melosys.saksflyt.steg.medl;

import java.util.Optional;

import no.finn.unleash.Unleash;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.saksflyt.steg.StegBehandler;
import no.nav.melosys.saksflyt.steg.jfr.FerdigstillJournalpostSed;
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
    private final Unleash unleash;


    public LagreLovvalgsperiodeMedl(BehandlingsresultatService behandlingsresultatService,
                                    MedlPeriodeService medlPeriodeService,
                                    Unleash unleash) {
        this.behandlingsresultatService = behandlingsresultatService;
        this.medlPeriodeService = medlPeriodeService;
        this.unleash = unleash;
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
        oppdaterLovvalgsperiode(behandling, lovvalgsperiode);
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
        if (lovvalgsperiode.erArtikkel13()) {
            medlPeriodeService.opprettPeriodeForeløpig(lovvalgsperiode, behandling.getId(),
                unleash.isEnabled("melosys.behandle_alle_saker") ? behandling.erBehandlingAvSed() : !behandling.erBehandlingAvSøknadGammel());
        } else {
            medlPeriodeService.opprettPeriodeEndelig(lovvalgsperiode, behandling.getId(),
                unleash.isEnabled("melosys.behandle_alle_saker") ? behandling.erBehandlingAvSed() : !behandling.erBehandlingAvSøknadGammel());
        }
    }

    private void oppdaterMedlPeriode(Behandling behandling, Lovvalgsperiode lovvalgsperiode) {
        if (lovvalgsperiode.erArtikkel13()) {
            medlPeriodeService.oppdaterPeriodeForeløpig(lovvalgsperiode,
                unleash.isEnabled("melosys.behandle_alle_saker") ? behandling.erBehandlingAvSed() : !behandling.erBehandlingAvSøknadGammel());
        } else {
            medlPeriodeService.oppdaterPeriodeEndelig(lovvalgsperiode,
                unleash.isEnabled("melosys.behandle_alle_saker") ? behandling.erBehandlingAvSed() : !behandling.erBehandlingAvSøknadGammel());
        }
    }
}
