package no.nav.melosys.service.dokument.brev.bygger;

import java.time.Instant;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.arkiv.Journalpost;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.integrasjon.joark.JoarkService;
import no.nav.melosys.service.dokument.brev.BrevData;
import no.nav.melosys.service.dokument.brev.BrevDataMottattDato;
import no.nav.melosys.service.dokument.brev.BrevbestillingDto;

public class BrevDataByggerMedMottattDato implements BrevDataBygger {

    private final Behandling behandling;
    private final BrevbestillingDto brevbestillingDto;
    private final JoarkService joarkService;

    public BrevDataByggerMedMottattDato(Behandling behandling, BrevbestillingDto brevbestillingDto, JoarkService joarkService) {
        this.behandling = behandling;
        this.brevbestillingDto = brevbestillingDto;
        this.joarkService = joarkService;
    }

    @Override
    public BrevData lag(String saksbehandler) throws SikkerhetsbegrensningException, IntegrasjonException {
        BrevDataMottattDato brevData = new BrevDataMottattDato(saksbehandler, brevbestillingDto);
        brevData.initierendeJournalpostForsendelseMottattTidspunkt = getForsendelseMottattFraJournalpost();
        return brevData;
    }

    private Instant getForsendelseMottattFraJournalpost() throws SikkerhetsbegrensningException, IntegrasjonException {
        String initierendeJournalpostId = behandling.getInitierendeJournalpostId();
        Journalpost journalpost = joarkService.hentJournalpost(initierendeJournalpostId);
        return journalpost.getForsendelseMottatt();
    }
}