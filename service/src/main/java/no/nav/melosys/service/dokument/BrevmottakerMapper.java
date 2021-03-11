package no.nav.melosys.service.dokument;

import java.util.List;
import java.util.Map;

import no.nav.melosys.domain.brev.Mottakerliste;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;

import static no.nav.melosys.domain.brev.FastMottaker.SKATT;
import static no.nav.melosys.domain.kodeverk.Aktoersroller.ARBEIDSGIVER;
import static no.nav.melosys.domain.kodeverk.Aktoersroller.BRUKER;
import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.*;

public class BrevmottakerMapper {

    public static final Map<Produserbaredokumenter, Mottakerliste> BREV_MOTTAKER_MAP;

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

    public static final List<Produserbaredokumenter> INFOBREV = List.of(
        MELDING_FORVENTET_SAKSBEHANDLINGSTID,
        MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD,
        MELDING_FORVENTET_SAKSBEHANDLINGSTID_KLAGE,
        MANGELBREV_BRUKER,
        MANGELBREV_ARBEIDSGIVER
    );
}
