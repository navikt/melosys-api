package no.nav.melosys.domain.datavarehus;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Objects;
import javax.persistence.*;

import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.Fagsaksstatus;
import no.nav.melosys.domain.Fagsakstype;

@Entity
@Table(name = "fagsak_dvh")
public class FagsakDvh extends DvhBaseEntitet {

    @Id
    @SequenceGenerator(name = "fagsak_dvh_sequence", sequenceName = "fagsak_dvh_seq")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "fagsak_dvh_sequence")
    @Column(name = "trans_id")
    private Long id;

    private String saksnummer;

    @Column(name = "gsak_saksnummer")
    private Long gsakSaksnummer;

    @Column(name = "fagsak_type")
    private String fagsakType;

    @Column(name = "status", nullable = false)
    private String fagsakStatus;

    private String brukerId;

    private String arbeidsgiverId;

    private String representantId;

    @Column(name = "registrert_dato", nullable = false, updatable = false)
    private Instant registrertDato;

    @Column(name = "endret_dato", nullable = false, updatable = false)
    private Instant endretDato;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSaksnummer() {
        return saksnummer;
    }

    public void setSaksnummer(String saksnummer) {
        this.saksnummer = saksnummer;
    }

    public Long getGsakSaksnummer() {
        return gsakSaksnummer;
    }

    public void setGsakSaksnummer(Long gsakSaksnummer) {
        this.gsakSaksnummer = gsakSaksnummer;
    }

    public String getFagsakType() {
        return fagsakType;
    }

    public void setFagsakType(String fagsakType) {
        this.fagsakType = fagsakType;
    }

    public String getFagsakStatus() {
        return fagsakStatus;
    }

    public void setFagsakStatus(String fagsakStatus) {
        this.fagsakStatus = fagsakStatus;
    }

    public String getBrukerId() {
        return brukerId;
    }

    public void setBrukerId(String brukerId) {
        this.brukerId = brukerId;
    }

    public String getArbeidsgiverId() {
        return arbeidsgiverId;
    }

    public void setArbeidsgiverId(String arbeidsgiverId) {
        this.arbeidsgiverId = arbeidsgiverId;
    }

    public String getRepresentantId() {
        return representantId;
    }

    public void setRepresentantId(String representantId) {
        this.representantId = representantId;
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
        if (!(other instanceof FagsakDvh)) {
            return false;
        }
        if (!super.equals(other)) {
            return false;
        }
        FagsakDvh castOther = (FagsakDvh) other;
        return Objects.equals(saksnummer, castOther.saksnummer)
            && Objects.equals(gsakSaksnummer, castOther.gsakSaksnummer)
            && Objects.equals(fagsakType, castOther.fagsakType)
            && Objects.equals(fagsakStatus, castOther.fagsakStatus)
            && Objects.equals(brukerId, castOther.brukerId)
            && Objects.equals(arbeidsgiverId, castOther.arbeidsgiverId)
            && Objects.equals(representantId, castOther.representantId)
            && Objects.equals(registrertDato, castOther.registrertDato)
            && Objects.equals(endretDato, castOther.endretDato);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), saksnummer, gsakSaksnummer, fagsakType, fagsakStatus, brukerId,
            arbeidsgiverId, representantId, registrertDato, endretDato);
    }


    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String saksnummer;
        private LocalDateTime funksjonellTid;
        private String endretAv;
        private Long gsakSaksnummer;
        private String fagsakType;
        private String fagsakStatus;
        private String brukerId;
        private String arbeidsgiverId;
        private String representantId;
        private Instant registrertDato;
        private Instant endretDato;

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

        public Builder gsakSaksnummer(Long gsakSaksnummer) {
            this.gsakSaksnummer = gsakSaksnummer;
            return this;
        }

        public Builder fagsakType(Fagsakstype fagsakType) {
            if (fagsakType != null) {
                this.fagsakType = fagsakType.getKode();
            }
            return this;
        }

        public Builder fagsakStatus(Fagsaksstatus fagsakStatus) {
            if (fagsakStatus != null) {
                this.fagsakStatus = fagsakStatus.getKode();
            }
            return this;
        }

        public Builder bruker(Aktoer aktør) {
            if (aktør != null) {
                this.brukerId = aktør.getAktørId();
            }
            return this;
        }

        public Builder arbeidsgiver(Aktoer aktør) {
            if (aktør != null) {
                this.arbeidsgiverId = aktør.getOrgnr() != null ? aktør.getOrgnr() : aktør.getAktørId();
            }
            return this;
        }

        public Builder representant(Aktoer aktør) {
            if (aktør != null) {
                this.representantId = aktør.getOrgnr() != null ? aktør.getOrgnr() : aktør.getAktørId();
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

        public FagsakDvh build() {
            FagsakDvh fagsakDvh = new FagsakDvh();
            fagsakDvh.setSaksnummer(saksnummer);
            fagsakDvh.setFunksjonellTid(funksjonellTid);
            fagsakDvh.setEndretAv(endretAv);
            fagsakDvh.setGsakSaksnummer(gsakSaksnummer);
            fagsakDvh.setFagsakType(fagsakType);
            fagsakDvh.setFagsakStatus(fagsakStatus);
            fagsakDvh.setBrukerId(brukerId);
            fagsakDvh.setArbeidsgiverId(arbeidsgiverId);
            fagsakDvh.setRepresentantId(representantId);
            fagsakDvh.setRegistrertDato(registrertDato);
            fagsakDvh.setEndretDato(endretDato);
            return fagsakDvh;
        }
    }
}
