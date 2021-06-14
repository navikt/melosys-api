package no.nav.melosys.service.dokument;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.brev.DoksysBrevbestilling;
import no.nav.melosys.domain.brev.Mottaker;
import no.nav.melosys.domain.dokument.DokumentBestiltEvent;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
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

    public byte[] produserUtkast(long behandlingId, BrevbestillingDto brevbestillingDto) {
        if (dokgenService.erTilgjengeligDokgenmal(brevbestillingDto.getProduserbardokument())) {
            return dokgenService.produserUtkast(behandlingId, brevbestillingDto);
        }
        return dokumentService.produserUtkast(behandlingId, brevbestillingDto);
    }

    @Transactional
    public void produserDokument(long behandlingId, BrevbestillingDto brevbestillingDto) {
        String saksbehandler = SubjectHandler.getInstance().getUserID();
        var behandling = behandlingService.hentBehandling(behandlingId);
        DoksysBrevbestilling brevbestilling = new DoksysBrevbestilling.Builder().medProduserbartDokument(brevbestillingDto.getProduserbardokument())
            .medAvsenderNavn(saksbehandler)
            .medMottakere(Mottaker.av(brevbestillingDto.getMottaker()))
            .medBegrunnelseKode(brevbestillingDto.getBegrunnelseKode())
            .medYtterligereInformasjon(brevbestillingDto.getYtterligereInformasjon())
            .medBehandling(behandling)
            .medFritekst(brevbestillingDto.getFritekst()).build();
        produserDokument(behandlingId, brevbestilling, brevbestillingDto, Mottaker.av(brevbestillingDto.getMottaker()));
    }

    @Transactional
    public void produserDokument(Produserbaredokumenter dokumentType, Mottaker mottaker, long behandlingId, DoksysBrevbestilling brevbestilling) {
        var brevbestillingDto = new BrevbestillingDto.Builder()
            .medProduserbardokument(dokumentType)
            .medMottaker(mottaker.getRolle())
            .build();

        produserDokument(behandlingId, brevbestilling, brevbestillingDto, mottaker);
    }

    private void produserDokument(long behandlingID, DoksysBrevbestilling brevbestilling, BrevbestillingDto brevbestillingDto, Mottaker mottaker) {
        Produserbaredokumenter produserbartDokument = brevbestillingDto.getProduserbardokument();

        if (dokgenService.erTilgjengeligDokgenmal(produserbartDokument)) {
            dokgenService.produserOgDistribuerBrev(behandlingID, brevbestillingDto);
        } else {
            dokumentSystemService.produserDokument(produserbartDokument, mottaker, behandlingID, brevbestilling);
        }

        applicationEventPublisher.publishEvent(new DokumentBestiltEvent(behandlingID, produserbartDokument));
    }
}
