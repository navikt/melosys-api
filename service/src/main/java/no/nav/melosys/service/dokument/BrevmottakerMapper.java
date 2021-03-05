package no.nav.melosys.service.dokument;

import java.util.List;
import java.util.Map;

import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.integrasjon.dokgen.dto.BrevMottaker;
import org.springframework.stereotype.Component;

import static java.util.Optional.ofNullable;
import static no.nav.melosys.domain.kodeverk.Aktoersroller.ARBEIDSGIVER;
import static no.nav.melosys.domain.kodeverk.Aktoersroller.BRUKER;
import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.*;
import static no.nav.melosys.integrasjon.dokgen.dto.FastMottaker.SKATT;

@Component
public class BrevmottakerMapper {
    private static Map<Produserbaredokumenter, BrevMottaker> brevMottakerMap = Map.of(
        MELDING_FORVENTET_SAKSBEHANDLINGSTID, new BrevMottaker.Builder()
            .medHovedMottaker(BRUKER).build(),

        MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD, new BrevMottaker.Builder()
            .medHovedMottaker(BRUKER).build(),

        MELDING_FORVENTET_SAKSBEHANDLINGSTID_KLAGE, new BrevMottaker.Builder()
            .medHovedMottaker(BRUKER).build(),

        MANGELBREV_BRUKER, new BrevMottaker.Builder()
            .medHovedMottaker(BRUKER).build(),

        MANGELBREV_ARBEIDSGIVER, new BrevMottaker.Builder()
            .medHovedMottaker(ARBEIDSGIVER)
            .medKopiMottaker(BRUKER).build(),

        INNVILGELSE_FOLKETRYGDLOVEN_2_8, new BrevMottaker.Builder()
            .medHovedMottaker(BRUKER)
            .medKopiMottakere(List.of(BRUKER, ARBEIDSGIVER))
            .medFastMottaker(SKATT).build()
    );

    public BrevMottaker finnBrevMottaker(Produserbaredokumenter produserbartdokument) {
        return ofNullable(brevMottakerMap.get(produserbartdokument))
            .orElseThrow(() -> new RuntimeException("Mangler mapping av mottakere for " + produserbartdokument));
    }
}
