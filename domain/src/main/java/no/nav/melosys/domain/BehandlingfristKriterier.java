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
 * ref: https://confluence.adeo.no/display/TEESSI/Behandlingsfrister+i+Melosys
 */
public class BehandlingfristKriterier extends Behandling {

    public static LocalDate hentBehandlingsFrist(Sakstemaer sakstema, Behandlingstema behandlingstema, Behandlingstyper behandlingstype) {
        LocalDate frist8Uker = LocalDate.now().plusWeeks(8);
        LocalDate frist70Dager = LocalDate.now().plusDays(70);
        LocalDate frist90Dager = LocalDate.now().plusDays(90);
        LocalDate frist180Dager = LocalDate.now().plusDays(180);

        List<Pair<Boolean, LocalDate>> behandlingfrister = List.of(
            Pair.of(harFrist8UkerLovvalg(behandlingstema, behandlingstype), frist8Uker),
            Pair.of(harFrist70DagerKlager(behandlingstype), frist70Dager),
            Pair.of(harFrist90DagerSøknadsbehandlinger(sakstema, behandlingstema, behandlingstype), frist90Dager),
            Pair.of(harFrist90DagerAnmodningOmUnntak(behandlingstema, behandlingstype), frist90Dager),
            Pair.of(harFrist90DagerAttesterFraAndreTrygdeavtaleland(behandlingstema, behandlingstype), frist90Dager),
            Pair.of(harFrist90DagerHenvendelser(behandlingstype), frist90Dager),
            Pair.of(harFrist180DagerMeldingOmUtstasjoneringEllerLovvalg(behandlingstema, behandlingstype), frist180Dager)
        );

        var behandlingsFrist = behandlingfrister.stream().filter(fristPair -> fristPair.getLeft() == true).findAny();

        if (!behandlingsFrist.isPresent()) {
            throw new FunksjonellException(String.format("Kunne ikke utlede behandlingsfrist for behandling med: sakstema %s, behandlingstema %s, behandlingstype %s", sakstema, behandlingstema, behandlingstype));
        }

        return behandlingsFrist.get().getRight();
    }

    private static boolean harFrist8UkerLovvalg(Behandlingstema behandlingsTema, Behandlingstyper behandlingstype) {
        Set<Behandlingstema> behandlingstemaer = Set.of(BESLUTNING_LOVVALG_NORGE, BESLUTNING_LOVVALG_ANNET_LAND);
        Set<Behandlingstyper> behandlingstyper = Set.of(FØRSTEGANG, NY_VURDERING);

        return behandlingstemaer.contains(behandlingsTema) && behandlingstyper.contains(behandlingstype);
    }

    private static boolean harFrist70DagerKlager(Behandlingstyper behandlingstype) {
        Set<Behandlingstyper> behandlingstyper = Set.of(KLAGE);

        return behandlingstyper.contains(behandlingstype);
    }

    private static boolean harFrist90DagerSøknadsbehandlinger(Sakstemaer sakstema, Behandlingstema behandlingstema, Behandlingstyper behandlingstype) {
        Set<Sakstemaer> sakstemaer = Set.of(MEDLEMSKAP_LOVVALG, TRYGDEAVGIFT);
        Set<Behandlingstema> behandlingstemaer = Set.of(BESLUTNING_LOVVALG_ANNET_LAND);
        Set<Behandlingstyper> behandlingstyper = Set.of(FØRSTEGANG, NY_VURDERING, ENDRET_PERIODE);

        return sakstemaer.contains(sakstema) && !behandlingstemaer.contains(behandlingstema) && behandlingstyper.contains(behandlingstype);
    }

    private static boolean harFrist90DagerAnmodningOmUnntak(Behandlingstema behandlingstema, Behandlingstyper behandlingstype) {
        Set<Behandlingstema> behandlingstemaer = Set.of(ANMODNING_OM_UNNTAK_HOVEDREGEL);
        Set<Behandlingstyper> behandlingstyper = Set.of(FØRSTEGANG, NY_VURDERING);

        return behandlingstemaer.contains(behandlingstema) && behandlingstyper.contains(behandlingstype);
    }

    private static boolean harFrist90DagerAttesterFraAndreTrygdeavtaleland(Behandlingstema behandlingstema, Behandlingstyper behandlingstype) {
        Set<Behandlingstema> behandlingstemaer = Set.of(REGISTRERING_UNNTAK);
        Set<Behandlingstyper> behandlingstyper = Set.of(FØRSTEGANG, NY_VURDERING);

        return behandlingstemaer.contains(behandlingstema) && behandlingstyper.contains(behandlingstype);
    }

    private static boolean harFrist90DagerHenvendelser(Behandlingstyper behandlingstype) {
        Set<Behandlingstyper> behandlingstyper = Set.of(HENVENDELSE);

        return behandlingstyper.contains(behandlingstype);
    }

    private static boolean harFrist180DagerMeldingOmUtstasjoneringEllerLovvalg(Behandlingstema behandlingstema, Behandlingstyper behandlingstype) {
        Set<Behandlingstema> behandlingstemaer = Set.of(REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING, REGISTRERING_UNNTAK_NORSK_TRYGD_ØVRIGE);
        Set<Behandlingstyper> behandlingstyper = Set.of(FØRSTEGANG, NY_VURDERING);

        return behandlingstemaer.contains(behandlingstema) && behandlingstyper.contains(behandlingstype);
    }
}
