package no.nav.melosys.domain.datavarehus;

import javax.persistence.*;

@Entity
@Table(name = "fagsak_dvh")
public class FagsakDvh {

    @Id
    @SequenceGenerator(name = "fagsak_dvh_sequence", sequenceName = "FAGSAK_DVH_SEQ")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "fagsak_dvh_sequence")
    @Column(name = "trans_id")
    private Long id;

    private String saksnummer;

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

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        public FagsakDvh build() {
            return null;
        }
    }
}
