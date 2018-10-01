package no.nav.melosys.domain.datavarehus;

import java.time.Instant;
import java.util.Objects;
import javax.persistence.*;

@Entity
@Table(name = "behandling_dvh")
public class BehandlingDvh extends DvhBaseEntitet {

    @Id
    @SequenceGenerator(name = "behandling_dvh_sequence", sequenceName = "behandling_dvh_seq")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "behandling_dvh_sequence")
    @Column(name = "trans_id", nullable = false)
    private Long id;

    @Column(nullable = false)
    private Long behandling;

    @Column(nullable = false)
    private String saksnummer;

    @Column(name = "status", nullable = false)
    private String behandlingStatus;

    @Column(name = "beh_type", nullable = false)
    private String behandlingstype;

    @Column(name = "registrert_dato", nullable = false)
    private Instant registrertDato;

    @Column(name = "endret_dato", nullable = false)
    private Instant endretDato;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getBehandling() {
        return behandling;
    }

    public void setBehandling(Long behandling) {
        this.behandling = behandling;
    }

    public String getSaksnummer() {
        return saksnummer;
    }

    public void setSaksnummer(String saksnummer) {
        this.saksnummer = saksnummer;
    }

    public String getBehandlingStatus() {
        return behandlingStatus;
    }

    public void setBehandlingStatus(String behandlingStatus) {
        this.behandlingStatus = behandlingStatus;
    }

    public String getBehandlingstype() {
        return behandlingstype;
    }

    public void setBehandlingstype(String behandlingstype) {
        this.behandlingstype = behandlingstype;
    }

    public Instant getRegistrertDato() {
        return registrertDato;
    }

    public void setRegistrertDato(Instant registrertDato) {
        this.registrertDato = registrertDato;
    }

    public Instant getEndretDato() {
        return endretDato;
    }

    public void setEndretDato(Instant endretDato) {
        this.endretDato = endretDato;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof BehandlingDvh)) {
            return false;
        }
        if (!super.equals(other)) {
            return false;
        }
        BehandlingDvh castOther = (BehandlingDvh) other;
        return Objects.equals(behandling, castOther.behandling)
            && Objects.equals(saksnummer, castOther.saksnummer)
            && Objects.equals(behandlingStatus, castOther.behandlingStatus)
            && Objects.equals(behandlingstype, castOther.behandlingstype)
            && Objects.equals(registrertDato, castOther.registrertDato)
            && Objects.equals(endretDato, castOther.endretDato);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), behandling, saksnummer, behandlingStatus, behandlingstype, registrertDato, endretDato);
    }
}