package no.nav.melosys.domain.datavarehus;

import java.time.Instant;
import java.util.Objects;
import javax.persistence.*;

@Entity
@Table(name = "fagsak_dvh")
public class FagsakDvh extends DvhBaseEntitet {

    @Id
    @SequenceGenerator(name = "fagsak_dvh_sequence", sequenceName = "fagsak_dvh_seq")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "fagsak_dvh_sequence")
    @Column(name = "trans_id", nullable = false)
    private Long id;

    @Column(nullable = false)
    private String saksnummer;

    @Column(name = "gsak_saksnummer")
    private Long gsakSaksnummer;

    @Column(name = "fagsak_type")
    private String fagsakType;

    @Column(name = "status", nullable = false)
    private String fagsakStatus;

    @Column(name = "bruker_id", nullable = false)
    private String brukerId;

    @Column(name = "arbeidsgiver_id", nullable = false)
    private String arbeidsgiverId;

    @Column(name = "representant_id")
    private String representantId;

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
}
