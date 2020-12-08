package no.nav.melosys.service.dokument;

import java.util.List;

import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.brev.Brevbestilling;
import no.nav.melosys.domain.brev.DokgenBrevbestilling;
import no.nav.melosys.domain.brev.Mottaker;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.dokument.brev.BrevbestillingDto;
import no.nav.melosys.service.saksflyt.ProsessinstansService;
import no.nav.melosys.sikkerhet.context.SubjectHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DokumentServiceFasade {

    private final DokumentService dokumentService;
    private final DokumentSystemService dokumentSystemService;
    private final DokgenService dokgenService;
    private final BehandlingService behandlingService;
    private final ProsessinstansService prosessinstansService;
    private final BrevmottakerService brevmottakerService;


    @Autowired
    public DokumentServiceFasade(DokumentService dokumentService, DokumentSystemService dokumentSystemService,
                                 DokgenService dokgenService, BehandlingService behandlingService,
                                 ProsessinstansService prosessinstansService, BrevmottakerService brevmottakerService) {
        this.dokumentService = dokumentService;
        this.dokumentSystemService = dokumentSystemService;
        this.dokgenService = dokgenService;
        this.behandlingService = behandlingService;
        this.prosessinstansService = prosessinstansService;
        this.brevmottakerService = brevmottakerService;
    }

    public byte[] produserUtkast(Produserbaredokumenter produserbartDokument, long behandlingId,
                                 BrevbestillingDto brevbestillingDto) throws FunksjonellException, TekniskException {
        if (dokgenService.erTilgjengeligDokgenmal(produserbartDokument)) {
            Behandling behandling = behandlingService.hentBehandling(behandlingId);
            DokgenBrevbestilling brevbestilling = new DokgenBrevbestilling(
                produserbartDokument,
                behandling,
                null,
                null
            );
            brevbestilling.setBestillKopi(true);
            return dokgenService.produserBrev(brevbestilling);
        }
        return dokumentService.produserUtkast(produserbartDokument, behandlingId, brevbestillingDto);
    }

    @Transactional
    public void produserDokument(Produserbaredokumenter produserbartDokument, long behandlingId,
                                 BrevbestillingDto brevbestillingDto) throws FunksjonellException, TekniskException {
        String saksbehandler = SubjectHandler.getInstance().getUserID();
        Behandling behandling = behandlingService.hentBehandling(behandlingId);
        Brevbestilling brevbestilling = new Brevbestilling.Builder().medDokumentType(produserbartDokument)
            .medAvsender(saksbehandler)
            .medMottakere(Mottaker.av(brevbestillingDto.mottaker))
            .medBegrunnelseKode(brevbestillingDto.begrunnelseKode)
            .medYtterligereInformasjon(brevbestillingDto.ytterligereInformasjon)
            .medBehandling(behandling)
            .medFritekst(brevbestillingDto.fritekst).build();
        produserDokument(produserbartDokument, Mottaker.av(brevbestillingDto.mottaker), behandlingId, brevbestilling);
    }

    @Transactional
    public void produserDokument(Produserbaredokumenter produserbartDokument, Mottaker mottaker,
                                 long behandlingID, Brevbestilling brevbestilling) throws TekniskException, FunksjonellException {
        if (dokgenService.erTilgjengeligDokgenmal(produserbartDokument)) {
            Behandling behandling = behandlingService.hentBehandling(behandlingID);
            List<Aktoer> mottakere = brevmottakerService.avklarMottakere(produserbartDokument, mottaker, behandling);
            for (Aktoer aktoer : mottakere) {
                prosessinstansService.opprettProsessinstansOpprettOgDistribuerBrev(produserbartDokument, behandling, aktoer);
            }
        } else {
            dokumentSystemService.produserDokument(produserbartDokument, mottaker, behandlingID, brevbestilling);
        }
    }
}
