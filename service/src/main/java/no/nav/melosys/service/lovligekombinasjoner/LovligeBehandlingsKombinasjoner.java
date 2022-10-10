package no.nav.melosys.service.lovligekombinasjoner;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;

import static no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema.*;
import static no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper.*;

// Alle mulige kombinasjoner ved endring og oppretting av sak og behandling.
// ref: https://confluence.adeo.no/display/TEESSI/Lovlige+kombinasjoner+av+sakstype%2C+sakstema%2C+behandlingstype+og+behandlingstema
public class LovligeBehandlingsKombinasjoner {
    // EU_EØS
    private static final Set<Behandlingstema> EU_EOS_LOVVALG_MEDLEMSKAP_BEHANDLINGSTEMA_SØKNAD = new LinkedHashSet<>(List.of(UTSENDT_ARBEIDSTAKER, UTSENDT_SELVSTENDIG, ARBEID_FLERE_LAND, ARBEID_TJENESTEPERSON_ELLER_FLY, ARBEID_KUN_NORGE, IKKE_YRKESAKTIV, PENSJONIST));
    private static final Set<Behandlingstema> EU_EOS_LOVVALG_MEDLEMSKAP_BEHANDLINGSTEMA_HENVENDELSER = new LinkedHashSet<>(List.of(FORESPØRSEL_TRYGDEMYNDIGHET, TRYGDETID));
    private static final Set<Behandlingstema> EU_EOS_UNNTAK_BEHANDLINGSTEMA = Set.of(FORESPØRSEL_TRYGDEMYNDIGHET);
    private static final Set<Behandlingstema> EU_EOS_TRYGDEAVGIFT_BEHANDLINGSTEMA = new LinkedHashSet<>(List.of(YRKESAKTIV, PENSJONIST));
    private static final Set<Behandlingstyper> EU_EOS_LOVVALG_MEDLEMSKAP_BEHANDLINGSTYPE_SØKNAD = new LinkedHashSet<>(List.of(FØRSTEGANG, NY_VURDERING, KLAGE, HENVENDELSE));
    private static final Set<Behandlingstyper> EU_EOS_LOVVALG_MEDLEMSKAP_BEHANDLINGSTYPE_HENVENDELSER = Set.of(HENVENDELSE);
    private static final Set<Behandlingstyper> EU_EOS_UNNTAK_BEHANDLINGSTYPE = Set.of(HENVENDELSE);
    private static final Set<Behandlingstyper> EU_EOS_TRYGDEAVGIFT_BEHANDLINGSTYPE = new LinkedHashSet<>(List.of(FØRSTEGANG, NY_VURDERING, KLAGE, HENVENDELSE));
    public static final BehandlingstemaBehandlingstyperKombinasjon EU_EOS_LOVVALG_MEDLEMSKAP_BEHANDLINGS_KOMBINASJON_SØKNAD = new BehandlingstemaBehandlingstyperKombinasjon(EU_EOS_LOVVALG_MEDLEMSKAP_BEHANDLINGSTEMA_SØKNAD, EU_EOS_LOVVALG_MEDLEMSKAP_BEHANDLINGSTYPE_SØKNAD);
    public static final BehandlingstemaBehandlingstyperKombinasjon EU_EOS_LOVVALG_MEDLEMSKAP_BEHANDLINGS_KOMBINASJON_HENVENDELSER = new BehandlingstemaBehandlingstyperKombinasjon(EU_EOS_LOVVALG_MEDLEMSKAP_BEHANDLINGSTEMA_HENVENDELSER, EU_EOS_LOVVALG_MEDLEMSKAP_BEHANDLINGSTYPE_HENVENDELSER);
    public static final BehandlingstemaBehandlingstyperKombinasjon EU_EOS_UNNTAK_BEHANDLINGS_KOMBINASJON = new BehandlingstemaBehandlingstyperKombinasjon(EU_EOS_UNNTAK_BEHANDLINGSTEMA, EU_EOS_UNNTAK_BEHANDLINGSTYPE);
    public static final BehandlingstemaBehandlingstyperKombinasjon EU_EOS_TRYGDEAVGIFT_BEHANDLINGS_KOMBINASJON = new BehandlingstemaBehandlingstyperKombinasjon(EU_EOS_TRYGDEAVGIFT_BEHANDLINGSTEMA, EU_EOS_TRYGDEAVGIFT_BEHANDLINGSTYPE);

    // FOLKETRYGDLOVEN
    private static final Set<Behandlingstema> FTRL_LOVVALG_MEDLEMSKAP_BEHANDLINGSTEMA = new LinkedHashSet<>(List.of(YRKESAKTIV, IKKE_YRKESAKTIV, PENSJONIST, UNNTAK_MEDLEMSKAP));
    private static final Set<Behandlingstema> FTRL_TRYGDEAVGIFT_BEHANDLINGSTEMA = new LinkedHashSet<>(List.of(YRKESAKTIV, PENSJONIST));
    private static final Set<Behandlingstyper> FTRL_LOVVALG_MEDLEMSKAP_BEHANDLINGSTYPE = new LinkedHashSet<>(List.of(FØRSTEGANG, NY_VURDERING, KLAGE, HENVENDELSE));
    private static final Set<Behandlingstyper> FTRL_TRYGDEAVGIFT_BEHANDLINGSTYPE = new LinkedHashSet<>(List.of(FØRSTEGANG, NY_VURDERING, KLAGE, HENVENDELSE));
    public static final BehandlingstemaBehandlingstyperKombinasjon FTRL_LOVVALG_MEDLEMSKAP_BEHANDLINGS_KOMBINASJON = new BehandlingstemaBehandlingstyperKombinasjon(FTRL_LOVVALG_MEDLEMSKAP_BEHANDLINGSTEMA, FTRL_LOVVALG_MEDLEMSKAP_BEHANDLINGSTYPE);
    public static final BehandlingstemaBehandlingstyperKombinasjon FTRL_TRYGDEAVGIFT_BEHANDLINGS_KOMBINASJON = new BehandlingstemaBehandlingstyperKombinasjon(FTRL_TRYGDEAVGIFT_BEHANDLINGSTEMA, FTRL_TRYGDEAVGIFT_BEHANDLINGSTYPE);

