package no.nav.melosys.service.lovligekombinasjoner;

import java.util.*;

import no.nav.melosys.domain.kodeverk.Sakstemaer;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;

import static no.nav.melosys.domain.kodeverk.Sakstemaer.*;
import static no.nav.melosys.domain.kodeverk.Sakstyper.*;
import static no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema.*;
import static no.nav.melosys.service.lovligekombinasjoner.LovligeBehandlingsKombinasjoner.*;

public class LovligeSakskombinasjoner {
    static final Set<Sakstyper> ALLE_MULIGE_SAKSTYPER = new LinkedHashSet<>(List.of(EU_EOS, TRYGDEAVTALE, FTRL));

    private static final SakstemaBehandlingsKombinasjon EU_EOS_LOVVALG_MEDLEMSKAP_SAK = new SakstemaBehandlingsKombinasjon(MEDLEMSKAP_LOVVALG, new LinkedHashSet<>(List.of(EU_EOS_LOVVALG_MEDLEMSKAP_BEHANDLINGS_KOMBINASJON_SØKNAD, EU_EOS_LOVVALG_MEDLEMSKAP_BEHANDLINGS_KOMBINASJON_HENVENDELSER)));
    private static final SakstemaBehandlingsKombinasjon EU_EOS_UNNTAK_SAK = new SakstemaBehandlingsKombinasjon(UNNTAK, Set.of(EU_EOS_UNNTAK_BEHANDLINGS_KOMBINASJON, EU_EOS_UNNTAK_A1_PAPIR_BEHANDLINGS_KOMBINASJON));
    private static final SakstemaBehandlingsKombinasjon EU_EOS_TRYGDEAVGIFT_SAK = new SakstemaBehandlingsKombinasjon(TRYGDEAVGIFT, Set.of(EU_EOS_TRYGDEAVGIFT_BEHANDLINGS_KOMBINASJON, EU_EOS_TRYGDEAVGIFT_PENSJONIST_BEHANDLINGS_KOMBINASJON));
    private static final SakstemaBehandlingsKombinasjon EU_EOS_TRYGDEAVGIFT_PENSJONIST_SAK = new SakstemaBehandlingsKombinasjon(TRYGDEAVGIFT, Set.of(EU_EOS_TRYGDEAVGIFT_PENSJONIST_BEHANDLINGS_KOMBINASJON));
    private static final SakstemaBehandlingsKombinasjon FOLKETRYGDLOVEL_LOVVALG_MEDLEMSKAP_SAK = new SakstemaBehandlingsKombinasjon(MEDLEMSKAP_LOVVALG, new LinkedHashSet<>(List.of(FTRL_LOVVALG_MEDLEMSKAP_BEHANDLINGS_KOMBINASJON, FTRL_ÅRSAVREGNING_BEHANDLINGS_KOMBINASJON)));
    private static final SakstemaBehandlingsKombinasjon FOLKETRYGDLOVEL_TRYGDEAVGIFT_SAK = new SakstemaBehandlingsKombinasjon(TRYGDEAVGIFT, new LinkedHashSet<>(List.of(FTRL_TRYGDEAVGIFT_BEHANDLINGS_KOMBINASJON)));
    private static final SakstemaBehandlingsKombinasjon TRYGDEAVTALE_LOVVALG_MEDLEMSKAP_SAK = new SakstemaBehandlingsKombinasjon(MEDLEMSKAP_LOVVALG, new LinkedHashSet<>(List.of(TRYGDEAVTALE_LOVVALG_MEDLEMSKAP_BEHANDLINGS_KOMBINASJON_1, TRYGDEAVTALE_LOVVALG_MEDLEMSKAP_BEHANDLINGS_KOMBINASJON_2)));
    private static final SakstemaBehandlingsKombinasjon TRYGDEAVTALE_UNNTAK_SAK = new SakstemaBehandlingsKombinasjon(UNNTAK, new LinkedHashSet<>(List.of(TRYGDEAVTALE_UNNTAK_BEHANDLINGS_KOMBINASJON_1, TRYGDEAVTALE_UNNTAK_BEHANDLINGS_KOMBINASJON_2)));
    private static final SakstemaBehandlingsKombinasjon TRYGDEAVTALE_TRYGDEAVGIFT_SAK = new SakstemaBehandlingsKombinasjon(TRYGDEAVGIFT, Set.of(TRYGDEAVTALE_TRYGDEAVGIFT_BEHANDLINGS_KOMBINASJON));

    private static final SakstemaBehandlingsKombinasjon EU_EOS_LOVVALG_MEDLEMSKAP_SAK_VIRKSOMHET = new SakstemaBehandlingsKombinasjon(MEDLEMSKAP_LOVVALG, new LinkedHashSet<>(List.of(EU_EOS_LOVVALG_MEDLEMSKAP_BEHANDLINGS_KOMBINASJON_VIRKSOMHET)));
    private static final SakstemaBehandlingsKombinasjon EU_EOS_UNNTAK_SAK_VIRKSOMHET = new SakstemaBehandlingsKombinasjon(UNNTAK, new LinkedHashSet<>(List.of(EU_EOS_UNNTAK_BEHANDLINGS_KOMBINASJON_VIRKSOMHET)));
    private static final SakstemaBehandlingsKombinasjon EU_EOS_TRYGDEAVGIFT_SAK_VIRKSOMHET = new SakstemaBehandlingsKombinasjon(TRYGDEAVGIFT, new LinkedHashSet<>(List.of(EU_EOS_TRYGDEAVGIFT_BEHANDLINGS_KOMBINASJON_VIRKSOMHET)));

