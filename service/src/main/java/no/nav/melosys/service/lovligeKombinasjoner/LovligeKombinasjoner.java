package no.nav.melosys.service.lovligeKombinasjoner;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Saksstatuser;
import no.nav.melosys.domain.kodeverk.Sakstemaer;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.service.sak.SakKombinasjon;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.kodeverk.Saksstatuser.*;
import static no.nav.melosys.domain.kodeverk.Sakstemaer.*;
import static no.nav.melosys.domain.kodeverk.Sakstyper.*;
import static no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema.*;
import static no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper.*;
import static no.nav.melosys.service.lovligeKombinasjoner.LovligeBehandlingsKombinasjoner.*;

@Component
public class LovligeKombinasjoner {
    private static final Set<Sakstyper> ALLE_MULIGE_SAKSTYPER = Set.of(EU_EOS, FTRL, TRYGDEAVTALE);
    private static final Set<Sakstemaer> ALLE_MULIGE_SAKSTEMAER = Set.of(MEDLEMSKAP_LOVVALG, UNNTAK, TRYGDEAVGIFT);
    private static final SakKombinasjon EU_EOS_LOVVALG_MEDLEMSKAP_SAK = new SakKombinasjon(MEDLEMSKAP_LOVVALG, Set.of(EU_EOS_LOVVALG_MEDLEMSKAP_BEHANDLINGS_KOMBINASJON_1, EU_EOS_LOVVALG_MEDLEMSKAP_BEHANDLINGS_KOMBINASJON_2));
    private static final SakKombinasjon EU_EOS_UNNTAK_SAK = new SakKombinasjon(UNNTAK, Set.of(EU_EOS_UNNTAK_BEHANDLINGS_KOMBINASJON));
    private static final SakKombinasjon EU_EOS_TRYGDEAVGIFT_SAK = new SakKombinasjon(TRYGDEAVGIFT, Set.of(EU_EOS_TRYGDEAVGIFT_BEHANDLINGS_KOMBINASJON));
    private static final SakKombinasjon FOLKETRYGDLOVEL_LOVVALG_MEDLEMSKAP_SAK = new SakKombinasjon(MEDLEMSKAP_LOVVALG, Set.of(FTRL_LOVVALG_MEDLEMSKAP_BEHANDLINGS_KOMBINASJON));
    private static final SakKombinasjon FOLKETRYGDLOVEL_TRYGDEAVGIFT_SAK = new SakKombinasjon(TRYGDEAVGIFT, Set.of(FTRL_TRYGDEAVGIFT_BEHANDLINGS_KOMBINASJON));
    private static final SakKombinasjon TRYGDEAVTALE_LOVVALG_MEDLEMSKAP_SAK = new SakKombinasjon(MEDLEMSKAP_LOVVALG, Set.of(TRYGDEAVTALE_LOVVALG_MEDLEMSKAP_BEHANDLINGS_KOMBINASJON_1, TRYGDEAVTALE_LOVVALG_MEDLEMSKAP_BEHANDLINGS_KOMBINASJON_2));
    private static final SakKombinasjon TRYGDEAVTALE_UNNTAK_SAK = new SakKombinasjon(UNNTAK, Set.of(TRYGDEAVTALE_UNNTAK_BEHANDLINGS_KOMBINASJON_1, TRYGDEAVTALE_UNNTAK_BEHANDLINGS_KOMBINASJON_2));
    private static final SakKombinasjon TRYGDEAVTALE_TRYGDEAVGIFT_SAK = new SakKombinasjon(TRYGDEAVGIFT, Set.of(TRYGDEAVTALE_TRYGDEAVGIFT_BEHANDLINGS_KOMBINASJON));
    private static final Map<Sakstyper, Set<SakKombinasjon>> alleMuligeSaksKombinasjonerBruker = new HashMap<>();

    public LovligeKombinasjoner() {
        alleMuligeSaksKombinasjonerBruker.put(EU_EOS, Set.of(EU_EOS_LOVVALG_MEDLEMSKAP_SAK, EU_EOS_UNNTAK_SAK, EU_EOS_TRYGDEAVGIFT_SAK));
        alleMuligeSaksKombinasjonerBruker.put(FTRL, Set.of(FOLKETRYGDLOVEL_LOVVALG_MEDLEMSKAP_SAK, FOLKETRYGDLOVEL_TRYGDEAVGIFT_SAK));
        alleMuligeSaksKombinasjonerBruker.put(TRYGDEAVTALE, Set.of(TRYGDEAVTALE_LOVVALG_MEDLEMSKAP_SAK, TRYGDEAVTALE_UNNTAK_SAK, TRYGDEAVTALE_TRYGDEAVGIFT_SAK));
    }

