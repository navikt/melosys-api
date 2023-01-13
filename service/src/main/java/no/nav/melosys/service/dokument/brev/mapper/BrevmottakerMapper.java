package no.nav.melosys.service.dokument.brev.mapper;

import java.util.Map;

import no.nav.melosys.domain.brev.Mottakerliste;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;

import static no.nav.melosys.domain.brev.BrevkopiRegel.*;
import static no.nav.melosys.domain.kodeverk.Aktoersroller.*;
import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.*;

public class BrevmottakerMapper {

    public static final Map<Produserbaredokumenter, Mottakerliste> BREV_MOTTAKER_MAP;

    static {
        BREV_MOTTAKER_MAP = Map.ofEntries(
            Map.entry(MELDING_FORVENTET_SAKSBEHANDLINGSTID, new Mottakerliste.Builder()
                .medHovedMottaker(BRUKER).build()),

            Map.entry(MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD, new Mottakerliste.Builder()
                .medHovedMottaker(BRUKER).build()),

            Map.entry(MELDING_FORVENTET_SAKSBEHANDLINGSTID_KLAGE, new Mottakerliste.Builder()
                .medHovedMottaker(BRUKER).build()),

            Map.entry(MANGELBREV_BRUKER, new Mottakerliste.Builder()
                .medHovedMottaker(BRUKER).build()),

            Map.entry(MANGELBREV_ARBEIDSGIVER, new Mottakerliste.Builder()
                .medHovedMottaker(ARBEIDSGIVER)
                .medBrevkopiRegler(
                    BRUKER_FÅR_KOPI
                ).build()),

            Map.entry(INNVILGELSE_FOLKETRYGDLOVEN_2_8, new Mottakerliste.Builder()
                .medHovedMottaker(BRUKER)
                .medBrevkopiRegler(
                    BRUKER_FÅR_KOPI_HVIS_FULLMEKTIG_FINNES,
                    ARBEIDSGIVER_FÅR_KOPI_HVIS_IKKE_SELVBETALENDE_BRUKER,
                    SKATT_FÅR_KOPI_HVIS_AVGIFTSPLIKTIG_INNTEKT
                ).build()),

            Map.entry(TRYGDEAVTALE_GB, new Mottakerliste.Builder()
                .medHovedMottaker(BRUKER)
                .medBrevkopiRegler(
                    ARBEIDSGIVER_FÅR_KOPI,
                    SKATT_FÅR_KOPI,
                    UTENLANDSK_TRYGDEMYNDIGHET_FÅR_KOPI_HVIS_IKKE_ART_8_2
                ).build()),

            Map.entry(TRYGDEAVTALE_US, new Mottakerliste.Builder()
                .medHovedMottaker(BRUKER)
                .medBrevkopiRegler(
                    ARBEIDSGIVER_FÅR_KOPI,
                    SKATT_FÅR_KOPI,
                    UTENLANDSK_TRYGDEMYNDIGHET_FÅR_KOPI
                ).build()),

            Map.entry(TRYGDEAVTALE_CAN, new Mottakerliste.Builder()
                .medHovedMottaker(BRUKER)
                .medBrevkopiRegler(
                    ARBEIDSGIVER_FÅR_KOPI,
                    SKATT_FÅR_KOPI,
                    UTENLANDSK_TRYGDEMYNDIGHET_FÅR_KOPI
                ).build()),

            Map.entry(TRYGDEAVTALE_AU, new Mottakerliste.Builder()
                .medHovedMottaker(BRUKER)
                .medBrevkopiRegler(
                    ARBEIDSGIVER_FÅR_KOPI,
                    SKATT_FÅR_KOPI,
                    UTENLANDSK_TRYGDEMYNDIGHET_FÅR_KOPI
                ).build()),

            Map.entry(GENERELT_FRITEKSTBREV_BRUKER, new Mottakerliste.Builder()
                .medHovedMottaker(BRUKER).build()),

            Map.entry(GENERELT_FRITEKSTBREV_ARBEIDSGIVER, new Mottakerliste.Builder()
                .medHovedMottaker(ARBEIDSGIVER)
                .medBrevkopiRegler(
                    BRUKER_FÅR_KOPI
                ).build()),

            Map.entry(GENERELT_FRITEKSTBREV_VIRKSOMHET, new Mottakerliste.Builder()
                .medHovedMottaker(VIRKSOMHET)
                .build()),

            Map.entry(FRITEKSTBREV, new Mottakerliste.Builder()
                .medHovedMottaker(OFFENTLIG_ETAT)
                .build()),

            Map.entry(UTENLANDSK_TRYGDEMYNDIGHET_FRITEKSTBREV, new Mottakerliste.Builder()
                .medHovedMottaker(TRYGDEMYNDIGHET)
                .build())
        );
    }
}
