package no.nav.melosys.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import no.nav.melosys.domain.kodeverk.Sakstemaer;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.exception.FunksjonellException;
import org.apache.commons.lang3.tuple.Pair;

import static no.nav.melosys.domain.kodeverk.Sakstemaer.MEDLEMSKAP_LOVVALG;
import static no.nav.melosys.domain.kodeverk.Sakstemaer.TRYGDEAVGIFT;
import static no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema.*;
import static no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper.*;

/*
 * Klasse for å finne behandlingsfrister basert på sakstema, behandlingstema og behandlingstype
 * ref: https://confluence.adeo.no/display/TEESSI/Behandlingsfrister+i+Melosys
 * */
public class BehandlingfristKriterier extends Behandling {

    public static LocalDate hentBehandlingsFrist(Sakstemaer sakstema, Behandlingstema behandlingstema, Behandlingstyper behandlingstype) {
        LocalDate frist8Uker = LocalDate.now().plusWeeks(8);
        LocalDate frist70Dager = LocalDate.now().plusDays(70);
        LocalDate frist90Dager = LocalDate.now().plusDays(90);
        LocalDate frist180Dager = LocalDate.now().plusDays(180);

        List<Pair<Boolean, LocalDate>> behandlingfrister = List.of(
            Pair.of(harFrist8Uker_Lovvalg(behandlingstema, behandlingstype), frist8Uker),
            Pair.of(harFrist70Dager_Klager(behandlingstype), frist70Dager),
            Pair.of(harFrist90Dager_Søknadsbehandlinger(sakstema, behandlingstema, behandlingstype), frist90Dager),
            Pair.of(harFrist90Dager_AnmodningOmUnntak(behandlingstema, behandlingstype), frist90Dager),
            Pair.of(harFrist90Dager_AttesterFraAndreTrygdeavtaleland(behandlingstema, behandlingstype), frist90Dager),
            Pair.of(harFrist90Dager_Henvendelser(behandlingstype), frist90Dager),
            Pair.of(harFrist180Dager_MeldingOmUtstasjoneringEllerLovvalg(behandlingstema, behandlingstype), frist180Dager)
        );

        var behandlingsFrist = behandlingfrister.stream().filter(fristPair -> fristPair.getLeft() == true).findAny();

        if (!behandlingsFrist.isPresent()) {
            throw new FunksjonellException(String.format("Kunne ikke utlede behandlingsfrist for behandling med: sakstema %s, behandlingstema %s, behandlingstype %s", sakstema, behandlingstema, behandlingstype));
        }

        return behandlingsFrist.get().getRight();
    }

    private static boolean harFrist8Uker_Lovvalg(Behandlingstema behandlingsTema, Behandlingstyper behandlingstype) {
        // 1.1. Beslutning om norsk lovvalg/Beslutning om lovvalg i et annet land
        Set<Behandlingstema> behandlingstemaer = Set.of(BESLUTNING_LOVVALG_NORGE, BESLUTNING_LOVVALG_ANNET_LAND);
        Set<Behandlingstyper> behandlingstyper = Set.of(FØRSTEGANG, NY_VURDERING);

        return behandlingstemaer.contains(behandlingsTema) && behandlingstyper.contains(behandlingstype);
    }

    private static boolean harFrist70Dager_Klager(Behandlingstyper behandlingstype) {
        // 2.1. Klager
        Set<Behandlingstyper> behandlingstyper = Set.of(KLAGE);

        return behandlingstyper.contains(behandlingstype);
    }

    private static boolean harFrist90Dager_Søknadsbehandlinger(Sakstemaer sakstema, Behandlingstema behandlingstema, Behandlingstyper behandlingstype) {
        // 3.1 Søknadsbehandlinger
        Set<Sakstemaer> sakstemaer = Set.of(MEDLEMSKAP_LOVVALG, TRYGDEAVGIFT);
        Set<Behandlingstema> behandlingstemaer = Set.of(BESLUTNING_LOVVALG_ANNET_LAND);
        Set<Behandlingstyper> behandlingstyper = Set.of(FØRSTEGANG, NY_VURDERING, ENDRET_PERIODE);

        return sakstemaer.contains(sakstema) && !behandlingstemaer.contains(behandlingstema) && behandlingstyper.contains(behandlingstype);
    }

    private static boolean harFrist90Dager_AnmodningOmUnntak(Behandlingstema behandlingstema, Behandlingstyper behandlingstype) {
        // 3.2. Anmodning om unntak
        Set<Behandlingstema> behandlingstemaer = Set.of(ANMODNING_OM_UNNTAK_HOVEDREGEL);
        Set<Behandlingstyper> behandlingstyper = Set.of(FØRSTEGANG, NY_VURDERING);

        return behandlingstemaer.contains(behandlingstema) && behandlingstyper.contains(behandlingstype);
    }

    private static boolean harFrist90Dager_AttesterFraAndreTrygdeavtaleland(Behandlingstema behandlingstema, Behandlingstyper behandlingstype) {
        // 3.3 Attester fra andre land vi har trygdeavtale med
        Set<Behandlingstema> behandlingstemaer = Set.of(REGISTRERING_UNNTAK);
        Set<Behandlingstyper> behandlingstyper = Set.of(FØRSTEGANG, NY_VURDERING);

        return behandlingstemaer.contains(behandlingstema) && behandlingstyper.contains(behandlingstype);
    }

    private static boolean harFrist90Dager_Henvendelser(Behandlingstyper behandlingstype) {
        // 3.4. Henvendelser
        Set<Behandlingstyper> behandlingstyper = Set.of(HENVENDELSE);

        return behandlingstyper.contains(behandlingstype);
    }

    private static boolean harFrist180Dager_MeldingOmUtstasjoneringEllerLovvalg(Behandlingstema behandlingstema, Behandlingstyper behandlingstype) {
        // 4.1. Melding om utstasjonering/Melding om lovvalg
        Set<Behandlingstema> behandlingstemaer = Set.of(REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING, REGISTRERING_UNNTAK_NORSK_TRYGD_ØVRIGE);
        Set<Behandlingstyper> behandlingstyper = Set.of(FØRSTEGANG, NY_VURDERING);

        return behandlingstemaer.contains(behandlingstema) && behandlingstyper.contains(behandlingstype);
    }
}
