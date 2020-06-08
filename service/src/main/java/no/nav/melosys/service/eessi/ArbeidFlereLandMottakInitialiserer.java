package no.nav.melosys.service.eessi;

import java.util.Optional;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.eessi.SedType;
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessType;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.oppgave.OppgaveService;
import no.nav.melosys.service.sak.FagsakService;
import org.springframework.stereotype.Service;

//A003
@Service
public class ArbeidFlereLandMottakInitialiserer implements AutomatiskSedBehandlingInitialiserer {

    private final FagsakService fagsakService;
    private final BehandlingService behandlingService;
    private final BehandlingsresultatService behandlingsresultatService;
    private final OppgaveService oppgaveService;

    public ArbeidFlereLandMottakInitialiserer(FagsakService fagsakService,
                                              BehandlingService behandlingService,
                                              BehandlingsresultatService behandlingsresultatService, OppgaveService oppgaveService) {
        this.fagsakService = fagsakService;
        this.behandlingService = behandlingService;
        this.behandlingsresultatService = behandlingsresultatService;
        this.oppgaveService = oppgaveService;
    }

    @Override
    public RutingResultat finnSakOgBestemRuting(Prosessinstans prosessinstans, Long gsakSaksnummer) throws TekniskException, FunksjonellException {

        if (gsakSaksnummer == null) {
            return RutingResultat.NY_SAK;
        }

        Optional<Fagsak> fagsak = fagsakService.finnFagsakFraGsakSaksnummer(gsakSaksnummer);

        if (fagsak.isEmpty()) {
            throw new FunksjonellException("Finner ingen sak tilknyttet gsaksaksnummer " + gsakSaksnummer);
        }

        final MelosysEessiMelding melosysEessiMelding = prosessinstans.getData(ProsessDataKey.EESSI_MELDING, MelosysEessiMelding.class);
        final Behandling eksisterendeBehandling = fagsak.get().hentSistAktiveBehandling();
        final Behandlingsresultat behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(eksisterendeBehandling.getId());
        final Behandlingstema nyttBehandlingstema = hentBehandlingstema(melosysEessiMelding);

        if (eksisterendeBehandling.getTema() != nyttBehandlingstema) {
            return RutingResultat.NY_BEHANDLING;
        } else if (nyttBehandlingstema == Behandlingstema.BESLUTNING_LOVVALG_ANNET_LAND && periodeErEndret(melosysEessiMelding, behandlingsresultat)) {
            return RutingResultat.NY_BEHANDLING;
        } else if (nyttBehandlingstema == Behandlingstema.BESLUTNING_LOVVALG_NORGE) {
            if (eksisterendeBehandling.erAktiv()) {
                behandlingService.oppdaterStatus(eksisterendeBehandling.getId(), Behandlingsstatus.VURDER_DOKUMENT);
            } else {
                oppgaveService.opprettEllerGjenbrukBehandlingsoppgave(
                    eksisterendeBehandling,
                    prosessinstans.getData(ProsessDataKey.JOURNALPOST_ID),
                    prosessinstans.getData(ProsessDataKey.AKTØR_ID),
                    prosessinstans.hentSaksbehandlerHvisTilordnes()
                );
            }
        }

        return RutingResultat.INGEN_BEHANDLING;
    }

    @Override
    public boolean gjelderSedType(SedType sedType) {
        return sedType == SedType.A003;
    }

    @Override
    public Behandlingstema hentBehandlingstema(MelosysEessiMelding melosysEessiMelding) {
        return Landkoder.NO.getKode().equals(melosysEessiMelding.getLovvalgsland())
            ? Behandlingstema.BESLUTNING_LOVVALG_NORGE
            : Behandlingstema.BESLUTNING_LOVVALG_ANNET_LAND;
    }

    @Override
    public ProsessType hentAktuellProsessType() {
        return ProsessType.ARBEID_FLERE_LAND;
    }
}
