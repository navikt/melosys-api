package no.nav.melosys.service.dokument.brev.bygger;

import java.time.Instant;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.arkiv.Journalpost;
import no.nav.melosys.integrasjon.joark.JoarkService;
import no.nav.melosys.service.dokument.brev.BrevData;
import no.nav.melosys.service.dokument.brev.BrevDataMottattDato;
import no.nav.melosys.service.dokument.brev.BrevbestillingDto;
import no.nav.melosys.service.dokument.brev.datagrunnlag.BrevDataGrunnlag;

public class BrevDataByggerMedMottattDato implements BrevDataBygger {
    private final BrevbestillingDto brevbestillingDto;
    private final JoarkService joarkService;

    public BrevDataByggerMedMottattDato(BrevbestillingDto brevbestillingDto, JoarkService joarkService) {
        this.brevbestillingDto = brevbestillingDto;
        this.joarkService = joarkService;
    }

    @Override
    public BrevData lag(BrevDataGrunnlag dataGrunnlag, String saksbehandler) {
        BrevDataMottattDato brevData = new BrevDataMottattDato(saksbehandler, brevbestillingDto);
        brevData.initierendeJournalpostForsendelseMottattTidspunkt = getForsendelseMottattFraJournalpost(dataGrunnlag.getBehandling());
        return brevData;
    }

    private Instant getForsendelseMottattFraJournalpost(Behandling behandling) {
        String initierendeJournalpostId = behandling.getInitierendeJournalpostId();
        Journalpost journalpost = joarkService.hentJournalpost(initierendeJournalpostId);
        return journalpost.getForsendelseMottatt();
    }
}
