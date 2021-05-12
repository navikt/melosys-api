package no.nav.melosys.service.eessi.ruting;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.eessi.SedType;
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.sak.FagsakService;
import no.nav.melosys.service.saksflyt.ProsessinstansService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

//A001
@Service
public class AnmodningOmUnntakSedRuter implements SedRuterForSedTyper {

    private static final Logger log = LoggerFactory.getLogger(SedRuterForSedTyper.class);

    private final ProsessinstansService prosessinstansService;
    private final FagsakService fagsakService;
    private final BehandlingsresultatService behandlingsresultatService;

    @Autowired
    public AnmodningOmUnntakSedRuter(ProsessinstansService prosessinstansService, FagsakService fagsakService, BehandlingsresultatService behandlingsresultatService) {
        this.prosessinstansService = prosessinstansService;
        this.fagsakService = fagsakService;
        this.behandlingsresultatService = behandlingsresultatService;
    }

    @Override
    public void rutSedTilBehandling(Prosessinstans prosessinstans, Long arkivsakID) throws FunksjonellException {
        final MelosysEessiMelding melosysEessiMelding = prosessinstans.getData(ProsessDataKey.EESSI_MELDING, MelosysEessiMelding.class);
        if (arkivsakID == null) {
            opprettNySak(prosessinstans, melosysEessiMelding);
            return;
        }

        Optional<Fagsak> fagsak = fagsakService.finnFagsakFraArkivsakID(arkivsakID);
        if (fagsak.isPresent()) {
            Behandling behandling = fagsak.get().getSistOppdaterteBehandling();
            prosessinstans.setBehandling(behandling);
            Behandlingsresultat behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandling.getId());
            if (periodeErEndret(melosysEessiMelding, behandlingsresultat)) {
                log.info("Mottatt ny A001 i {} hvor periode er endret. Oppretter ny behandling", fagsak.get().getSaksnummer());
                opprettNyBehandling(melosysEessiMelding, arkivsakID);
            } else {
                log.info("Mottatt ny A001 i {}, periode er ikke endret. Oppretter ikke ny behandling", fagsak.get().getSaksnummer());
                opprettJournalføringProsess(melosysEessiMelding, behandling);
            }
        } else {
            opprettNySak(prosessinstans, melosysEessiMelding);
        }
    }

    private void opprettJournalføringProsess(MelosysEessiMelding melosysEessiMelding, Behandling sistAktiveBehandling) {
        prosessinstansService.opprettProsessinstansSedJournalføring(
            sistAktiveBehandling,
            melosysEessiMelding
        );
    }

    private void opprettNySak(Prosessinstans prosessinstans, MelosysEessiMelding melosysEessiMelding) {
        prosessinstansService.opprettProsessinstansNySakMottattAnmodningOmUnntak(
            melosysEessiMelding,
            prosessinstans.hentAktørIDFraDataEllerSED()
        );
    }

    private void opprettNyBehandling(MelosysEessiMelding melosysEessiMelding, Long arkivsakID) {
        prosessinstansService.opprettProsessinstansNyBehandlingMottattAnmodningUnntak(
            melosysEessiMelding,
            arkivsakID
        );
    }

    @Override
    public Collection<SedType> gjelderSedTyper() {
        return Collections.singleton(SedType.A001);
    }
}