    public static Set<Sakstemaer> hentAlleSakstemaer(Aktoersroller hovedpart, Sakstyper sakstype) {
        switch (hovedpart) {
            case BRUKER:
                return alleMuligeSaksKombinasjonerBruker.get(sakstype).stream()
                    .map(sak -> sak.getSakstema())
                    .collect(Collectors.toSet());
            case VIRKSOMHET:
                return ALLE_MULIGE_SAKSTEMAER;
            default:
                return Collections.emptySet();
        }
    }

    public static Set<Behandlingstema> hentAlleMuligeBehandlingstemaer(
        Aktoersroller hovedpart,
        Sakstyper sakstype,
        Sakstemaer sakstema,
        @Nullable Behandlingstema sistBehandlingstema
    ) {
        switch (hovedpart) {
            case BRUKER:
                Set<Behandlingstema> behandlingstemaer = alleMuligeSaksKombinasjonerBruker.get(sakstype).stream()
                    .filter(sakKombinasjon -> sakKombinasjon.getSakstema() == sakstema)
                    .flatMap(sakKombinasjon -> sakKombinasjon.getBehandlingsKombinasjoner().stream())
                    .flatMap(behandlingsKombinasjon -> behandlingsKombinasjon.behandlingsTemaer().stream())
                    .collect(Collectors.toSet());

                if (sistBehandlingstema != null && Set.of(REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING,
                    REGISTRERING_UNNTAK_NORSK_TRYGD_ØVRIGE,
                    BESLUTNING_LOVVALG_NORGE,
                    BESLUTNING_LOVVALG_ANNET_LAND,
                    ANMODNING_OM_UNNTAK_HOVEDREGEL).contains(sistBehandlingstema)) {
                    return Set.of(sistBehandlingstema);
                }

                return behandlingstemaer;
            case VIRKSOMHET:
                return Collections.emptySet();
            default:
                return Collections.emptySet();
        }
    }

    public static Set<Behandlingstyper> hentAlleMuligeBehandlingstyper(
        Aktoersroller hovedpart,
        Sakstyper sakstype,
        Sakstemaer sakstema,
        Behandlingstema behandlingstema,
        @Nullable Behandlingstema sistBehandlingstema,
        @Nullable Behandlingstyper sistBehandlingstype,
        @Nullable Saksstatuser saksstatus
    ) {
        switch (hovedpart) {
            case BRUKER:
                Set<Behandlingstyper> behandlingstyper = alleMuligeSaksKombinasjonerBruker.get(sakstype).stream()
                    .filter(sakKombinasjon -> sakKombinasjon.getSakstema() == sakstema)
                    .flatMap(sakKombinasjon -> sakKombinasjon.getBehandlingsKombinasjoner().stream())
                    .filter(behandlingsKombinasjon -> behandlingsKombinasjon.behandlingsTemaer().contains(behandlingstema))
                    .flatMap(behandlingsKombinasjon -> behandlingsKombinasjon.behandlingsTyper().stream())
                    .collect(Collectors.toSet());

                if (sistBehandlingstema != null && Set.of(REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING,
                    REGISTRERING_UNNTAK_NORSK_TRYGD_ØVRIGE,
                    BESLUTNING_LOVVALG_NORGE,
                    BESLUTNING_LOVVALG_ANNET_LAND,
                    ANMODNING_OM_UNNTAK_HOVEDREGEL).contains(sistBehandlingstema)) {
                    behandlingstyper = Set.of(NY_VURDERING, KLAGE, HENVENDELSE);
                }

                if (sistBehandlingstema != null &&
                    sistBehandlingstype != null &&
                    sistBehandlingstype == FØRSTEGANG &&
                    Set.of(UTSENDT_ARBEIDSTAKER, UTSENDT_SELVSTENDIG).contains(sistBehandlingstema)
                ) {
                    behandlingstyper.add(ENDRET_PERIODE);
                }

                if (saksstatus != null && Set.of(HENLAGT, HENLAGT_BORTFALT, AVSLUTTET).contains(saksstatus)) {
                    return Set.of(HENVENDELSE);
                }

                return behandlingstyper;
            case VIRKSOMHET:
                return BEHANDLINGSTYPER_FOR_VIRKSOMHET;
            default:
                return Collections.emptySet();
        }
    }

    public static Set<Sakstyper> hentAlleMuligeSakstyper() {
        return ALLE_MULIGE_SAKSTYPER;
    }
}

