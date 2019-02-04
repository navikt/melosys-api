package no.nav.melosys.domain;

import java.time.Instant;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import javax.persistence.*;

import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "behandling")
@EntityListeners(AuditingEntityListener.class)
public class Behandling extends RegistreringsInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "saksnummer", nullable = false, updatable = false)
    private Fagsak fagsak;

    @Column(name = "status", nullable = false)
    @Convert(converter = Behandlingsstatus.DbKonverterer.class)
    private Behandlingsstatus status;

    @Column(name = "beh_type", nullable = false, updatable = false)
    @Convert(converter = Behandlingstype.DbKonverterer.class)
    private Behandlingstype type;

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

    public long getId() {
        return id;
    }

    public void setId(long id) {
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

    public Behandlingstype getType() {
        return type;
    }

    public void setType(Behandlingstype type) {
        this.type = type;
    }
    
    public Set<Saksopplysning> getSaksopplysninger() {
        return saksopplysninger;
    }

    public void setSaksopplysninger(Set<Saksopplysning> saksopplysninger) {
        this.saksopplysninger = saksopplysninger;
    }

    public Set<BehandlingHistorikk> getBehandlingshistorikk() {
        return behandlingshistorikk;
    }

    public void setBehandlingshistorikk(Set<BehandlingHistorikk> behandlingshistorikk) {
        this.behandlingshistorikk = behandlingshistorikk;
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
        return !status.equals(Behandlingsstatus.AVSLUTTET);
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
            return this.id == that.id;
        }
        return Objects.equals(registrertDato, that.registrertDato)
            && Objects.equals(this.fagsak, that.fagsak);
    }

    @Override
    public int hashCode() {
        return Objects.hash(registrertDato, fagsak);
    }

}
