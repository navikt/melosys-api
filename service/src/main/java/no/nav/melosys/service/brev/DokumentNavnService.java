package no.nav.melosys.service.brev;

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
public class DokumentNavnService {

    private final BrevmottakerService brevmottakerService;
    private final DokgenService dokgenService;
    private final LovvalgsperiodeService lovvalgsperiodeService;
    private final BehandlingService behandlingService;

    public DokumentNavnService(BehandlingService behandlingService, BrevmottakerService brevmottakerService, DokgenService dokgenService, LovvalgsperiodeService lovvalgsperiodeService) {
        this.behandlingService = behandlingService;
        this.brevmottakerService = brevmottakerService;
        this.dokgenService = dokgenService;
        this.lovvalgsperiodeService = lovvalgsperiodeService;
    }


    public String utledDokumentNavnForProduserbaredokumenterOgAktoerRolle(Behandling behandling, Produserbaredokumenter produserbaredokumenter, Aktoersroller mottakerRolle) {
        if (!STORBRITANNIA.equals(produserbaredokumenter)) {
            return produserbaredokumenter.getBeskrivelse();
        }
        Aktoer mottaker = brevmottakerService.avklarMottaker(produserbaredokumenter, Mottaker.av(mottakerRolle), behandling);
        return utledDokumentNavnForProduserbaredokumenterOgAktoer(behandling, produserbaredokumenter, mottaker, null);
    }

    public String utledDokumentNavnForProduserbaredokumenterOgAktoer(Behandling behandling, Produserbaredokumenter produserbaredokumenter, Aktoer mottaker, String standardTekst) {
        if (!STORBRITANNIA.equals(produserbaredokumenter)) {
            return standardTekst != null ? standardTekst : produserbaredokumenter.getBeskrivelse();
        }
        DokumentproduksjonsInfo dokumentproduksjonsInfo = dokgenService.hentDokumentInfo(produserbaredokumenter);
        return utledDokumentNavn(behandling, dokumentproduksjonsInfo, mottaker);
    }

    public String utledDokumentNavn(Behandling behandling, DokumentproduksjonsInfo dokumentproduksjonsInfo, Aktoer mottaker) {
        String tittel = utledTittel(behandling, dokumentproduksjonsInfo, mottaker);

        if (behandling.erNyVurdering()) {
            return lagEndringTittel(tittel);
        }

        return tittel;
    }

    private String utledTittel(Behandling behandling, DokumentproduksjonsInfo dokumentproduksjonsInfo, Aktoer mottaker) {
        if (mottaker.erUtenlandskMyndighet()) {
            return dokumentproduksjonsInfo.vedleggsTitler().get(VedleggTyper.ATTEST);
        }

        var vedtaksbrevTittel = dokumentproduksjonsInfo.vedleggsTitler().get(VedleggTyper.VEDTAKSBREV);

        if (FastMottakerMedOrgnr.SKATT.getOrgnr().equals((mottaker.getOrgnr()))) {
            return lagKopiTittel(vedtaksbrevTittel);
        }

        boolean erArtikkel8_2 = lovvalgsperiodeService.hentValidertLovvalgsperiode(behandling.getId()).getBestemmelse() == UK_ART8_2;

        if (mottaker.erBruker()) {
            return erArtikkel8_2 ? vedtaksbrevTittel : dokumentproduksjonsInfo.journalføringsTittel();
        } else {
            return lagKopiTittel(erArtikkel8_2 ? vedtaksbrevTittel : dokumentproduksjonsInfo.journalføringsTittel());
        }
    }

    private String lagKopiTittel(String tittel) {
        return "Kopi av " + org.springframework.util.StringUtils.uncapitalize(tittel);
    }

    private String lagEndringTittel(String tittel) {
        return tittel + " - endring";
    }
}
