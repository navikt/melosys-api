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
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.oppgave.OppgaveService;
import no.nav.melosys.service.sak.FagsakService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

//A003
@Service
public class ArbeidFlereLandMottakInitialiserer implements AutomatiskSedBehandlingInitialiserer {

    private static final Logger log = LoggerFactory.getLogger(ArbeidFlereLandMottakInitialiserer.class);

    private final FagsakService fagsakService;
    private final BehandlingService behandlingService;
    private final BehandlingsresultatService behandlingsresultatService;
    private final OppgaveService oppgaveService;

    public ArbeidFlereLandMottakInitialiserer(FagsakService fagsakService,
                                              BehandlingService behandlingService,
                                              BehandlingsresultatService behandlingsresultatService,
                                              @Qualifier("system") OppgaveService oppgaveService) {
        this.fagsakService = fagsakService;
        this.behandlingService = behandlingService;
        this.behandlingsresultatService = behandlingsresultatService;
        this.oppgaveService = oppgaveService;
    }

    @Override
    public RutingResultat finnSakOgBestemRuting(Prosessinstans prosessinstans, Long gsakSaksnummer) throws MelosysException {

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

            validerNorgeIkkeUtpektOgVedtakIkkeFattet(eksisterendeBehandling, behandlingsresultat);
            log.info("Ny A003 resulterer i nytt behandlingstema {}", nyttBehandlingstema);
            return RutingResultat.NY_BEHANDLING;

        } else if (eksisterendeBehandling.erBeslutningLovvalgAnnetLand() && periodeErEndret(melosysEessiMelding, behandlingsresultat)) {

            log.info("Mottatt oppdatert A003 i {}, rinasak {} hvor et annet land er utpekt",
                fagsak.get().getSaksnummer(), melosysEessiMelding.getRinaSaksnummer());
            return RutingResultat.NY_BEHANDLING;

        } else if (eksisterendeBehandling.erNorgeUtpekt()) {

            if (eksisterendeBehandling.erAktiv()) {
                log.info("Mottatt oppdatert A003 norge utpekt sak {}, oppdaterer status til {}",
                    fagsak.get().getSaksnummer(), Behandlingsstatus.VURDER_DOKUMENT);
                behandlingService.oppdaterStatus(eksisterendeBehandling.getId(), Behandlingsstatus.VURDER_DOKUMENT);
            } else {
                log.info("Mottatt oppdatert A003 norge utpekt sak {}. Behandling er avsluttet, oppretter oppgave", fagsak.get().getSaksnummer());
                oppgaveService.opprettEllerGjenbrukBehandlingsoppgave(
                    eksisterendeBehandling,
                    prosessinstans.getData(ProsessDataKey.JOURNALPOST_ID),
                    prosessinstans.getData(ProsessDataKey.AKTØR_ID),
                    prosessinstans.hentSaksbehandlerHvisTilordnes()
                );
            }
        }

        prosessinstans.setBehandling(eksisterendeBehandling);
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

    private void validerNorgeIkkeUtpektOgVedtakIkkeFattet(Behandling behandling, Behandlingsresultat behandlingsresultat) throws FunksjonellException {
        if (behandling.erNorgeUtpekt() && behandlingsresultat.harVedtak()) {
            throw new FunksjonellException(String.format(
                    "Det er allerede fattet vedtak på behandling %s med tema %s. Støtte for omgjøring ikke implementert",
                    behandling.getId(), behandling.getTema()));
        }
    }
}
