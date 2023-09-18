package no.nav.melosys.service.eessi.ruting;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

import io.getunleash.Unleash;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.eessi.SedType;
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Sakstemaer;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.integrasjon.oppgave.OppgaveOppdatering;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.oppgave.OppgaveService;
import no.nav.melosys.service.sak.FagsakService;
import no.nav.melosys.service.saksflyt.ProsessinstansService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

//A003
@Service
public class ArbeidFlereLandSedRuter implements SedRuterForSedTyper {

    private static final Logger log = LoggerFactory.getLogger(ArbeidFlereLandSedRuter.class);

    private final ProsessinstansService prosessinstansService;
    private final FagsakService fagsakService;
    private final BehandlingService behandlingService;
    private final BehandlingsresultatService behandlingsresultatService;
    private final OppgaveService oppgaveService;

    public ArbeidFlereLandSedRuter(ProsessinstansService prosessinstansService, FagsakService fagsakService,
                                   BehandlingService behandlingService,
                                   BehandlingsresultatService behandlingsresultatService,
                                   OppgaveService oppgaveService) {
        this.prosessinstansService = prosessinstansService;
        this.fagsakService = fagsakService;
        this.behandlingService = behandlingService;
        this.behandlingsresultatService = behandlingsresultatService;
        this.oppgaveService = oppgaveService;
    }

    @Override
    public void rutSedTilBehandling(Prosessinstans prosessinstans, Long arkivsakID) {

        final MelosysEessiMelding melosysEessiMelding = prosessinstans.getData(ProsessDataKey.EESSI_MELDING, MelosysEessiMelding.class);

        if (arkivsakID == null) {
            opprettNySak(prosessinstans, melosysEessiMelding);
            return;
        }

        Optional<Fagsak> fagsak = fagsakService.finnFagsakFraArkivsakID(arkivsakID);

        if (fagsak.isEmpty()) {
            throw new FunksjonellException("Finner ingen sak tilknyttet arkivsak " + arkivsakID);
        }

        final Behandling eksisterendeBehandling = fagsak.get().hentSistAktivBehandling();
        final Behandlingsresultat behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(eksisterendeBehandling.getId());
        final Behandlingstema nyttBehandlingstema = hentBehandlingstema(melosysEessiMelding);

        if (eksisterendeBehandling.getTema() != nyttBehandlingstema) {
            validerLovligeKombinasjoner(nyttBehandlingstema, eksisterendeBehandling.getFagsak());
            validerNorgeIkkeUtpektOgVedtakIkkeFattet(eksisterendeBehandling, behandlingsresultat);
            log.info("Ny A003 resulterer i nytt behandlingstema {}", nyttBehandlingstema);
            opprettNyBehandling(melosysEessiMelding, arkivsakID);
        } else if (eksisterendeBehandling.erBeslutningLovvalgAnnetLand() && periodeErEndret(melosysEessiMelding, behandlingsresultat)) {
            log.info("Mottatt oppdatert A003 i {}, rinasak {} hvor et annet land er utpekt",
                fagsak.get().getSaksnummer(), melosysEessiMelding.getRinaSaksnummer());
            opprettNyBehandling(melosysEessiMelding, arkivsakID);
        } else if (eksisterendeBehandling.erNorgeUtpekt()) {

            if (eksisterendeBehandling.erAktiv()) {
                log.info("Mottatt oppdatert A003 norge utpekt sak {}, oppdaterer status til {}",
                    fagsak.get().getSaksnummer(), Behandlingsstatus.VURDER_DOKUMENT);
                oppgaveService.finnÅpenBehandlingsoppgaveMedFagsaksnummer(fagsak.get().getSaksnummer())
                    .ifPresent(o -> oppgaveService.oppdaterOppgave(o.getOppgaveId(), OppgaveOppdatering.builder().beskrivelse("Mottatt SED " + SedType.A003).build()));
                behandlingService.endreStatus(eksisterendeBehandling.getId(), Behandlingsstatus.VURDER_DOKUMENT);
            } else {
                log.info("Mottatt oppdatert A003 norge utpekt sak {}. Behandling er avsluttet, oppretter oppgave", fagsak.get().getSaksnummer());
                oppgaveService.opprettEllerGjenbrukBehandlingsoppgave(
                    eksisterendeBehandling,
                    prosessinstans.getData(ProsessDataKey.JOURNALPOST_ID),
                    prosessinstans.getData(ProsessDataKey.AKTØR_ID),
                    prosessinstans.hentSaksbehandlerHvisTilordnes()
                );
            }

            opprettJournalføringProsess(melosysEessiMelding, eksisterendeBehandling);
        } else {
            opprettJournalføringProsess(melosysEessiMelding, eksisterendeBehandling);
        }
    }

