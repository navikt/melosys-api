package no.nav.melosys.service.dokument;

import java.util.Map;

import no.nav.melosys.domain.brev.Mottakerliste;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;

import static no.nav.melosys.domain.brev.BrevkopiRegel.*;
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
                .medBrevkopiRegler(
                    BRUKER_FÅR_KOPI
                )
                .build(),

            INNVILGELSE_FOLKETRYGDLOVEN_2_8, new Mottakerliste.Builder()
                .medHovedMottaker(BRUKER)
                .medBrevkopiRegler(
                    BRUKER_FÅR_KOPI_HVIS_FULLMEKTIG_FINNES,
                    ARBEIDSGIVER_FÅR_KOPI_HVIS_IKKE_SELVBETALENDE_BRUKER,
                    SKATT_FÅR_KOPI_HVIS_AVGIFTSPLIKTIG_INNTEKT
                ).build(),

            INNVILGELSE_UK, new Mottakerliste.Builder()
                .medHovedMottaker(BRUKER)
                .medBrevkopiRegler(ARBEIDSGIVER_FÅR_KOPI, SKATT_FÅR_KOPI)
                .build(),

            GENERELT_FRITEKSTBREV_BRUKER, new Mottakerliste.Builder()
                .medHovedMottaker(BRUKER).build(),

            GENERELT_FRITEKSTBREV_ARBEIDSGIVER, new Mottakerliste.Builder()
                .medHovedMottaker(ARBEIDSGIVER)
                .medBrevkopiRegler(
                    BRUKER_FÅR_KOPI
                ).build()
        );
    }
}
