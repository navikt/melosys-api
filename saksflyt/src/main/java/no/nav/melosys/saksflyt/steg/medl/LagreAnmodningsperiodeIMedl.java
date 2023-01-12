package no.nav.melosys.saksflyt.steg.medl;

import java.util.Optional;

import no.nav.melosys.domain.Anmodningsperiode;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.saksflyt.steg.StegBehandler;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.kontroll.regler.PeriodeRegler;
import no.nav.melosys.service.medl.MedlPeriodeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper.ANMODNING_OM_UNNTAK;
import static no.nav.melosys.domain.saksflyt.ProsessSteg.LAGRE_ANMODNINGSPERIODE_MEDL;

@Component
public class LagreAnmodningsperiodeIMedl implements StegBehandler {

    private static final Logger log = LoggerFactory.getLogger(LagreAnmodningsperiodeIMedl.class);

    private final BehandlingsresultatService behandlingsresultatService;
    private final MedlPeriodeService medlPeriodeService;

    public LagreAnmodningsperiodeIMedl(BehandlingsresultatService behandlingsresultatService, MedlPeriodeService medlPeriodeService) {
        this.behandlingsresultatService = behandlingsresultatService;
        this.medlPeriodeService = medlPeriodeService;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return LAGRE_ANMODNINGSPERIODE_MEDL;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) {
        final Behandling behandling = prosessinstans.getBehandling();
        final long behandlingID = behandling.getId();
        Anmodningsperiode anmodningsperiode = behandlingsresultatService.hentBehandlingsresultat(behandlingID).hentAnmodningsperiode();
        if (PeriodeRegler.feilIPeriode(anmodningsperiode.getFom(), anmodningsperiode.getTom())) {
            log.info("Lagrer ikke anmodningsperiode i MEDL pga ulogisk periode. BehID={}", behandlingID);
            return;
        }
        if (behandling.erNyVurdering() && !behandling.erBehandlingAvSed()) {
            anmodningsperiode.setMedlPeriodeID(finnOpprinneligMedlPeriodeID(behandling).orElse(null));
        }
        opprettEllerOppdaterMedlPeriode(behandling, anmodningsperiode);
    }

    private void opprettEllerOppdaterMedlPeriode(Behandling behandling, Anmodningsperiode anmodningsperiode) {
        if (anmodningsperiode.getMedlPeriodeID() == null) {
            medlPeriodeService.opprettPeriodeUnderAvklaring(anmodningsperiode, behandling.getId(), behandling.erBehandlingAvSed());
        } else {
            medlPeriodeService.oppdaterPeriodeUnderAvklaring(anmodningsperiode, behandling.erBehandlingAvSed());
        }
    }

    private Optional<Long> finnOpprinneligMedlPeriodeID(Behandling nyBehandling) {
        Fagsak fagsak = nyBehandling.getFagsak();
        Optional<Behandling> forrigeBehandling = fagsak.hentBehandlingerSortertSynkendePåRegistrertDato().stream()
            .filter(behandling -> !behandling.getId().equals(nyBehandling.getId()))
            .filter(behandling -> {
                Behandlingsresultat behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandling.getId());
                return ANMODNING_OM_UNNTAK == behandlingsresultat.getType();
            })
            .findFirst();

        if (forrigeBehandling.isEmpty()) {
            return Optional.empty();
        } else {
            Behandlingsresultat opprinneligResultat = behandlingsresultatService.hentBehandlingsresultat(forrigeBehandling.get().getId());
            return opprinneligResultat.finnAnmodningsperiode().map(Anmodningsperiode::getMedlPeriodeID);
        }
    }
}
