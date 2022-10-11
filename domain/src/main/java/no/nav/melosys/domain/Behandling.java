package no.nav.melosys.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import javax.persistence.*;

import no.nav.melosys.domain.behandlingsgrunnlag.Behandlingsgrunnlag;
import no.nav.melosys.domain.dokument.SaksopplysningDokument;
import no.nav.melosys.domain.dokument.arbeidsforhold.ArbeidsforholdDokument;
import no.nav.melosys.domain.dokument.inntekt.InntektDokument;
import no.nav.melosys.domain.dokument.medlemskap.MedlemskapDokument;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.dokument.sed.SedDokument;
import no.nav.melosys.domain.dokument.utbetaling.UtbetalingDokument;
import no.nav.melosys.domain.kodeverk.Behandlingsgrunnlagtyper;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.TekniskException;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import static no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema.*;

@Entity
@Table(name = "behandling")
@EntityListeners(AuditingEntityListener.class)
public class Behandling extends RegistreringsInfo {

    public static final Set<Behandlingstema> BEHANDLINGSTEMA_SED_FORESPØRSEL = Set.of(ØVRIGE_SED_MED, ØVRIGE_SED_UFM,
        FORESPØRSEL_TRYGDEMYNDIGHET,
        TRYGDETID);

    private static final Set<Behandlingstema> BEHANDLINGSTEMA_SOM_IKKE_KAN_ENDRES = Set.of(
        REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING,
        REGISTRERING_UNNTAK_NORSK_TRYGD_ØVRIGE,
        BESLUTNING_LOVVALG_NORGE,
        BESLUTNING_LOVVALG_ANNET_LAND,
        ANMODNING_OM_UNNTAK_HOVEDREGEL
    );
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "saksnummer", nullable = false, updatable = false)
    private Fagsak fagsak;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private Behandlingsstatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "beh_type", nullable = false)
    private Behandlingstyper type;

    @Enumerated(EnumType.STRING)
    @Column(name = "beh_tema", nullable = false)
    private Behandlingstema tema;

    @Column(name = "siste_opplysninger_hentet_dato")
    private Instant sisteOpplysningerHentetDato;

    @Column(name = "dokumentasjon_svarfrist_dato")
    private Instant dokumentasjonSvarfristDato;

    @Column(name = "initierende_journalpost_id")
    private String initierendeJournalpostId;

    @Column(name = "initierende_dokument_id")
    private String initierendeDokumentId;

    @Column(name = "behandlingsfrist")
    private LocalDate behandlingsfrist;

    @OneToMany(mappedBy = "behandling", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private Set<Saksopplysning> saksopplysninger = new HashSet<>(1);

    @OneToMany(mappedBy = "behandling", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private Set<BehandlingHistorikk> behandlingshistorikk = new HashSet<>(1);

    @OneToMany(mappedBy = "behandling", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private Set<Behandlingsnotat> behandlingsnotater = new HashSet<>(1);

    @OneToOne(mappedBy = "behandling", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private Behandlingsgrunnlag behandlingsgrunnlag;

    @ManyToOne()
    @JoinColumn(name = "opprinnelig_behandling_id")
    private Behandling opprinneligBehandling;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Fagsak getFagsak() {
        return fagsak;
    }

    public void setFagsak(Fagsak fagsak) {
        this.fagsak = fagsak;
    }

    public Behandlingsstatus getStatus() {
        return status;
    }

    public void setStatus(Behandlingsstatus status) {
        this.status = status;
    }

    public Behandlingstyper getType() {
        return type;
    }

    public void setType(Behandlingstyper type) {
        this.type = type;
    }

    public Behandlingstema getTema() {
        return tema;
    }

    public void setTema(Behandlingstema tema) {
        this.tema = tema;
    }

    public Set<Saksopplysning> getSaksopplysninger() {
        return saksopplysninger;
    }

    public void setSaksopplysninger(Set<Saksopplysning> saksopplysninger) {
        this.saksopplysninger = saksopplysninger;
    }

    public Instant getSistOpplysningerHentetDato() {
        return sisteOpplysningerHentetDato;
    }

    public void setSisteOpplysningerHentetDato(Instant sisteOpplysningerHentetDato) {
        this.sisteOpplysningerHentetDato = sisteOpplysningerHentetDato;
    }

    public Instant getDokumentasjonSvarfristDato() {
        return dokumentasjonSvarfristDato;
    }

    public void setDokumentasjonSvarfristDato(Instant dokumentasjonSvarfristDato) {
        this.dokumentasjonSvarfristDato = dokumentasjonSvarfristDato;
    }

    public String getInitierendeJournalpostId() {
        return initierendeJournalpostId;
    }

    public void setInitierendeJournalpostId(String initierendeJournalpostId) {
        this.initierendeJournalpostId = initierendeJournalpostId;
    }

    public String getInitierendeDokumentId() {
        return initierendeDokumentId;
    }

    public void setInitierendeDokumentId(String initierendeDokumentId) {
        this.initierendeDokumentId = initierendeDokumentId;
    }

    public LocalDate getBehandlingsfrist() {
        return behandlingsfrist;
    }

    public void setBehandlingsfrist(LocalDate behandlingsfrist) {
        this.behandlingsfrist = behandlingsfrist;
    }

    public Behandling getOpprinneligBehandling() {
        return opprinneligBehandling;
    }

    public void setOpprinneligBehandling(Behandling opprinneligBehandling) {
        this.opprinneligBehandling = opprinneligBehandling;
    }

    public Set<Behandlingsnotat> getBehandlingsnotater() {
        return behandlingsnotater;
    }

    public void setBehandlingsnotater(Set<Behandlingsnotat> behandlingsnotater) {
        this.behandlingsnotater = behandlingsnotater;
    }

    public Behandlingsgrunnlag getBehandlingsgrunnlag() {
        return behandlingsgrunnlag;
    }

    public void setBehandlingsgrunnlag(Behandlingsgrunnlag behandlingsgrunnlag) {
        this.behandlingsgrunnlag = behandlingsgrunnlag;
    }

    /**
     * @deprecated Persondata skal ikke lagres under saksopplysning ifm. PDL.
     */
    @Deprecated
    public PersonDokument hentPersonDokument() {
        Optional<SaksopplysningDokument> saksopplysning = finnDokument(SaksopplysningType.PERSOPL);
        return (PersonDokument) saksopplysning
            .orElseThrow(() -> new TekniskException("Finner ikke persondokument"));
    }

    public MedlemskapDokument hentMedlemskapDokument() {
        Optional<SaksopplysningDokument> saksopplysning = finnDokument(SaksopplysningType.MEDL);
        return (MedlemskapDokument) saksopplysning
            .orElseThrow(() -> new TekniskException("Finner ikke medlemskapdokument"));
    }

    public ArbeidsforholdDokument hentArbeidsforholdDokument() {
        Optional<SaksopplysningDokument> saksopplysning = finnDokument(SaksopplysningType.ARBFORH);
        return (ArbeidsforholdDokument) saksopplysning
            .orElseThrow(() -> new TekniskException("Finner ikke arbeidsforholddokument"));
    }

    public List<OrganisasjonDokument> hentOrganisasjonDokumenter() {
        return getSaksopplysninger().stream()
            .filter(saksopplysning -> saksopplysning.getType().equals(SaksopplysningType.ORG))
            .map(Saksopplysning::getDokument)
            .map(OrganisasjonDokument.class::cast)
            .toList();
    }

    public boolean kanIkkeEndres() {
        return erInaktiv() || BEHANDLINGSTEMA_SOM_IKKE_KAN_ENDRES.contains(tema);
    }

    public SedDokument hentSedDokument() {
        Optional<SaksopplysningDokument> saksopplysning = finnDokument(SaksopplysningType.SEDOPPL);
        return (SedDokument) saksopplysning
            .orElseThrow(() -> new TekniskException("Finner ikke seddokument"));
    }

    public Optional<SedDokument> finnSedDokument() {
        return finnDokument(SaksopplysningType.SEDOPPL).map(SedDokument.class::cast);
    }

    public InntektDokument hentInntektDokument() {
        Optional<SaksopplysningDokument> saksopplysning = finnDokument(SaksopplysningType.INNTK);
        return (InntektDokument) saksopplysning
            .orElseThrow(() -> new TekniskException("Finner ikke inntektdokument"));
    }

    public Optional<UtbetalingDokument> finnUtbetalingDokument() {
        return finnDokument(SaksopplysningType.UTBETAL).map(UtbetalingDokument.class::cast);
    }

    public Optional<SaksopplysningDokument> finnDokument(SaksopplysningType saksopplysningType) {
        return getSaksopplysninger().stream()
            .filter(saksopplysning -> saksopplysning.getType().equals(saksopplysningType))
            .findFirst().map(Saksopplysning::getDokument);
    }

    public boolean harPeriodeOgLand() {
        var optionalPeriode = finnPeriode();
        var harPeriode = optionalPeriode.isPresent() && optionalPeriode.get().getFom() != null;

        var harLand = behandlingsgrunnlag.getBehandlingsgrunnlagdata().soeknadsland.erGyldig();

        return harPeriode && harLand;
    }

    public ErPeriode hentPeriode() {
        return finnPeriode()
            .orElseThrow(() -> new IkkeFunnetException("Finner ikke periode for behandling " + id));
    }

    public Optional<ErPeriode> finnPeriode() {
        var optionalSeddokument = finnSedDokument();
        if (optionalSeddokument.isPresent()) {
            return Optional.of(optionalSeddokument.get().getLovvalgsperiode());
        }

        if (behandlingsgrunnlag != null && behandlingsgrunnlag.getBehandlingsgrunnlagdata() != null) {
            return Optional.of(behandlingsgrunnlag.getBehandlingsgrunnlagdata().periode);
        }

        return Optional.empty();
    }

    public Collection<String> hentSøknadsLand() {
        if (erNorgeUtpekt()) {
            var utenlandskeArbeidsstederLandkoder = behandlingsgrunnlag.getBehandlingsgrunnlagdata().hentUtenlandskeArbeidsstederLandkode();
            return utenlandskeArbeidsstederLandkoder.isEmpty() ? Collections.singleton(Landkoder.NO.getKode()) : utenlandskeArbeidsstederLandkoder;
        } else {
            return behandlingsgrunnlag.getBehandlingsgrunnlagdata().soeknadsland.landkoder;
        }
    }

    /**
     * @deprecated Fjernes med toggle melosys.behandle_alle_saker. Skal erstattes alle steder den er brukt
     */
    @Deprecated
    public ErPeriode hentPeriodeGammel() {
        return finnPeriodeGammel()
            .orElseThrow(() -> new IkkeFunnetException("Finner ikke periode for behandling " + id));
    }

    /**
     * @deprecated Fjernes med toggle melosys.behandle_alle_saker. Skal erstattes alle steder den er brukt
     */
    @Deprecated
    public Optional<ErPeriode> finnPeriodeGammel() {
        if (kanResultereIVedtakGammel()) {
            return Optional.of(behandlingsgrunnlag.getBehandlingsgrunnlagdata().periode);
        } else if (erBehandlingAvSedGammel()) {
            return finnSedDokument().map(SedDokument::getLovvalgsperiode);
        }

        return Optional.empty();
    }

    /**
     * @deprecated Fjernes med toggle melosys.behandle_alle_saker. Skal erstattes alle steder den er brukt
     */
    @Deprecated
    public Collection<String> finnSøknadsLandGammel() {
        if (!kanResultereIVedtakGammel()) {
            return Collections.emptyList();
        }

        Collection<String> søknadsland;
        if (erNorgeUtpekt()) {
            søknadsland = behandlingsgrunnlag.getBehandlingsgrunnlagdata().hentUtenlandskeArbeidsstederLandkode();
            if (søknadsland.isEmpty()) {
                søknadsland.add(Landkoder.NO.getKode());
            }
        } else {
            søknadsland = behandlingsgrunnlag.getBehandlingsgrunnlagdata().soeknadsland.landkoder;
        }
        return søknadsland;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Behandling that)) { // Implisitt nullsjekk
            return false;
        }
        if (this.id != 0 && that.id != 0) { // Begge entiteter er persistert. True hvis samme rad i db.
            return this.id.equals(that.getId());
        }
        return Objects.equals(this.getRegistrertDato(), that.getRegistrertDato())
            && Objects.equals(this.fagsak, that.fagsak);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getRegistrertDato(), fagsak);
    }


    public boolean kanResultereIVedtak() {
        return erNorgeUtpekt() || !erBehandlingAvSed();
    }

    /**
     * @deprecated Fjernes med toggle melosys.behandle_alle_saker. Skal erstattes alle steder den er brukt
     */
    @Deprecated
    public boolean kanResultereIVedtakGammel() {
        return erBehandlingAvSøknadGammel() || erNorgeUtpekt();
    }

    public boolean erAktiv() {
        return !erInaktiv();
    }

    public boolean erInaktiv() {
        return erAvsluttet() || erMidlertidigLovvalgsbeslutning();
    }

    public boolean erAvsluttet() {
        return status == Behandlingsstatus.AVSLUTTET;
    }

    private boolean erMidlertidigLovvalgsbeslutning() {
        return status == Behandlingsstatus.MIDLERTIDIG_LOVVALGSBESLUTNING;
    }

    public boolean erRedigerbar() {
        return erAktiv() && status != Behandlingsstatus.IVERKSETTER_VEDTAK &&
            !(status == Behandlingsstatus.ANMODNING_UNNTAK_SENDT && tema != IKKE_YRKESAKTIV);
    }

    public boolean erVenterForDokumentasjon() {
        return status == Behandlingsstatus.AVVENT_DOK_PART
            || status == Behandlingsstatus.AVVENT_DOK_UTL
            || status == Behandlingsstatus.ANMODNING_UNNTAK_SENDT;
    }

    public boolean erNyVurdering() {
        return type == Behandlingstyper.NY_VURDERING;
    }

    public boolean erForVirksomhet() {
        return tema == VIRKSOMHET;
    }

    public boolean erEndretPeriode() {
        return type == Behandlingstyper.ENDRET_PERIODE;
    }

    public boolean erKlage() {
        return type == Behandlingstyper.KLAGE;
    }

    public boolean erNorgeUtpekt() {
        return tema == BESLUTNING_LOVVALG_NORGE;
    }

    public boolean erBeslutningLovvalgAnnetLand() {
        return tema == BESLUTNING_LOVVALG_ANNET_LAND;
    }

    public boolean erBeslutningLovvalgNorge() {
        return tema == BESLUTNING_LOVVALG_NORGE;
    }

    public boolean erUtsending() {
        return tema == UTSENDT_ARBEIDSTAKER || tema == UTSENDT_SELVSTENDIG;
    }

    public boolean erRegisteringAvUnntak() {
        return erRegistreringAvUnntak(tema.getKode());
    }

    public boolean erAnmodningOmUnntak() {
        return erAnmodningOmUnntak(tema.getKode());
    }

    public boolean erElektroniskSøknad() {
        if (behandlingsgrunnlag != null) {
            return behandlingsgrunnlag.getType() == Behandlingsgrunnlagtyper.SØKNAD_A1_UTSENDTE_ARBEIDSTAKERE_EØS;
        }
        return false;
    }

    public boolean erBehandlingAvSed() {
        return tema != null && fagsak != null && (
            erRegistreringAvUnntak(tema) ||
                erAnmodningOmUnntakOgSakstypeEuEøs(tema, fagsak.getType()) ||
                BESLUTNING_LOVVALG_NORGE.equals(tema));
    }

    /**
     * @deprecated Fjernes med toggle melosys.behandle_alle_saker. Skal erstattes alle steder den er brukt
     */
    @Deprecated
    public boolean erBehandlingAvSøknadGammel() {
        return tema != null && erBehandlingAvSøknadGammel(tema.getKode());
    }

    /**
     * @deprecated Fjernes med toggle melosys.behandle_alle_saker. Skal erstattes alle steder den er brukt
     */
    @Deprecated
    public static boolean erBehandlingAvSøknadGammel(Behandlingstema behandlingstema) {
        return erBehandlingAvSøknadGammel(behandlingstema.getKode());
    }

    /**
     * @deprecated Fjernes med toggle melosys.behandle_alle_saker. Skal erstattes alle steder den er brukt
     */
    @Deprecated
    public static boolean erBehandlingAvSøknadGammel(String behandlingstemaKode) {
        return erBehandlingAvSøknadUtsendtArbeidstaker(behandlingstemaKode)
            || erBehandlingAvSøknadArbeidIFlereLand(behandlingstemaKode)
            || ARBEID_ETT_LAND_ØVRIG.getKode().equalsIgnoreCase(behandlingstemaKode)
            || ARBEID_TJENESTEPERSON_ELLER_FLY.getKode().equalsIgnoreCase(behandlingstemaKode)
            || IKKE_YRKESAKTIV.getKode().equalsIgnoreCase(behandlingstemaKode)
            || ARBEID_NORGE_BOSATT_ANNET_LAND.getKode().equalsIgnoreCase(behandlingstemaKode)
            || ARBEID_I_UTLANDET.getKode().equalsIgnoreCase(behandlingstemaKode)
            || ARBEID_KUN_NORGE.getKode().equalsIgnoreCase(behandlingstemaKode)
            || YRKESAKTIV.getKode().equalsIgnoreCase(behandlingstemaKode);
    }

    public static boolean erBehandlingAvSøknadUtsendtArbeidstaker(String behandlingstemaKode) {
        return UTSENDT_ARBEIDSTAKER.getKode().equalsIgnoreCase(behandlingstemaKode)
            || UTSENDT_SELVSTENDIG.getKode().equalsIgnoreCase(behandlingstemaKode);
    }

    public static boolean erBehandlingAvSøknadArbeidIFlereLand(String behandlingstemaKode) {
        return ARBEID_FLERE_LAND.getKode().equalsIgnoreCase(behandlingstemaKode);
    }

    /**
     * @deprecated Fjernes med toggle melosys.behandle_alle_saker. Skal erstattes alle steder den er brukt
     */
    @Deprecated
    public boolean erBehandlingAvSedGammel() {
        return tema != null && erBehandlingAvSedGammel(tema.getKode());
    }

    /**
     * @deprecated Fjernes med toggle melosys.behandle_alle_saker. Skal erstattes alle steder den er brukt
     */
    @Deprecated
    private static boolean erBehandlingAvSedGammel(String behandlingstemaKode) {
        return erRegistreringAvUnntak(behandlingstemaKode)
            || erAnmodningOmUnntak(behandlingstemaKode)
            || BESLUTNING_LOVVALG_NORGE.getKode().equalsIgnoreCase(behandlingstemaKode);
    }

    public static boolean erAnmodningOmUnntak(Behandlingstema behandlingstema) {
        return erAnmodningOmUnntak(behandlingstema.getKode());
    }

    private static boolean erAnmodningOmUnntak(String behandlingstemaKode) {
        return ANMODNING_OM_UNNTAK_HOVEDREGEL.getKode().equalsIgnoreCase(behandlingstemaKode);
    }

    private static boolean erAnmodningOmUnntakOgSakstypeEuEøs(Behandlingstema behandlingstema, Sakstyper sakstype) {
        return ANMODNING_OM_UNNTAK_HOVEDREGEL.equals(behandlingstema) && Sakstyper.EU_EOS.equals(sakstype);
    }

    public static boolean erBehandlingAvSedForespørsler(Behandlingstema behandlingstema) {
        return BEHANDLINGSTEMA_SED_FORESPØRSEL.contains(behandlingstema);
    }

    public static boolean erBehandlingAvSedForespørsler(String behandlingstemaKode) {
        return erBehandlingAvSedForespørsler(valueOf(behandlingstemaKode));
    }

    public static boolean erRegistreringAvUnntak(Behandlingstema behandlingstema) {
        return erRegistreringAvUnntak(behandlingstema.getKode());
    }

    private static boolean erRegistreringAvUnntak(String behandlingstemaKode) {
        return REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING.getKode().equalsIgnoreCase(behandlingstemaKode)
            || REGISTRERING_UNNTAK_NORSK_TRYGD_ØVRIGE.getKode().equalsIgnoreCase(behandlingstemaKode)
            || BESLUTNING_LOVVALG_ANNET_LAND.getKode().equalsIgnoreCase(behandlingstemaKode);
    }

    public static LocalDate utledFristForBehandlingstema(Behandlingstema behandlingstema) {
        return switch (behandlingstema) {
            case UTSENDT_ARBEIDSTAKER,
                UTSENDT_SELVSTENDIG,
                ARBEID_FLERE_LAND,
                ARBEID_ETT_LAND_ØVRIG,
                ARBEID_TJENESTEPERSON_ELLER_FLY,
                ARBEID_KUN_NORGE,
                IKKE_YRKESAKTIV,
                ARBEID_I_UTLANDET,
                ARBEID_NORGE_BOSATT_ANNET_LAND,
                YRKESAKTIV,
                UNNTAK_MEDLEMSKAP,
                REGISTRERING_UNNTAK,
                PENSJONIST -> LocalDate.now().plusDays(30);
            case REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING,
                REGISTRERING_UNNTAK_NORSK_TRYGD_ØVRIGE -> LocalDate.now().plusWeeks(2);
            case BESLUTNING_LOVVALG_NORGE,
                BESLUTNING_LOVVALG_ANNET_LAND -> LocalDate.now().plusWeeks(4);
            case ANMODNING_OM_UNNTAK_HOVEDREGEL,
                ØVRIGE_SED_UFM,
                ØVRIGE_SED_MED,
                FORESPØRSEL_TRYGDEMYNDIGHET,
                TRYGDETID -> LocalDate.now().plusWeeks(8);
            case VIRKSOMHET -> LocalDate.now().plusDays(90);
        };
    }

    public boolean harStatus(Behandlingsstatus status) {
        return this.status == status;
    }

    public boolean manglerSaksopplysningerAvType(List<SaksopplysningType> saksopplysningTyper) {
        return Collections.disjoint(saksopplysningTyper, getSaksopplysninger().stream().map(Saksopplysning::getType).toList());
    }

    @Override
    public String toString() {
        return "Behandling{" +
            "id=" + id +
            ", fagsak=" + fagsak.getSaksnummer() +
            ", type=" + type +
            ", status=" + status +
            "} ";
    }
}
