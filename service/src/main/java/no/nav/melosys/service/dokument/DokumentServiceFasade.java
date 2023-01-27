package no.nav.melosys.service.dokument;

import no.nav.melosys.domain.brev.DoksysBrevbestilling;
import no.nav.melosys.domain.brev.Mottaker;
import no.nav.melosys.domain.dokument.DokumentBestiltEvent;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.dokument.brev.BrevbestillingRequest;
import no.nav.melosys.sikkerhet.context.SubjectHandler;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DokumentServiceFasade {

    private final DokumentService dokumentService;
    private final DokgenService dokgenService;
    private final BehandlingService behandlingService;
    private final ApplicationEventPublisher applicationEventPublisher;


    public DokumentServiceFasade(DokumentService dokumentService,
                                 DokgenService dokgenService, BehandlingService behandlingService,
                                 ApplicationEventPublisher applicationEventPublisher) {
        this.dokumentService = dokumentService;
        this.dokgenService = dokgenService;
        this.behandlingService = behandlingService;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Deprecated(since = "Slettes når nytt endepunkt i 'BrevbestillingTjeneste' er klare")
    public byte[] produserUtkast(long behandlingId, BrevbestillingRequest brevbestillingRequest) {
        if (dokgenService.erTilgjengeligDokgenmal(brevbestillingRequest.getProduserbardokument())) {
            return dokgenService.produserUtkast(behandlingId, brevbestillingRequest);
        }
        return dokumentService.produserUtkast(behandlingId, brevbestillingRequest);
    }

    @Transactional
    public void produserDokument(long behandlingId, BrevbestillingRequest brevbestillingRequest) {
        String saksbehandlerID = SubjectHandler.getInstance().getUserID();
        var behandling = behandlingService.hentBehandlingMedSaksopplysninger(behandlingId);
        DoksysBrevbestilling brevbestilling = new DoksysBrevbestilling.Builder()
            .medProduserbartDokument(brevbestillingRequest.getProduserbardokument())
            .medAvsenderID(saksbehandlerID)
            .medMottakere(Mottaker.av(brevbestillingRequest.getMottaker()))
            .medBegrunnelseKode(brevbestillingRequest.getBegrunnelseKode())
            .medYtterligereInformasjon(brevbestillingRequest.getYtterligereInformasjon())
            .medBehandling(behandling)
            .medDistribusjonsType(brevbestillingRequest.getDistribusjonstype())
            .medFritekst(brevbestillingRequest.getFritekst()).build();
        produserDokument(behandlingId, brevbestilling, brevbestillingRequest, Mottaker.av(brevbestillingRequest.getMottaker()));
    }

    @Transactional
    public void produserOgDistribuerBrev(Produserbaredokumenter produserbartDokument, Mottaker mottaker, String fritekst,
                                         String begrunnelseKode, String avsenderId, long behandlingId) {
        var brevbestillingRequest = new BrevbestillingRequest.Builder()
            .medProduserbardokument(produserbartDokument)
            .medMottaker(mottaker.hentAktørsRolle())
            .medFritekst(fritekst)
            .medBegrunnelseKode(begrunnelseKode)
            .medBestillersId(avsenderId)
            .build();

        dokgenService.produserOgDistribuerBrev(behandlingId, brevbestillingRequest);
    }

    @Transactional
    public void produserDokument(Produserbaredokumenter dokumentType, Mottaker mottaker, long behandlingId, DoksysBrevbestilling brevbestilling) {
        var brevbestillingRequest = new BrevbestillingRequest.Builder()
            .medProduserbardokument(dokumentType)
            .medMottaker(mottaker.hentAktørsRolle())
            .medFritekst(hentFritekst(brevbestilling))
            .medBegrunnelseKode(brevbestilling.getBegrunnelseKode())
            .medBestillersId(brevbestilling.getAvsenderID())
            .build();

        produserDokument(behandlingId, brevbestilling, brevbestillingRequest, mottaker);
    }

    private String hentFritekst(DoksysBrevbestilling brevbestilling) {
        if (brevbestilling.getProduserbartdokument() == null) return null;

        return switch (brevbestilling.getProduserbartdokument()) {
            case AVSLAG_MANGLENDE_OPPLYSNINGER, MELDING_HENLAGT_SAK -> brevbestilling.getFritekst();
            default -> null;
        };
    }

    private void produserDokument(long behandlingID, DoksysBrevbestilling brevbestilling, BrevbestillingRequest brevbestillingRequest, Mottaker mottaker) {
        Produserbaredokumenter produserbartDokument = brevbestillingRequest.getProduserbardokument();

        if (dokgenService.erTilgjengeligDokgenmal(produserbartDokument)) {
            dokgenService.produserOgDistribuerBrev(behandlingID, brevbestillingRequest);
        } else {
            dokumentService.produserDokument(produserbartDokument, mottaker, behandlingID, brevbestilling);
        }

        applicationEventPublisher.publishEvent(new DokumentBestiltEvent(behandlingID, produserbartDokument));
    }
}
