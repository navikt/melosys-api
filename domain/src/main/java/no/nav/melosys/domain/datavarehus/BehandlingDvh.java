package no.nav.melosys.domain.datavarehus;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Objects;
import javax.persistence.*;

import no.nav.melosys.domain.Behandlingsstatus;
import no.nav.melosys.domain.Behandlingstype;

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

    public static FagsakDvh.Builder builder() {
        return new FagsakDvh.Builder();
    }

    public static class Builder {
        private Long behandling;
        private String saksnummer;
        private LocalDateTime funksjonellTid;
        private String endretAv;
        private String behandlingStatus;
        private String behandlingstype;
        private Instant registrertDato;
        private Instant endretDato;

        public Builder behandling(Long behandling) {
            this.behandling = behandling;
            return this;
        }

        public Builder saksnummer(String saksnummer) {
            this.saksnummer = saksnummer;
            return this;
        }

        public Builder funksjonellTid(LocalDateTime funksjonellTid) {
            this.funksjonellTid = funksjonellTid;
            return this;
        }

        public Builder endretAv(String endretAv) {
            this.endretAv = endretAv;
            return this;
        }

        public Builder behandlingStatus(Behandlingsstatus behandlingStatus) {
            if (behandlingStatus != null) {
                this.behandlingStatus = behandlingStatus.getKode();
            }
            return this;
        }

        public Builder behandlingstype(Behandlingstype behandlingstype) {
            if (behandlingstype != null) {
                this.behandlingstype = behandlingstype.getKode();
            }
            return this;
        }

        public Builder registrertDato(Instant registrertDato) {
            this.registrertDato = registrertDato;
            return this;
        }

        public Builder endretDato(Instant endretDato) {
            this.endretDato = endretDato;
            return this;
        }

        public BehandlingDvh build() {
            BehandlingDvh behandlingDvh = new BehandlingDvh();
            behandlingDvh.setBehandling(behandling);
            behandlingDvh.setSaksnummer(saksnummer);
            behandlingDvh.setFunksjonellTid(funksjonellTid);
            behandlingDvh.setEndretAv(endretAv);
            behandlingDvh.setBehandlingStatus(behandlingStatus);
            behandlingDvh.setBehandlingstype(behandlingstype);
            behandlingDvh.setRegistrertDato(registrertDato);
            behandlingDvh.setEndretDato(endretDato);
            return behandlingDvh;
        }
    }
}