package no.nav.melosys.service.brev.components;

import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.service.dokument.DokumentServiceFasade;
import no.nav.melosys.service.dokument.brev.BrevbestillingDto;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.*;

@Component
public class ProduserBrevComponent {

    private static final List<Produserbaredokumenter> DOKUMENTER_SOM_KAN_MANUELT_PRODUSERES = List.of(
        MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD,
        MELDING_FORVENTET_SAKSBEHANDLINGSTID_KLAGE,
        MANGELBREV_BRUKER, MANGELBREV_ARBEIDSGIVER,
        GENERELT_FRITEKSTBREV_BRUKER,
        GENERELT_FRITEKSTBREV_ARBEIDSGIVER,
        GENERELT_FRITEKSTBREV_VIRKSOMHET,
        AVSLAG_MANGLENDE_OPPLYSNINGER,
        UTENLANDSK_TRYGDEMYNDIGHET_FRITEKSTBREV,
        FRITEKSTBREV
    );

    private final DokumentServiceFasade dokumentServiceFasade;

    public ProduserBrevComponent(DokumentServiceFasade dokumentServiceFasade) {
        this.dokumentServiceFasade = dokumentServiceFasade;
    }

    @Transactional
    public void produserBrev(long behandlingId, BrevbestillingDto brevbestillingDto) {
        validerAtBrevKanProduseres(brevbestillingDto.getProduserbardokument());
        dokumentServiceFasade.produserDokument(behandlingId, brevbestillingDto);
    }

    private void validerAtBrevKanProduseres(Produserbaredokumenter produserbardokument) {
        var brevKanManueltProduseres = DOKUMENTER_SOM_KAN_MANUELT_PRODUSERES.contains(produserbardokument);
        if (!brevKanManueltProduseres) {
            throw new FunksjonellException("Manuell bestilling av " + produserbardokument + " er ikke støttet.");
        }
    }
}
