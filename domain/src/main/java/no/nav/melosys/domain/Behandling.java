package no.nav.melosys.domain;

import java.time.Instant;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import javax.persistence.*;

import no.nav.melosys.domain.behandlingsgrunnlag.Behandlingsgrunnlag;
import no.nav.melosys.domain.dokument.SaksopplysningDokument;
import no.nav.melosys.domain.dokument.arbeidsforhold.ArbeidsforholdDokument;
import no.nav.melosys.domain.dokument.inntekt.InntektDokument;
import no.nav.melosys.domain.dokument.medlemskap.MedlemskapDokument;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.dokument.sed.SedDokument;
import no.nav.melosys.domain.dokument.utbetaling.UtbetalingDokument;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.exception.TekniskException;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "behandling")
@EntityListeners(AuditingEntityListener.class)
public class Behandling extends RegistreringsInfo {

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
    @Column(name = "beh_type", nullable = false, updatable = false)
    private Behandlingstyper type;

    @Enumerated(EnumType.STRING)
    @Column(name = "beh_tema", nullable = false, updatable = false)
    private Behandlingstema tema;

    @Column(name = "siste_opplysninger_hentet_dato")
    private Instant sisteOpplysningerHentetDato;

    @Column(name = "dokumentasjon_svarfrist_dato")
    private Instant dokumentasjonSvarfristDato;

    @Column(name = "initierende_journalpost_id")
    private String initierendeJournalpostId;

    @Column(name = "initierende_dokument_id")
    private String initierendeDokumentId;

