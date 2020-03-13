package no.nav.melosys.domain;

import java.time.Instant;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import javax.persistence.*;

import no.nav.melosys.domain.behandlingsgrunnlag.Behandlingsgrunnlag;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
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

    public boolean erAktiv() {
        return !erAvsluttet();
    }

    public boolean erAvsluttet() {
        return status == Behandlingsstatus.AVSLUTTET;
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

    public Behandlingsgrunnlag getBehandlingsgrunnlag() {
        return behandlingsgrunnlag;
    }

    public void setBehandlingsgrunnlag(Behandlingsgrunnlag behandlingsgrunnlag) {
        this.behandlingsgrunnlag = behandlingsgrunnlag;
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

    public boolean erRedigerbar() {
        return !(status == Behandlingsstatus.IVERKSETTER_VEDTAK
                    || (status == Behandlingsstatus.ANMODNING_UNNTAK_SENDT && type != Behandlingstyper.SOEKNAD_IKKE_YRKESAKTIV)
                    || status == Behandlingsstatus.AVSLUTTET
                    || status == Behandlingsstatus.MIDLERTIDIG_LOVVALGSBESLUTNING);
    }

    public boolean erVenterForDokumentasjon() {
        return status == Behandlingsstatus.AVVENT_DOK_PART
            || status == Behandlingsstatus.AVVENT_DOK_UTL
            || status == Behandlingsstatus.ANMODNING_UNNTAK_SENDT;
    }

    public boolean erBehandlingAvSøknad() {
        return type != null && erBehandlingAvSøknad(type.getKode());
    }

    public static boolean erBehandlingAvSøknad(String behandlingstypeKode) {
        return Behandlingstyper.SOEKNAD.getKode().equalsIgnoreCase(behandlingstypeKode)
            || Behandlingstyper.ENDRET_PERIODE.getKode().equalsIgnoreCase(behandlingstypeKode)
            || Behandlingstyper.NY_VURDERING.getKode().equalsIgnoreCase(behandlingstypeKode)
            || Behandlingstyper.SOEKNAD_IKKE_YRKESAKTIV.getKode().equalsIgnoreCase(behandlingstypeKode)
            || Behandlingstyper.SOEKNAD_ARBEID_FLERE_LAND.getKode().equalsIgnoreCase(behandlingstypeKode);
    }
    public boolean isAktiv() {
        return status != Behandlingsstatus.AVSLUTTET;
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
