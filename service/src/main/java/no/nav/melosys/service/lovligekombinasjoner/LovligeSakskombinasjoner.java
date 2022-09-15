package no.nav.melosys.service.lovligekombinasjoner;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

import no.nav.melosys.domain.kodeverk.Sakstemaer;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;

import static no.nav.melosys.domain.kodeverk.Sakstemaer.*;
import static no.nav.melosys.domain.kodeverk.Sakstyper.*;
import static no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema.*;
import static no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL;
import static no.nav.melosys.service.lovligekombinasjoner.LovligeBehandlingsKombinasjoner.*;

public class LovligeSakskombinasjoner {
    static final Set<Sakstyper> ALLE_MULIGE_SAKSTYPER = Set.of(EU_EOS, FTRL, TRYGDEAVTALE);
    static final Set<Sakstemaer> ALLE_MULIGE_SAKSTEMAER = Set.of(MEDLEMSKAP_LOVVALG, UNNTAK, TRYGDEAVGIFT);
    static final Set<Behandlingstema> SED_BEHANDLINGSTEMA = Set.of(REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING, REGISTRERING_UNNTAK_NORSK_TRYGD_ØVRIGE, BESLUTNING_LOVVALG_ANNET_LAND, ANMODNING_OM_UNNTAK_HOVEDREGEL, FORESPØRSEL_TRYGDEMYNDIGHET);

    private static final SakstemaBehandlingsKombinasjon EU_EOS_LOVVALG_MEDLEMSKAP_SAK = new SakstemaBehandlingsKombinasjon(MEDLEMSKAP_LOVVALG, Set.of(EU_EOS_LOVVALG_MEDLEMSKAP_BEHANDLINGS_KOMBINASJON_SØKNAD, EU_EOS_LOVVALG_MEDLEMSKAP_BEHANDLINGS_KOMBINASJON_HENVENDELSER));
    private static final SakstemaBehandlingsKombinasjon EU_EOS_UNNTAK_SAK = new SakstemaBehandlingsKombinasjon(UNNTAK, Set.of(EU_EOS_UNNTAK_BEHANDLINGS_KOMBINASJON));
    private static final SakstemaBehandlingsKombinasjon EU_EOS_TRYGDEAVGIFT_SAK = new SakstemaBehandlingsKombinasjon(TRYGDEAVGIFT, Set.of(EU_EOS_TRYGDEAVGIFT_BEHANDLINGS_KOMBINASJON));
    private static final SakstemaBehandlingsKombinasjon FOLKETRYGDLOVEL_LOVVALG_MEDLEMSKAP_SAK = new SakstemaBehandlingsKombinasjon(MEDLEMSKAP_LOVVALG, Set.of(FTRL_LOVVALG_MEDLEMSKAP_BEHANDLINGS_KOMBINASJON));
    private static final SakstemaBehandlingsKombinasjon FOLKETRYGDLOVEL_TRYGDEAVGIFT_SAK = new SakstemaBehandlingsKombinasjon(TRYGDEAVGIFT, Set.of(FTRL_TRYGDEAVGIFT_BEHANDLINGS_KOMBINASJON));
    private static final SakstemaBehandlingsKombinasjon TRYGDEAVTALE_LOVVALG_MEDLEMSKAP_SAK = new SakstemaBehandlingsKombinasjon(MEDLEMSKAP_LOVVALG, Set.of(TRYGDEAVTALE_LOVVALG_MEDLEMSKAP_BEHANDLINGS_KOMBINASJON_1, TRYGDEAVTALE_LOVVALG_MEDLEMSKAP_BEHANDLINGS_KOMBINASJON_2));
    private static final SakstemaBehandlingsKombinasjon TRYGDEAVTALE_UNNTAK_SAK = new SakstemaBehandlingsKombinasjon(UNNTAK, Set.of(TRYGDEAVTALE_UNNTAK_BEHANDLINGS_KOMBINASJON_1, TRYGDEAVTALE_UNNTAK_BEHANDLINGS_KOMBINASJON_2));
    private static final SakstemaBehandlingsKombinasjon TRYGDEAVTALE_TRYGDEAVGIFT_SAK = new SakstemaBehandlingsKombinasjon(TRYGDEAVGIFT, Set.of(TRYGDEAVTALE_TRYGDEAVGIFT_BEHANDLINGS_KOMBINASJON));

    static final Map<Sakstyper, Set<SakstemaBehandlingsKombinasjon>> muligeSaksKombinasjonerBruker = new EnumMap<>(Sakstyper.class);

    static {
        muligeSaksKombinasjonerBruker.put(EU_EOS, Set.of(EU_EOS_LOVVALG_MEDLEMSKAP_SAK, EU_EOS_UNNTAK_SAK, EU_EOS_TRYGDEAVGIFT_SAK));
        muligeSaksKombinasjonerBruker.put(FTRL, Set.of(FOLKETRYGDLOVEL_LOVVALG_MEDLEMSKAP_SAK, FOLKETRYGDLOVEL_TRYGDEAVGIFT_SAK));
        muligeSaksKombinasjonerBruker.put(TRYGDEAVTALE, Set.of(TRYGDEAVTALE_LOVVALG_MEDLEMSKAP_SAK, TRYGDEAVTALE_UNNTAK_SAK, TRYGDEAVTALE_TRYGDEAVGIFT_SAK));
    }

    private LovligeSakskombinasjoner() {
    }
}