    private static final SakstemaBehandlingsKombinasjon FOLKETRYGDLOVEN_LOVVALG_MEDLEMSKAP_SAK_VIRKSOMHET = new SakstemaBehandlingsKombinasjon(MEDLEMSKAP_LOVVALG, new LinkedHashSet<>(List.of(FOLKETRYGDLOVEN_LOVVALG_MEDLEMSKAP_BEHANDLINGS_KOMBINASJON_VIRKSOMHET)));
    private static final SakstemaBehandlingsKombinasjon FOLKETRYGDLOVEN_TRYGDEAVGIFT_SAK_VIRKSOMHET = new SakstemaBehandlingsKombinasjon(TRYGDEAVGIFT, new LinkedHashSet<>(List.of(FOLKETRYGDLOVEN_TRYGDEAVGIFT_BEHANDLINGS_KOMBINASJON_VIRKSOMHET)));

    private static final SakstemaBehandlingsKombinasjon TRYGDEAVTALE_LOVVALG_MEDLEMSKAP_SAK_VIRKSOMHET = new SakstemaBehandlingsKombinasjon(MEDLEMSKAP_LOVVALG, new LinkedHashSet<>(List.of(TRYGDEAVTALE_LOVVALG_MEDLEMSKAP_BEHANDLINGS_KOMBINASJON_VIRKSOMHET)));
    private static final SakstemaBehandlingsKombinasjon TRYGDEAVTALE_UNNTAK_SAK_VIRKSOMHET = new SakstemaBehandlingsKombinasjon(UNNTAK, new LinkedHashSet<>(List.of(TRYGDEAVTALE_UNNTAK_BEHANDLINGS_KOMBINASJON_VIRKSOMHET)));
    private static final SakstemaBehandlingsKombinasjon TRYGDEAVTALE_TRYGDEAVGIFT_SAK_VIRKSOMHET = new SakstemaBehandlingsKombinasjon(TRYGDEAVGIFT, new LinkedHashSet<>(List.of(TRYGDEAVTALE_TRYGDEAVGIFT_BEHANDLINGS_KOMBINASJON_VIRKSOMHET)));

    static final Map<Sakstyper, Set<SakstemaBehandlingsKombinasjon>> muligeSaksKombinasjonerBruker = new EnumMap<>(Sakstyper.class);
    static final Map<Sakstyper, Set<SakstemaBehandlingsKombinasjon>> muligeSaksKombinasjonerVirksomhet = new EnumMap<>(Sakstyper.class);

    static final Map<Sakstemaer, Set<Behandlingstema>> EU_EOS_SED_BEHANDLINGSTEMA = new EnumMap<>(Sakstemaer.class);

    static {
        muligeSaksKombinasjonerBruker.put(EU_EOS, new LinkedHashSet<>(List.of(EU_EOS_LOVVALG_MEDLEMSKAP_SAK, EU_EOS_UNNTAK_SAK, EU_EOS_TRYGDEAVGIFT_SAK, EU_EOS_TRYGDEAVGIFT_PENSJONIST_SAK)));
        muligeSaksKombinasjonerBruker.put(FTRL, new LinkedHashSet<>(List.of(FOLKETRYGDLOVEL_LOVVALG_MEDLEMSKAP_SAK, FOLKETRYGDLOVEL_TRYGDEAVGIFT_SAK)));
        muligeSaksKombinasjonerBruker.put(TRYGDEAVTALE, new LinkedHashSet<>(List.of(TRYGDEAVTALE_LOVVALG_MEDLEMSKAP_SAK, TRYGDEAVTALE_UNNTAK_SAK, TRYGDEAVTALE_TRYGDEAVGIFT_SAK)));

        muligeSaksKombinasjonerVirksomhet.put(EU_EOS, new LinkedHashSet<>(List.of(EU_EOS_LOVVALG_MEDLEMSKAP_SAK_VIRKSOMHET, EU_EOS_UNNTAK_SAK_VIRKSOMHET, EU_EOS_TRYGDEAVGIFT_SAK_VIRKSOMHET)));
        muligeSaksKombinasjonerVirksomhet.put(FTRL, new LinkedHashSet<>(List.of(FOLKETRYGDLOVEN_LOVVALG_MEDLEMSKAP_SAK_VIRKSOMHET, FOLKETRYGDLOVEN_TRYGDEAVGIFT_SAK_VIRKSOMHET)));
        muligeSaksKombinasjonerVirksomhet.put(TRYGDEAVTALE, new LinkedHashSet<>(List.of(TRYGDEAVTALE_LOVVALG_MEDLEMSKAP_SAK_VIRKSOMHET, TRYGDEAVTALE_UNNTAK_SAK_VIRKSOMHET, TRYGDEAVTALE_TRYGDEAVGIFT_SAK_VIRKSOMHET)));

        EU_EOS_SED_BEHANDLINGSTEMA.put(MEDLEMSKAP_LOVVALG, new HashSet<>(List.of(BESLUTNING_LOVVALG_NORGE)));
        EU_EOS_SED_BEHANDLINGSTEMA.put(UNNTAK, new LinkedHashSet<>(List.of(REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING, REGISTRERING_UNNTAK_NORSK_TRYGD_ØVRIGE, BESLUTNING_LOVVALG_ANNET_LAND, ANMODNING_OM_UNNTAK_HOVEDREGEL)));
    }

    private LovligeSakskombinasjoner() {
    }
}

