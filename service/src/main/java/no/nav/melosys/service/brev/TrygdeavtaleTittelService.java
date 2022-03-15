package no.nav.melosys.service.brev;

import java.util.List;

import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.brev.FastMottakerMedOrgnr;
import no.nav.melosys.domain.brev.Mottaker;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.dokument.BrevmottakerService;
import no.nav.melosys.service.dokument.DokgenService;
import no.nav.melosys.service.dokument.DokumentproduksjonsInfo;
import no.nav.melosys.service.dokument.VedleggTyper;
import org.springframework.stereotype.Service;

import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.STORBRITANNIA;
import static no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_trygdeavtale_uk.UK_ART8_2;

@Service
public class TrygdeavtaleTittelService {

    private final BrevmottakerService brevmottakerService;
    private final DokgenService dokgenService;
    private final LovvalgsperiodeService lovvalgsperiodeService;
    private final BehandlingService behandlingService;

    public TrygdeavtaleTittelService(BehandlingService behandlingService, BrevmottakerService brevmottakerService, DokgenService dokgenService, LovvalgsperiodeService lovvalgsperiodeService) {
        this.behandlingService = behandlingService;
        this.brevmottakerService = brevmottakerService;
        this.dokgenService = dokgenService;
        this.lovvalgsperiodeService = lovvalgsperiodeService;
    }


    public String utledDokumentNavnForProduserbaredokumenterOgAktoerRolle(Behandling behandling, Produserbaredokumenter produserbaredokumenter, Aktoersroller mottakerRolle, String fastTekst) {
        if (!STORBRITANNIA.equals(produserbaredokumenter)) {
            return fastTekst != null ? fastTekst : produserbaredokumenter.getBeskrivelse();
        }
        List<Aktoer> mottakere = brevmottakerService.avklarMottakere(produserbaredokumenter, Mottaker.av(mottakerRolle), behandling, false, true);
        if (mottakere.size() > 1) {
            throw new RuntimeException("Skal bare være en mottaker, men var: " + mottakere.size());
        }
        return utledDokumentNavnForProduserbaredokumenterOgAktoer(behandling, produserbaredokumenter, mottakere.get(0), null);
    }

    public String utledDokumentNavnForProduserbaredokumenterOgAktoer(Behandling behandling, Produserbaredokumenter produserbaredokumenter, Aktoer mottaker, String fastTekst) {
        if (!STORBRITANNIA.equals(produserbaredokumenter)) {
            return fastTekst != null ? fastTekst : produserbaredokumenter.getBeskrivelse();
        }
        DokumentproduksjonsInfo dokumentproduksjonsInfo = dokgenService.hentDokumentInfo(produserbaredokumenter);
        return utledDokumentNavn(behandling.getId(), dokumentproduksjonsInfo, mottaker);
    }

    public String utledDokumentNavn(long behandlingID, DokumentproduksjonsInfo dokumentproduksjonsInfo, Aktoer mottaker) {
        String tittel = utledTittel(behandlingID, dokumentproduksjonsInfo, mottaker);
        Behandling behandling = behandlingService.hentBehandling(behandlingID);

        if (behandling.erNyVurdering()) {
            return lagEndringTittel(tittel);
        }

        return tittel;
    }

    private String utledTittel(long behandlingID, DokumentproduksjonsInfo dokumentproduksjonsInfo, Aktoer mottaker) {
        if (mottaker.erUtenlandskMyndighet()) {
            return dokumentproduksjonsInfo.vedleggsTitler().get(VedleggTyper.ATTEST);
        }

        var vedtaksbrevTittel = dokumentproduksjonsInfo.vedleggsTitler().get(VedleggTyper.VEDTAKSBREV);

        if (FastMottakerMedOrgnr.SKATT.getOrgnr().equals((mottaker.getOrgnr()))) {
            return lagKopiTittel(vedtaksbrevTittel);
        }

        boolean erArtikkel8_2 = lovvalgsperiodeService.hentValidertLovvalgsperiode(behandlingID).getBestemmelse() == UK_ART8_2;

        if (mottaker.erOrganisasjon()) {
            return lagKopiTittel(erArtikkel8_2 ? vedtaksbrevTittel : dokumentproduksjonsInfo.journalføringsTittel());
        } else {
            return erArtikkel8_2 ? vedtaksbrevTittel : dokumentproduksjonsInfo.journalføringsTittel();
        }
    }

    private String lagKopiTittel(String tittel) {
        return "Kopi av " + org.springframework.util.StringUtils.uncapitalize(tittel);
    }

    private String lagEndringTittel(String tittel) {
        return tittel + " - endring";
    }
}
