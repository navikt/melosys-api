package no.nav.melosys.service.dokument;

import no.nav.melosys.domain.brev.DoksysBrevbestilling;
import no.nav.melosys.domain.brev.Mottaker;
import no.nav.melosys.domain.dokument.DokumentBestiltEvent;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.dokument.brev.BrevbestillingDto;
import no.nav.melosys.sikkerhet.context.SubjectHandler;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD;

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

    @Transactional
    public void produserDokument(long behandlingId, BrevbestillingDto brevbestillingDto) {
        String saksbehandlerID = SubjectHandler.getInstance().getUserID();
        var behandling = behandlingService.hentBehandlingMedSaksopplysninger(behandlingId);
        DoksysBrevbestilling brevbestilling = new DoksysBrevbestilling.Builder()
            .medProduserbartDokument(brevbestillingDto.getProduserbardokument())
            .medAvsenderID(saksbehandlerID)
            .medMottakere(Mottaker.medRolle(brevbestillingDto.getMottaker()))
            .medBegrunnelseKode(brevbestillingDto.getBegrunnelseKode())
            .medYtterligereInformasjon(brevbestillingDto.getYtterligereInformasjon())
            .medBehandling(behandling)
            .medDistribusjonsType(brevbestillingDto.getDistribusjonstype())
            .medFritekst(brevbestillingDto.getFritekst()).build();
        produserDokument(behandlingId, brevbestilling, brevbestillingDto, Mottaker.medRolle(brevbestillingDto.getMottaker()));
    }

    @Transactional
    public void produserOgDistribuerBrev(Produserbaredokumenter produserbartDokument, Mottaker mottaker, String fritekst,
                                         String begrunnelseKode, String avsenderId, long behandlingId) {
        var brevbestillingDto = new BrevbestillingDto();
        brevbestillingDto.setProduserbardokument(produserbartDokument);
        brevbestillingDto.setMottaker(mottaker.getRolle());
        brevbestillingDto.setFritekst(fritekst);
        brevbestillingDto.setBegrunnelseKode(begrunnelseKode);
        brevbestillingDto.setBestillersId(avsenderId);
        // Metoden kalles kun gjennom PIene for forvaltningsmelding og henleggelsesmelding. Overstyringen av mottaker legges inn under en if blokk
        // for å unngå uønsket funksjonalitet dersom man tar i bruk denne metoden i fremtiden.
        if (produserbartDokument.equals(MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD)) {
            brevbestillingDto.setOrgnr(mottaker.getOrgnr());
            brevbestillingDto.setAnnenPersonMottakerIdent(mottaker.getPersonIdent());
        }

        dokgenService.produserOgDistribuerBrev(behandlingId, brevbestillingDto);
    }


    @Transactional
    public void produserDokument(Produserbaredokumenter dokumentType, Mottaker mottaker, long behandlingId, DoksysBrevbestilling brevbestilling) {
        var brevbestillingDto = new BrevbestillingDto();
        brevbestillingDto.setProduserbardokument(dokumentType);
        brevbestillingDto.setMottaker(mottaker.getRolle());
        brevbestillingDto.setOrgnr(mottaker.getOrgnr());
        brevbestillingDto.setFritekst(brevbestilling.getFritekst());
        brevbestillingDto.setBegrunnelseKode(brevbestilling.getBegrunnelseKode());
        brevbestillingDto.setBestillersId(brevbestilling.getAvsenderID());

        produserDokument(behandlingId, brevbestilling, brevbestillingDto, mottaker);
    }

    private void produserDokument(long behandlingID, DoksysBrevbestilling brevbestilling, BrevbestillingDto brevbestillingDto, Mottaker mottaker) {
        Produserbaredokumenter produserbartDokument = brevbestillingDto.getProduserbardokument();

        if (dokgenService.erTilgjengeligDokgenmal(produserbartDokument)) {
            dokgenService.produserOgDistribuerBrev(behandlingID, brevbestillingDto);
        } else {
            dokumentService.produserDokument(produserbartDokument, mottaker, behandlingID, brevbestilling);
        }

        applicationEventPublisher.publishEvent(new DokumentBestiltEvent(behandlingID, produserbartDokument));
    }
}
