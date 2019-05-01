package no.nav.melosys.service.dokument.brev.bygger;

import java.time.Instant;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.arkiv.Journalpost;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.joark.JoarkService;
import no.nav.melosys.service.dokument.brev.BrevData;
import no.nav.melosys.service.dokument.brev.BrevDataHenleggelse;
import no.nav.melosys.service.dokument.brev.BrevbestillingDto;

public class BrevDataByggerForsendelseMottattDato implements BrevDataBygger {

    private final BrevbestillingDto brevbestillingDto;
    private final JoarkService joarkService;

    public BrevDataByggerForsendelseMottattDato(BrevbestillingDto brevbestillingDto, JoarkService joarkService) {
        this.brevbestillingDto = brevbestillingDto;
        this.joarkService = joarkService;
    }

    @Override
    public BrevData lag(Behandling behandling, String saksbehandler) throws FunksjonellException, TekniskException {
        BrevDataHenleggelse brevData = new BrevDataHenleggelse(saksbehandler, brevbestillingDto);
        brevData.initierendeJournalpostForsendelseMottattTidspunkt = getForsendelseMottattFraJournalpost(behandling);
        return brevData;
    }

    private Instant getForsendelseMottattFraJournalpost(Behandling behandling) throws FunksjonellException, TekniskException {
        String initierendeJournalpostId = behandling.getInitierendeJournalpostId();
        Journalpost journalpost;
        try {
            journalpost = joarkService.hentJournalpost(initierendeJournalpostId);
        } catch (IntegrasjonException e) {
            throw new TekniskException(e);
        } catch (SikkerhetsbegrensningException e) {
            throw new FunksjonellException(e);
        }
        return journalpost.getForsendelseMottatt();
    }}