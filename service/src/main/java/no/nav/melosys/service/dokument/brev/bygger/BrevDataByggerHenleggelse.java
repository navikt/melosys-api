package no.nav.melosys.service.dokument.brev.bygger;

import java.time.Instant;
import java.util.Comparator;
import java.util.Optional;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.RegistreringsInfo;
import no.nav.melosys.domain.arkiv.Journalpost;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.integrasjon.joark.JoarkService;
import no.nav.melosys.service.dokument.brev.BrevData;
import no.nav.melosys.service.dokument.brev.BrevDataMottattDato;
import no.nav.melosys.service.dokument.brev.BrevbestillingDto;

public class BrevDataByggerHenleggelse implements BrevDataBygger {
    private final Behandling behandling;
    private final JoarkService joarkService;
    private BrevbestillingDto brevbestillingDto;

    public BrevDataByggerHenleggelse(Behandling behandling, JoarkService joarkService, BrevbestillingDto brevbestillingDto) {
        this.behandling = behandling;
        this.joarkService = joarkService;
        this.brevbestillingDto = brevbestillingDto;
    }

    @Override
    public BrevData lag(String saksbehandler) throws FunksjonellException, IntegrasjonException {
        Instant forsendelseMottattTidspunkt = hentInitierendeJournalpostMottattTidspunktFraFørsteBehandling(behandling);

        BrevDataMottattDato brevData = new BrevDataMottattDato(saksbehandler, brevbestillingDto);
        brevData.initierendeJournalpostForsendelseMottattTidspunkt = forsendelseMottattTidspunkt;
        return brevData;
    }

    private Instant hentInitierendeJournalpostMottattTidspunktFraFørsteBehandling(Behandling behandling) throws FunksjonellException, IntegrasjonException {
        final Fagsak fagsak = behandling.getFagsak();
        String initierendeJournalpostId = fagsak.getBehandlinger()
            .stream()
            .min(Comparator.comparing(RegistreringsInfo::getRegistrertDato))
            .map(Behandling::getInitierendeJournalpostId)
            .orElseThrow(() -> new IkkeFunnetException("Initierende behandling for " + fagsak.getSaksnummer() + " har ingen initierendeJournalpostId"));

        Journalpost journalpost = Optional.of(joarkService.hentJournalpost(initierendeJournalpostId))
            .orElseThrow(() -> new IkkeFunnetException("Journalpost " + initierendeJournalpostId + " finnes ikke."));
        return journalpost.getForsendelseMottatt();
    }
}
