package no.nav.melosys.service.eessi.ruting;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import no.nav.melosys.domain.Anmodningsperiode;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.eessi.SedType;
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.integrasjon.oppgave.OppgaveOppdatering;
import no.nav.melosys.saksflytapi.ProsessinstansService;
import no.nav.melosys.saksflytapi.domain.ProsessDataKey;
import no.nav.melosys.saksflytapi.domain.Prosessinstans;
import no.nav.melosys.service.oppgave.OppgaveService;
import no.nav.melosys.service.sak.FagsakService;
import no.nav.melosys.service.unntak.AnmodningsperiodeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import static no.nav.melosys.integrasjon.oppgave.OppgaveFasadeImpl.hentNyBeskrivelseHendelseslogg;

@Service
public class SvarAnmodningUnntakSedRuter implements SedRuterForSedTyper {

    private static final Logger log = LoggerFactory.getLogger(SvarAnmodningUnntakSedRuter.class);
    private static final String MOTTATT_SED_BESKRIVELSE = "Mottatt svar på A001: SED %s";

    private final ProsessinstansService prosessinstansService;
    private final FagsakService fagsakService;
    private final AnmodningsperiodeService anmodningsperiodeService;
    private final OppgaveService oppgaveService;

    public SvarAnmodningUnntakSedRuter(ProsessinstansService prosessinstansService, FagsakService fagsakService,
                                       AnmodningsperiodeService anmodningsperiodeService,
                                       OppgaveService oppgaveService) {
        this.prosessinstansService = prosessinstansService;
        this.fagsakService = fagsakService;
        this.anmodningsperiodeService = anmodningsperiodeService;
        this.oppgaveService = oppgaveService;
    }

    @Override
    public void rutSedTilBehandling(Prosessinstans prosessinstans, Long arkivsakID) {
        MelosysEessiMelding melosysEessiMelding = prosessinstans.getData(ProsessDataKey.EESSI_MELDING, MelosysEessiMelding.class);

        if (arkivsakID == null) {
            oppgaveService.opprettJournalføringsoppgave(melosysEessiMelding.getJournalpostId(), melosysEessiMelding.getAktoerId());
            return;
        }

        Behandling behandling = hentBehandling(arkivsakID);
        prosessinstans.setBehandling(behandling);
        Optional<Anmodningsperiode> anmodningsperiode = anmodningsperiodeService.hentAnmodningsperioder(behandling.getId())
            .stream().findFirst();

        if (behandling.erUtsending() && anmodningsperiode.isEmpty()) {
            throw new FunksjonellException(String.format(
                "Mottatt SED %s på buctype %s - finner behandling %s for rinasak %s, men behandlingen har ingen anmodningsperiode",
                melosysEessiMelding.getSedType(), melosysEessiMelding.getBucType(), behandling.getId(), melosysEessiMelding.getRinaSaksnummer()));
        } else if (behandling.getTema() == Behandlingstema.IKKE_YRKESAKTIV) {
            oppdaterBehandlingOgOppgave(behandling, melosysEessiMelding.getSedType());
            opprettJournalføringProsess(melosysEessiMelding, behandling);
        } else if (anmodningsperiode.map(Anmodningsperiode::getAnmodningsperiodeSvar).isPresent()) {
            opprettJournalføringProsess(melosysEessiMelding, behandling);
        } else {
            prosessinstansService.opprettProsessinstansMottattSvarAnmodningUnntak(behandling, melosysEessiMelding);
        }
    }

    private void opprettJournalføringProsess(MelosysEessiMelding melosysEessiMelding, Behandling sistAktiveBehandling) {
        prosessinstansService.opprettProsessinstansSedJournalføring(
            sistAktiveBehandling,
            melosysEessiMelding
        );
    }

    private Behandling hentBehandling(Long gsakSaksnummer) {
        return fagsakService
            .hentFagsakFraArkivsakID(gsakSaksnummer)
            .hentSistAktivBehandlingIkkeÅrsavregning();
    }

    private void oppdaterBehandlingOgOppgave(Behandling behandling, String sedType) {
        behandling.setStatus(Behandlingsstatus.VURDER_DOKUMENT);
        Optional<Oppgave> oppgave = oppgaveService.finnÅpenBehandlingsoppgaveMedFagsaksnummer(behandling.getFagsak().getSaksnummer());
        if (oppgave.isEmpty()) {
            opprettOppgave(behandling, sedType);
        } else {
            oppgaveService.oppdaterOppgave(oppgave.get().getOppgaveId(),
                OppgaveOppdatering.builder().beskrivelse(lagMottattSedBeskrivelse(sedType)).build());
        }
    }

    private void opprettOppgave(Behandling behandling, String sedType) {
        String aktørID = behandling.getFagsak().hentBrukersAktørID();
        String saksnummer = behandling.getFagsak().getSaksnummer();

        Oppgave.Builder oppgaveBuilder = oppgaveService.lagBehandlingsoppgave(behandling)
            .setAktørId(aktørID)
            .setJournalpostId(behandling.getInitierendeJournalpostId())
            .setSaksnummer(saksnummer)
            .setBeskrivelse(hentNyBeskrivelseHendelseslogg(lagMottattSedBeskrivelse(sedType), saksnummer));

        String oppgaveID = oppgaveService.opprettOppgave(oppgaveBuilder.build());
        log.info("Opprettet behandlingsoppgave med id {} for behandling {}", oppgaveID, behandling.getId());
    }

    private String lagMottattSedBeskrivelse(String sedType) {
        return String.format(MOTTATT_SED_BESKRIVELSE, sedType);
    }

    @Override
    public Collection<SedType> gjelderSedTyper() {
        return Set.of(SedType.A011, SedType.A002);
    }
}
