package no.nav.melosys.service.dokument;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.brev.DoksysBrevbestilling;
import no.nav.melosys.domain.brev.Mottaker;
import no.nav.melosys.domain.dokument.DokumentBestiltEvent;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.dokument.brev.BrevbestillingDto;
import no.nav.melosys.sikkerhet.context.SubjectHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DokumentServiceFasade {

    private final DokumentService dokumentService;
    private final DokumentSystemService dokumentSystemService;
    private final DokgenService dokgenService;
    private final BehandlingService behandlingService;
    private final ApplicationEventPublisher applicationEventPublisher;


    @Autowired
    public DokumentServiceFasade(DokumentService dokumentService, DokumentSystemService dokumentSystemService,
                                 DokgenService dokgenService, BehandlingService behandlingService,
                                 ApplicationEventPublisher applicationEventPublisher) {
        this.dokumentService = dokumentService;
        this.dokumentSystemService = dokumentSystemService;
        this.dokgenService = dokgenService;
        this.behandlingService = behandlingService;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    public byte[] produserUtkast(Produserbaredokumenter produserbartDokument, long behandlingId,
                                 BrevbestillingDto brevbestillingDto) throws FunksjonellException, TekniskException {
        if (dokgenService.erTilgjengeligDokgenmal(produserbartDokument)) {
            return dokgenService.produserUtkast(produserbartDokument, behandlingId,
                brevbestillingDto.getOrgNr(), brevbestillingDto);
        }
        return dokumentService.produserUtkast(produserbartDokument, behandlingId, brevbestillingDto);
    }

    @Transactional
    public void produserDokument(Produserbaredokumenter produserbartDokument, long behandlingId,
                                 BrevbestillingDto brevbestillingDto) throws FunksjonellException, TekniskException {
        String saksbehandler = SubjectHandler.getInstance().getUserID();
        Behandling behandling = behandlingService.hentBehandling(behandlingId);
        DoksysBrevbestilling brevbestilling = new DoksysBrevbestilling.Builder().medProduserbartDokument(produserbartDokument)
            .medAvsenderNavn(saksbehandler)
            .medMottakere(Mottaker.av(brevbestillingDto.getMottaker()))
            .medBegrunnelseKode(brevbestillingDto.getBegrunnelseKode())
            .medYtterligereInformasjon(brevbestillingDto.getYtterligereInformasjon())
            .medBehandling(behandling)
            .medFritekst(brevbestillingDto.getFritekst()).build();
        produserDokument(produserbartDokument, Mottaker.av(brevbestillingDto.getMottaker()), behandlingId, brevbestilling, brevbestillingDto);
    }

    @Transactional
    public void produserDokument(Produserbaredokumenter dokumentType, Mottaker mottaker, long id, DoksysBrevbestilling brevbestilling)
        throws FunksjonellException, TekniskException {
        BrevbestillingDto brevbestillingDto = new BrevbestillingDto.Builder()
            .medMottaker(mottaker.getRolle())
            .build();

        produserDokument(dokumentType, mottaker, id, brevbestilling, brevbestillingDto);
    }

    private void produserDokument(Produserbaredokumenter produserbartDokument, Mottaker mottaker,
                                 long behandlingID, DoksysBrevbestilling brevbestilling, BrevbestillingDto brevbestillingDto)
        throws TekniskException, FunksjonellException {
        if (dokgenService.erTilgjengeligDokgenmal(produserbartDokument)) {
            dokgenService.produserOgDistribuerBrev(produserbartDokument, behandlingID, brevbestillingDto);
        } else {
            dokumentSystemService.produserDokument(produserbartDokument, mottaker, behandlingID, brevbestilling);
        }

        applicationEventPublisher.publishEvent(new DokumentBestiltEvent(behandlingID, produserbartDokument));
    }
}
