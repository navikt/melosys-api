package no.nav.melosys.service.dokument.brev.bygger;

import java.time.Instant;
import java.util.Comparator;
import java.util.Optional;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.RegistreringsInfo;
import no.nav.melosys.domain.arkiv.Journalpost;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.integrasjon.joark.JoarkService;
import no.nav.melosys.service.dokument.brev.BrevData;
import no.nav.melosys.service.dokument.brev.BrevDataMottattDato;
import no.nav.melosys.service.dokument.brev.BrevbestillingRequest;
import no.nav.melosys.service.dokument.brev.datagrunnlag.BrevDataGrunnlag;

public class BrevDataByggerHenleggelse implements BrevDataBygger {
    private final JoarkService joarkService;
    private BrevbestillingRequest brevbestillingRequest;

    public BrevDataByggerHenleggelse(JoarkService joarkService, BrevbestillingRequest brevbestillingRequest) {
        this.joarkService = joarkService;
        this.brevbestillingRequest = brevbestillingRequest;
    }

    @Override
    public BrevData lag(BrevDataGrunnlag dataGrunnlag, String saksbehandler) {
        Instant forsendelseMottattTidspunkt = hentInitierendeJournalpostMottattTidspunktFraFørsteBehandling(dataGrunnlag.getBehandling());

        BrevDataMottattDato brevData = new BrevDataMottattDato(saksbehandler, brevbestillingRequest);
        brevData.initierendeJournalpostForsendelseMottattTidspunkt = forsendelseMottattTidspunkt;
        return brevData;
    }

    private Instant hentInitierendeJournalpostMottattTidspunktFraFørsteBehandling(Behandling behandling) {
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