    @OneToMany(mappedBy = "behandling", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private Set<Saksopplysning> saksopplysninger = new HashSet<>(1);

    @OneToMany(mappedBy = "behandling", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private Set<BehandlingHistorikk> behandlingshistorikk = new HashSet<>(1);

    @OneToMany(mappedBy = "behandling", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private Set<Behandlingsnotat> behandlingsnotater = new HashSet<>(1);

    @OneToOne(mappedBy = "behandling", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private Behandlingsgrunnlag behandlingsgrunnlag;

    @ManyToOne()
    @JoinColumn(name="opprinnelig_behandling_id")
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

    public PersonDokument hentPersonDokument() throws TekniskException {
        Optional<SaksopplysningDokument> saksopplysning = hentDokument(SaksopplysningType.PERSOPL);
        return (PersonDokument) saksopplysning
            .orElseThrow(() -> new TekniskException("Finner ikke persondokument"));
    }

    public Optional<PersonDokument> finnPersonDokument() {
        return hentDokument(SaksopplysningType.PERSOPL).map(s -> (PersonDokument) s);
    }

    public MedlemskapDokument hentMedlemskapDokument() throws TekniskException {
        Optional<SaksopplysningDokument> saksopplysning = hentDokument(SaksopplysningType.MEDL);
        return (MedlemskapDokument) saksopplysning
            .orElseThrow(() -> new TekniskException("Finner ikke medlemskapdokument"));
    }

    public ArbeidsforholdDokument hentArbeidsforholdDokument() throws TekniskException {
        Optional<SaksopplysningDokument> saksopplysning = hentDokument(SaksopplysningType.ARBFORH);
        return (ArbeidsforholdDokument) saksopplysning
            .orElseThrow(() -> new TekniskException("Finner ikke arbeidsforholddokument"));
    }

    public SedDokument hentSedDokument() throws TekniskException {
        Optional<SaksopplysningDokument> saksopplysning = hentDokument(SaksopplysningType.SEDOPPL);
        return (SedDokument) saksopplysning
            .orElseThrow(() -> new TekniskException("Finner ikke seddokument"));
    }

    public Optional<SedDokument> finnSedDokument() {
        return hentDokument(SaksopplysningType.SEDOPPL).map(s -> (SedDokument) s);
    }

    public InntektDokument hentInntektDokument() throws TekniskException {
        Optional<SaksopplysningDokument> saksopplysning = hentDokument(SaksopplysningType.INNTK);
        return (InntektDokument) saksopplysning
            .orElseThrow(() -> new TekniskException("Finner ikke inntektdokument"));
    }

    public Optional<UtbetalingDokument> finnUtbetalingDokument() {
        return hentDokument(SaksopplysningType.UTBETAL).map(d -> (UtbetalingDokument) d);
    }

    private Optional<SaksopplysningDokument> hentDokument(SaksopplysningType saksopplysningType) {
        return getSaksopplysninger().stream()
            .filter(saksopplysning -> saksopplysning.getType().equals(saksopplysningType))
            .findFirst().map(Saksopplysning::getDokument);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Behandling)) { // Implisitt nullsjekk
            return false;
        }
        Behandling that = (Behandling) o;
        if (this.id != 0 && that.id != 0) { // Begge entiteter er persistert. True hvis samme rad i db.
            return this.id.equals(that.getId());
        }
        return Objects.equals(registrertDato, that.registrertDato)
            && Objects.equals(this.fagsak, that.fagsak);
    }

    @Override
    public int hashCode() {
        return Objects.hash(registrertDato, fagsak);
    }

    public boolean kanAvsluttesManuelt() {
        return (tema == Behandlingstema.IKKE_YRKESAKTIV
            || tema == Behandlingstema.ØVRIGE_SED_MED
            || tema == Behandlingstema.ØVRIGE_SED_UFM
            || tema == Behandlingstema.TRYGDETID);
    }

    public boolean kanResultereIVedtak() {
        return erBehandlingAvSøknad() || erNorgeUtpekt();
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
        return !(status == Behandlingsstatus.IVERKSETTER_VEDTAK
            || (status == Behandlingsstatus.ANMODNING_UNNTAK_SENDT && tema != Behandlingstema.IKKE_YRKESAKTIV)
            || erInaktiv());
    }

    public boolean erVenterForDokumentasjon() {
        return status == Behandlingsstatus.AVVENT_DOK_PART
            || status == Behandlingsstatus.AVVENT_DOK_UTL
            || status == Behandlingsstatus.ANMODNING_UNNTAK_SENDT;
    }

    public boolean erBehandlingAvSøknad() {
        return tema != null && erBehandlingAvSøknad(tema.getKode());
    }

    public boolean erBehandlingAvSed() {
        return tema != null && erBehandlingAvSed(tema.getKode());
    }

    public boolean erNyVurdering() {
        return type == Behandlingstyper.NY_VURDERING;
    }

    public boolean erEndretPeriode() {
        return type == Behandlingstyper.ENDRET_PERIODE;
    }

    public boolean erKlage() {
        return type == Behandlingstyper.KLAGE;
    }

    public boolean erNorgeUtpekt() {
        return tema == Behandlingstema.BESLUTNING_LOVVALG_NORGE;
    }

    public boolean erBeslutningLovvalgAnnetLand() {
        return tema == Behandlingstema.BESLUTNING_LOVVALG_ANNET_LAND;
    }

    public boolean erUtsending() {
        return tema == Behandlingstema.UTSENDT_ARBEIDSTAKER || tema == Behandlingstema.UTSENDT_SELVSTENDIG;
    }

    public static boolean erBehandlingAvSøknad(Behandlingstema behandlingstema) {
        return erBehandlingAvSøknad(behandlingstema.getKode());
    }

    public static boolean erBehandlingAvSøknad(String behandlingstemaKode) {
        return erBehandlingAvSøknadUtsendtArbeidstaker(behandlingstemaKode)
            || erBehandlingAvSøknadArbeidIFlereLand(behandlingstemaKode)
            || Behandlingstema.ARBEID_ETT_LAND_ØVRIG.getKode().equalsIgnoreCase(behandlingstemaKode)
            || Behandlingstema.IKKE_YRKESAKTIV.getKode().equalsIgnoreCase(behandlingstemaKode)
            || Behandlingstema.ARBEID_NORGE_BOSATT_ANNET_LAND.getKode().equalsIgnoreCase(behandlingstemaKode);
    }

    public static boolean erBehandlingAvSøknadUtsendtArbeidstaker(String behandlingstemaKode) {
        return Behandlingstema.UTSENDT_ARBEIDSTAKER.getKode().equalsIgnoreCase(behandlingstemaKode)
            || Behandlingstema.UTSENDT_SELVSTENDIG.getKode().equalsIgnoreCase(behandlingstemaKode);
    }

    public static boolean erBehandlingAvSøknadArbeidIFlereLand(String behandlingstemaKode) {
        return Behandlingstema.ARBEID_FLERE_LAND.getKode().equalsIgnoreCase(behandlingstemaKode);
    }

    private static boolean erBehandlingAvSed(String behandlingstemaKode) {
        return Behandlingstema.REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING.getKode().equalsIgnoreCase(behandlingstemaKode)
            || Behandlingstema.REGISTRERING_UNNTAK_NORSK_TRYGD_ØVRIGE.getKode().equalsIgnoreCase(behandlingstemaKode)
            || Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL.getKode().equalsIgnoreCase(behandlingstemaKode)
            || Behandlingstema.BESLUTNING_LOVVALG_NORGE.getKode().equalsIgnoreCase(behandlingstemaKode)
            || Behandlingstema.BESLUTNING_LOVVALG_ANNET_LAND.getKode().equalsIgnoreCase(behandlingstemaKode);
    }

    public static boolean erBehandlingAvSedForespørsler(String behandlingstemaKode) {
        return Behandlingstema.ØVRIGE_SED_MED.getKode().equalsIgnoreCase(behandlingstemaKode)
            || Behandlingstema.ØVRIGE_SED_UFM.getKode().equalsIgnoreCase(behandlingstemaKode)
            || Behandlingstema.TRYGDETID.getKode().equalsIgnoreCase(behandlingstemaKode);
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