    private void opprettNySak(Prosessinstans prosessinstans, MelosysEessiMelding melosysEessiMelding) {
        prosessinstansService.opprettProsessinstansNySakArbeidFlereLand(
            melosysEessiMelding, hentSakstema(melosysEessiMelding), hentBehandlingstema(melosysEessiMelding),
            prosessinstans.hentAktørIDFraDataEllerSED()
        );
    }

    private void opprettNyBehandling(MelosysEessiMelding melosysEessiMelding, Long arkivSakID) {
        prosessinstansService.opprettProsessinstansNyBehandlingArbeidFlereLand(
            melosysEessiMelding,
            hentBehandlingstema(melosysEessiMelding),
            arkivSakID
        );
    }

    private void opprettJournalføringProsess(MelosysEessiMelding melosysEessiMelding, Behandling sistAktiveBehandling) {
        prosessinstansService.opprettProsessinstansSedJournalføring(
            sistAktiveBehandling,
            melosysEessiMelding
        );
    }

    @Override
    public Collection<SedType> gjelderSedTyper() {
        return Collections.singleton(SedType.A003);
    }

    private Sakstemaer hentSakstema(MelosysEessiMelding melosysEessiMelding) {
        return Landkoder.NO.getKode().equals(melosysEessiMelding.getLovvalgsland())
            ? Sakstemaer.MEDLEMSKAP_LOVVALG
            : Sakstemaer.UNNTAK;
    }

    public Behandlingstema hentBehandlingstema(MelosysEessiMelding melosysEessiMelding) {
        return Landkoder.NO.getKode().equals(melosysEessiMelding.getLovvalgsland())
            ? Behandlingstema.BESLUTNING_LOVVALG_NORGE
            : Behandlingstema.BESLUTNING_LOVVALG_ANNET_LAND;
    }

    private void validerNorgeIkkeUtpektOgVedtakIkkeFattet(Behandling behandling, Behandlingsresultat behandlingsresultat) {
        if (behandling.erNorgeUtpekt() && behandlingsresultat.harVedtak()) {
            throw new FunksjonellException(String.format(
                    "Det er allerede fattet vedtak på behandling %s med tema %s. Støtte for omgjøring ikke implementert",
                    behandling.getId(), behandling.getTema()));
        }
    }

    private void validerLovligeKombinasjoner(Behandlingstema nyttBehandlingsTema, Fagsak fagsak) {
        if (fagsak.getTema() != null) {
            if (nyttBehandlingsTema.equals(Behandlingstema.BESLUTNING_LOVVALG_NORGE) && fagsak.getTema().equals(Sakstemaer.UNNTAK)) {
                fagsakService.oppdaterSakstema(fagsak, Sakstemaer.MEDLEMSKAP_LOVVALG);
            } else if (nyttBehandlingsTema.equals(Behandlingstema.BESLUTNING_LOVVALG_ANNET_LAND) && fagsak.getTema().equals(Sakstemaer.MEDLEMSKAP_LOVVALG)) {
                fagsakService.oppdaterSakstema(fagsak, Sakstemaer.UNNTAK);
            }
        }
    }
}
