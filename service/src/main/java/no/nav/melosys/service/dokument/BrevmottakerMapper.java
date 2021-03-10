package no.nav.melosys.service.dokument;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.brev.Mottaker;
import no.nav.melosys.domain.brev.Mottakerliste;
import no.nav.melosys.domain.folketrygden.FastsattTrygdeavgift;
import no.nav.melosys.domain.folketrygden.MedlemAvFolketrygden;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.repository.MedlemAvFolketrygdenRepository;
import no.nav.melosys.service.behandling.BehandlingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static java.util.Optional.ofNullable;
import static no.nav.melosys.domain.Fagsak.erSakstypeFtrl;
import static no.nav.melosys.domain.brev.FastMottaker.SKATT;
import static no.nav.melosys.domain.kodeverk.Aktoersroller.ARBEIDSGIVER;
import static no.nav.melosys.domain.kodeverk.Aktoersroller.BRUKER;
import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.*;

@Component
public class BrevmottakerMapper {

    private static final Map<Produserbaredokumenter, Mottakerliste> BREV_MOTTAKER_MAP;

    static {
        BREV_MOTTAKER_MAP = Map.of(
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
                .medKopiMottakere(BRUKER).build(),

            INNVILGELSE_FOLKETRYGDLOVEN_2_8, new Mottakerliste.Builder()
                .medHovedMottaker(BRUKER)
                .medKopiMottakere(BRUKER, ARBEIDSGIVER)
                .medFasteMottakere(SKATT).build()
        );
    }

    private static final List<Produserbaredokumenter> INFOBREV = List.of(
        MELDING_FORVENTET_SAKSBEHANDLINGSTID,
        MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD,
        MELDING_FORVENTET_SAKSBEHANDLINGSTID_KLAGE,
        MANGELBREV_BRUKER,
        MANGELBREV_ARBEIDSGIVER
    );

    private final BrevmottakerService brevmottakerService;
    private final BehandlingService behandlingService;
    private final MedlemAvFolketrygdenRepository medlemAvFolketrygdenRepository;

    @Autowired
    public BrevmottakerMapper(BrevmottakerService brevmottakerService, BehandlingService behandlingService, MedlemAvFolketrygdenRepository medlemAvFolketrygdenRepository) {
        this.brevmottakerService = brevmottakerService;
        this.behandlingService = behandlingService;
        this.medlemAvFolketrygdenRepository = medlemAvFolketrygdenRepository;
    }

    public Mottakerliste finnBrevMottaker(Produserbaredokumenter produserbartdokument, long behandlingId)
        throws FunksjonellException, TekniskException {

        Mottakerliste mottakerliste = ofNullable(BREV_MOTTAKER_MAP.get(produserbartdokument))
            .orElseThrow(() -> new IkkeFunnetException("Mangler mapping av mottakere for " + produserbartdokument));

        Mottakerliste mottakerListeKopi = new Mottakerliste.Builder()
            .medHovedMottaker(mottakerliste.getHovedMottaker())
            .medKopiMottakere(new ArrayList<>(mottakerliste.getKopiMottakere()))
            .medFasteMottakere(new ArrayList<>(mottakerliste.getFasteMottakere()))
            .build();

        if (!mottakerListeKopi.getKopiMottakere().isEmpty()) {
            avklarKopier(produserbartdokument, behandlingId, mottakerListeKopi);
        }

        return mottakerListeKopi;
    }

    private Mottakerliste avklarKopier(Produserbaredokumenter produserbartdokument, long behandlingId, Mottakerliste mottakerliste)
        throws FunksjonellException, TekniskException {
        Behandling behandling = behandlingService.hentBehandling(behandlingId);
        Aktoer hovedmottaker = brevmottakerService.avklarMottakere(produserbartdokument, Mottaker.av(mottakerliste.getHovedMottaker()), behandling).get(0);

        if (hovedmottaker.getRolle() == BRUKER) {
            mottakerliste.getKopiMottakere().remove(BRUKER);
        }

        if (erSakstypeFtrl(behandling.getFagsak().getType()) && !INFOBREV.contains(produserbartdokument)) {
            MedlemAvFolketrygden medlemAvFolketrygden = medlemAvFolketrygdenRepository.findByBehandlingsresultatId(behandlingId)
                .orElseThrow(() -> new IkkeFunnetException("Finner ikke medlemAvFolketrygden for behandlingsresultatID " + behandlingId));

            FastsattTrygdeavgift fastsattTrygdeavgift = medlemAvFolketrygden.getFastsattTrygdeavgift();

            if (fastsattTrygdeavgift.harIkkeAvgiftspliktigInntekt()) {
                mottakerliste.getFasteMottakere().remove(SKATT);
            }

            if (brukerErSelvbetalende(fastsattTrygdeavgift)) {
                mottakerliste.getKopiMottakere().remove(ARBEIDSGIVER);
            }
        }

        return mottakerliste;
    }

    private boolean brukerErSelvbetalende(FastsattTrygdeavgift fastsattTrygdeavgift) {
        return fastsattTrygdeavgift.getBetalesAv().getRolle() == BRUKER;
    }
}
