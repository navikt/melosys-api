package no.nav.melosys.service.eessi.ruting;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.eessi.SedType;
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.saksflytapi.domain.ProsessDataKey;
import no.nav.melosys.saksflytapi.domain.Prosessinstans;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.sak.FagsakService;
import no.nav.melosys.service.saksflyt.ProsessinstansService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class UnntaksperiodeSedRuter implements SedRuterForSedTyper {

    private static final Logger log = LoggerFactory.getLogger(UnntaksperiodeSedRuter.class);

    private final ProsessinstansService prosessinstansService;
    private final FagsakService fagsakService;
    private final BehandlingsresultatService behandlingsresultatService;

    public UnntaksperiodeSedRuter(ProsessinstansService prosessinstansService, FagsakService fagsakService, BehandlingsresultatService behandlingsresultatService) {
        this.prosessinstansService = prosessinstansService;
        this.fagsakService = fagsakService;
        this.behandlingsresultatService = behandlingsresultatService;
    }

    @Override
    public void rutSedTilBehandling(Prosessinstans prosessinstans, Long arkivsakID) {

        MelosysEessiMelding melosysEessiMelding = prosessinstans.getData(ProsessDataKey.EESSI_MELDING, MelosysEessiMelding.class);

        if (arkivsakID == null) {
            opprettNySak(prosessinstans, melosysEessiMelding);
            return;
        }

        Optional<Fagsak> fagsak = fagsakService.finnFagsakFraArkivsakID(arkivsakID);
        if (fagsak.isPresent()) {
            Behandling behandling = fagsak.get().hentSistAktivBehandling();
            Behandlingsresultat behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandling.getId());
            if (periodeErEndret(melosysEessiMelding, behandlingsresultat)) {
                log.info("Mottatt ny {} i {} hvor periode er endret. Oppretter ny behandling",
                    melosysEessiMelding.getSedType(), fagsak.get().getSaksnummer());
                opprettNyBehandling(melosysEessiMelding, arkivsakID);
            } else {
                log.info("Mottatt ny {} i {} periode er ikke endret. Oppretter ikke ny behandling",
                    melosysEessiMelding.getSedType(), fagsak.get().getSaksnummer());
                prosessinstans.setBehandling(behandling);
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

    private void opprettNyBehandling(MelosysEessiMelding melosysEessiMelding, Long arkivSakID) {
        prosessinstansService.opprettProsessinstansNyBehandlingUnntaksregistrering(
            melosysEessiMelding,
            hentBehandlingstema(melosysEessiMelding),
            arkivSakID
        );
    }

    private void opprettNySak(Prosessinstans prosessinstans, MelosysEessiMelding melosysEessiMelding) {
        prosessinstansService.opprettProsessinstansNySakUnntaksregistrering(
            melosysEessiMelding,
            hentBehandlingstema(melosysEessiMelding),
            prosessinstans.hentAktørIDFraDataEllerSED()
        );
    }

    @Override
    public Collection<SedType> gjelderSedTyper() {
        return Set.of(SedType.A009, SedType.A010);
    }

    public Behandlingstema hentBehandlingstema(MelosysEessiMelding melosysEessiMelding) {
        SedType sedType = SedType.valueOf(melosysEessiMelding.getSedType());

        if (sedType == SedType.A009) {
            return Behandlingstema.REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING;
        } else if (sedType == SedType.A010) {
            return Behandlingstema.REGISTRERING_UNNTAK_NORSK_TRYGD_ØVRIGE;
        }

        throw new IllegalArgumentException("UnntaksperiodeMottakInitialiserer støtter ikke sedtype " + sedType);
    }
}
