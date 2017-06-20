package no.nav.melosys.domain;

import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "FAGSAK")
public class Fagsak {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "saksnummer")
    private Long saksnummer;

    @ManyToOne
    @JoinColumn(name = "status", nullable = false)
    private FagsakStatus status = FagsakStatus.OPPRETTET;

    @Column(name = "virkemiddel")
    private String virkemiddel;

    @ManyToOne
    @JoinColumn(name = "bruker")
    private Bruker bruker;

    @ManyToOne
    @JoinColumn(name = "arbeidsgiver")
    private Arbeidsgiver arbeidsgiver;

    @ManyToOne
    @JoinColumn(name = "fullmektig")
    private Fullmektig fullmektig;

    public Long getId() {
        return id;
    }

    public Arbeidsgiver getArbeidsgiver() {
        return arbeidsgiver;
    }

    public void setArbeidsgiver(Arbeidsgiver arbeidsgiver) {
        this.arbeidsgiver = arbeidsgiver;
    }

    public Bruker getBruker() {
        return bruker;
    }

    public void setBruker(Bruker bruker) {
        this.bruker = bruker;
    }

    public Fullmektig getFullmektig() {
        return fullmektig;
    }

    public void setFullmektig(Fullmektig fullmektig) {
        this.fullmektig = fullmektig;
    }

    public Long getSaksnummer() {
        return saksnummer;
    }

    public void setSaksnummer(Long saksnummer) {
        this.saksnummer = saksnummer;
    }

    public FagsakStatus getStatus() {
        return status;
    }

    public void setStatus(FagsakStatus status) {
        this.status = status;
    }

    public String getVirkemiddel() {
        return virkemiddel;
    }

    public void setVirkemiddel(String virkemiddel) {
        this.virkemiddel = virkemiddel;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Fagsak fagsak = (Fagsak) o;
        return Objects.equals(saksnummer, fagsak.getSaksnummer());
    }

    @Override
    public int hashCode() {
        return Objects.hash(saksnummer);
    }
}