    // TRYGDEAVTALE
    private static final Set<Behandlingstema> TRYGDEAVTALE_LOVVALG_MEDLEMSKAP_BEHANDLINGSTEMA_1 = new LinkedHashSet<>(List.of(YRKESAKTIV, IKKE_YRKESAKTIV, PENSJONIST));
    private static final Set<Behandlingstema> TRYGDEAVTALE_LOVVALG_MEDLEMSKAP_BEHANDLINGSTEMA_2 = Set.of(FORESPØRSEL_TRYGDEMYNDIGHET);
    private static final Set<Behandlingstema> TRYGDEAVTALE_UNNTAK_BEHANDLINGSTEMA_1 = new LinkedHashSet<>(List.of(ANMODNING_OM_UNNTAK_HOVEDREGEL, REGISTRERING_UNNTAK));
    private static final Set<Behandlingstema> TRYGDEAVTALE_UNNTAK_BEHANDLINGSTEMA_2 = Set.of(FORESPØRSEL_TRYGDEMYNDIGHET);
    private static final Set<Behandlingstema> TRYGDEAVTALE_TRYGDEAVGIFT_BEHANDLINGSTEMA = new LinkedHashSet<>(List.of(YRKESAKTIV, PENSJONIST));
    private static final Set<Behandlingstyper> TRYGDEAVTALE_LOVVALG_MEDLEMSKAP_BEHANDLINGSTYPE_1 = new LinkedHashSet<>(List.of(FØRSTEGANG, NY_VURDERING, KLAGE, HENVENDELSE));
    private static final Set<Behandlingstyper> TRYGDEAVTALE_LOVVALG_MEDLEMSKAP_BEHANDLINGSTYPE_2 = Set.of(HENVENDELSE);
    private static final Set<Behandlingstyper> TRYGDEAVTALE_UNNTAK_BEHANDLINGSTYPE_1 = new LinkedHashSet<>(List.of(FØRSTEGANG, NY_VURDERING, KLAGE, HENVENDELSE));
    private static final Set<Behandlingstyper> TRYGDEAVTALE_UNNTAK_BEHANDLINGSTYPE_2 = Set.of(HENVENDELSE);
    private static final Set<Behandlingstyper> TRYGDEAVTALE_TRYGDEAVGIFT_BEHANDLINGSTYPE = new LinkedHashSet<>(List.of(FØRSTEGANG, NY_VURDERING, KLAGE, HENVENDELSE));
    public static final BehandlingstemaBehandlingstyperKombinasjon TRYGDEAVTALE_LOVVALG_MEDLEMSKAP_BEHANDLINGS_KOMBINASJON_1 = new BehandlingstemaBehandlingstyperKombinasjon(TRYGDEAVTALE_LOVVALG_MEDLEMSKAP_BEHANDLINGSTEMA_1, TRYGDEAVTALE_LOVVALG_MEDLEMSKAP_BEHANDLINGSTYPE_1);
    public static final BehandlingstemaBehandlingstyperKombinasjon TRYGDEAVTALE_LOVVALG_MEDLEMSKAP_BEHANDLINGS_KOMBINASJON_2 = new BehandlingstemaBehandlingstyperKombinasjon(TRYGDEAVTALE_LOVVALG_MEDLEMSKAP_BEHANDLINGSTEMA_2, TRYGDEAVTALE_LOVVALG_MEDLEMSKAP_BEHANDLINGSTYPE_2);
    public static final BehandlingstemaBehandlingstyperKombinasjon TRYGDEAVTALE_UNNTAK_BEHANDLINGS_KOMBINASJON_1 = new BehandlingstemaBehandlingstyperKombinasjon(TRYGDEAVTALE_UNNTAK_BEHANDLINGSTEMA_1, TRYGDEAVTALE_UNNTAK_BEHANDLINGSTYPE_1);
    public static final BehandlingstemaBehandlingstyperKombinasjon TRYGDEAVTALE_UNNTAK_BEHANDLINGS_KOMBINASJON_2 = new BehandlingstemaBehandlingstyperKombinasjon(TRYGDEAVTALE_UNNTAK_BEHANDLINGSTEMA_2, TRYGDEAVTALE_UNNTAK_BEHANDLINGSTYPE_2);
    public static final BehandlingstemaBehandlingstyperKombinasjon TRYGDEAVTALE_TRYGDEAVGIFT_BEHANDLINGS_KOMBINASJON = new BehandlingstemaBehandlingstyperKombinasjon(TRYGDEAVTALE_TRYGDEAVGIFT_BEHANDLINGSTEMA, TRYGDEAVTALE_TRYGDEAVGIFT_BEHANDLINGSTYPE);

    // VIRKSOMHET - har alle behandlingstema på alle sakstema og type
    public static final Set<Behandlingstyper> BEHANDLINGSTYPER_FOR_VIRKSOMHET = Set.of(HENVENDELSE);

    private LovligeBehandlingsKombinasjoner() {}
}
