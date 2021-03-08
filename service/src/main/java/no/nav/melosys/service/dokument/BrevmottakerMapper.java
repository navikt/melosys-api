package no.nav.melosys.service.dokument;

import java.util.List;
import java.util.Map;

import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.brev.Mottaker;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.domain.brev.Mottakerliste;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.behandling.BehandlingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static java.util.Optional.ofNullable;
import static no.nav.melosys.domain.kodeverk.Aktoersroller.*;
import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.*;
import static no.nav.melosys.domain.brev.FastMottaker.SKATT;

@Component
public class BrevmottakerMapper {

    private static final Map<Produserbaredokumenter, Mottakerliste> brevMottakerMap = Map.of(
        MELDING_FORVENTET_SAKSBEHANDLINGSTID, new Mottakerliste.Builder()
            .medHovedMottaker(BRUKER).build(),

        MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD, new Mottakerliste.Builder()
            .medHovedMottaker(BRUKER).build(),

        MELDING_FORVENTET_SAKSBEHANDLINGSTID_KLAGE, new Mottakerliste.Builder()
            .medHovedMottaker(BRUKER).build(),

        MANGELBREV_BRUKER, new Mottakerliste.Builder()
            .medHovedMottaker(BRUKER).build(),

        MANGELBREV_ARBEIDSGIVER, new Mottakerliste.Builder()
            .medHovedMottaker(ARBEIDSGIVER)
            .medKopiMottaker(BRUKER).build(),

        INNVILGELSE_FOLKETRYGDLOVEN_2_8, new Mottakerliste.Builder()
            .medHovedMottaker(BRUKER)
            .medKopiMottakere(List.of(BRUKER, ARBEIDSGIVER))
            .medFastMottaker(SKATT).build()
    );

    private static final List<Produserbaredokumenter> vedtaksbrev = List.of(
        INNVILGELSE_FOLKETRYGDLOVEN_2_8
    );

    private static final List<Produserbaredokumenter> avslagsbrev = List.of();

    private final BrevmottakerService brevmottakerService;
    private final BehandlingService behandlingService;

    @Autowired
    public BrevmottakerMapper(BrevmottakerService brevmottakerService, BehandlingService behandlingService) {
        this.brevmottakerService = brevmottakerService;
        this.behandlingService = behandlingService;
    }

    public Mottakerliste finnBrevMottaker(Produserbaredokumenter produserbartdokument, long behandlingId)
        throws FunksjonellException, TekniskException {

        Mottakerliste mottakerliste = ofNullable(brevMottakerMap.get(produserbartdokument))
            .orElseThrow(() -> new RuntimeException("Mangler mapping av mottakere for " + produserbartdokument));

        if (vedtaksbrev.contains(produserbartdokument) || avslagsbrev.contains(produserbartdokument)) {
            mottakerliste = avklarKopier(produserbartdokument, behandlingId, mottakerliste);
        }

        return mottakerliste;
    }

    private Mottakerliste avklarKopier(Produserbaredokumenter produserbartdokument, long behandlingId, Mottakerliste mottakerliste)
        throws FunksjonellException, TekniskException {
        Behandling behandling = behandlingService.hentBehandling(behandlingId);

        Aktoer hovedmottaker = brevmottakerService.avklarMottakere(produserbartdokument, Mottaker.av(mottakerliste.getHovedMottaker()), behandling).get(0);

        if (hovedmottaker.getRolle() == BRUKER && mottakerliste.getKopiMottakere().contains(BRUKER)) {
            mottakerliste.getKopiMottakere().remove(BRUKER);
        }

        //TODO Hent info om avgiftsplikt
//        if (!erAvgiftspliktig()) {
//            mottakerliste.getFasteMottakere().remove(SKATT);
//        }

        //TODO Sjekk om bruker er selvbetalende (ikke norsk arbeidsgiver som representant)
//        if(erSelvBetalende()) {
//            mottakerliste.getKopiMottakere().remove(ARBEIDSGIVER);
//        }

        return mottakerliste;
    }
}
