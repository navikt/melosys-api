package no.nav.melosys.saksflyt.steg.behandling;

import java.time.LocalDate;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsaarsaktyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.saksflyt.steg.StegBehandler;
import no.nav.melosys.saksflytapi.domain.ProsessSteg;
import no.nav.melosys.saksflytapi.domain.Prosessinstans;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.sak.FagsakService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import static no.nav.melosys.saksflytapi.domain.ProsessDataKey.*;
import static no.nav.melosys.saksflytapi.domain.ProsessSteg.OPPRETT_NY_BEHANDLING;


@Component
public class OpprettNyBehandling implements StegBehandler {
    private static final Logger log = LoggerFactory.getLogger(OpprettNyBehandling.class);

    private final FagsakService fagsakService;
    private final BehandlingService behandlingService;

    public OpprettNyBehandling(FagsakService fagsakService, BehandlingService behandlingService) {
        this.fagsakService = fagsakService;
        this.behandlingService = behandlingService;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return OPPRETT_NY_BEHANDLING;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) {
        String saksnummer = prosessinstans.getData(SAKSNUMMER);
        String initierendeJournalpostId = prosessinstans.getData(JOURNALPOST_ID);
        String initierendeDokumentId = prosessinstans.getData(DOKUMENT_ID);
        Behandlingsaarsaktyper behandlingsårsaktype = prosessinstans.getData(BEHANDLINGSÅRSAKTYPE, Behandlingsaarsaktyper.class);
        String behandlingsårsakFritekst = prosessinstans.getData(BEHANDLINGSÅRSAK_FRITEKST);
        Behandlingstyper behandlingstype = prosessinstans.getData(BEHANDLINGSTYPE, Behandlingstyper.class);
        Behandlingstema behandlingstema = prosessinstans.getData(BEHANDLINGSTEMA, Behandlingstema.class);
        LocalDate mottaksdato = prosessinstans.getData(MOTTATT_DATO, LocalDate.class);

        Fagsak fagsak = fagsakService.hentFagsak(saksnummer);
        Behandling behandling = behandlingService.nyBehandling(fagsak,
            Behandlingsstatus.OPPRETTET,
            behandlingstype,
            behandlingstema,
            initierendeJournalpostId,
            initierendeDokumentId,
            mottaksdato,
            behandlingsårsaktype,
            behandlingsårsakFritekst);
        prosessinstans.setBehandling(behandling);
        log.info("Opprettet ny behandling {} på eksiterende fagsak {}", behandling.getId(), fagsak.getSaksnummer());
    }
}
