package no.nav.melosys.service.dokument;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.brev.Brevbestilling;
import no.nav.melosys.domain.brev.Mottaker;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.dokument.brev.BrevData;
import no.nav.melosys.service.dokument.brev.BrevbestillingDto;
import no.nav.melosys.sikkerhet.context.SubjectHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static java.lang.String.format;

@Service
public class DokumentServiceFasade {

    private final DokumentService dokumentService;
    private final DokumentSystemService dokumentSystemService;
    private final DokgenService dokgenService;
    private final BehandlingService behandlingService;


    @Autowired
    public DokumentServiceFasade(DokumentService dokumentService, DokumentSystemService dokumentSystemService, DokgenService dokgenService, BehandlingService behandlingService) {
        this.dokumentService = dokumentService;
        this.dokumentSystemService = dokumentSystemService;
        this.dokgenService = dokgenService;
        this.behandlingService = behandlingService;
    }

    public byte[] produserUtkast(Produserbaredokumenter produserbartDokument, long behandlingId, BrevbestillingDto brevbestillingDto) throws FunksjonellException, TekniskException {
        if (dokgenService.erTilgjengeligDokgenmal(produserbartDokument)) {
            return dokgenService.produserUtkast(produserbartDokument, behandlingId, new BrevData(brevbestillingDto));
        }
        return dokumentService.produserUtkast(produserbartDokument, behandlingId, brevbestillingDto);
    }

    public void produserDokument(Produserbaredokumenter produserbartDokument, long behandlingId, BrevbestillingDto brevbestillingDto) throws FunksjonellException, TekniskException {
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

    public void produserDokument(Produserbaredokumenter produserbartDokument, Mottaker mottaker, long behandlingID, Brevbestilling brevbestilling) throws TekniskException, FunksjonellException {
        if (dokgenService.erTilgjengeligDokgenmal(produserbartDokument)) {
            dokgenService.produserDokument(produserbartDokument, mottaker, brevbestilling);
        } else {
            dokumentSystemService.produserDokument(produserbartDokument, mottaker, behandlingID, brevbestilling);
        }
    }

    public byte[] produserBrev(Produserbaredokumenter produserbartDokument, Behandling behandling) throws MelosysException {
        if (dokgenService.erTilgjengeligDokgenmal(produserbartDokument)) {
            return dokgenService.produserBrev(produserbartDokument, behandling);
        }

        throw new FunksjonellException(format("Produserbart dokument %s er ikke støttet", produserbartDokument));
    }

    //TODO Slett og gjør til synkrontkall
    public void produserDokumentISaksflyt(Produserbaredokumenter produserbartDokument, Aktoersroller mottaker, long behandlingID, BrevData brevdata)
        throws FunksjonellException {
        dokumentService.produserDokumentISaksflyt(produserbartDokument, mottaker, behandlingID, brevdata);
    }
}
