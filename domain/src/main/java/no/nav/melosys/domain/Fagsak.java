package no.nav.melosys.domain;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

@Entity
@Table(name = "fagsak")
public class Fagsak {
    
    // FIXME (farjam): Ikke tatt med fra logisk modell: tema, virkemiddel og registreringsmetainfo

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "gsak_saksnummer")
    private String gsakSaksnummer;

    @Column(name = "fagsak_type", nullable = false, updatable = false)
    @Convert(converter = FagsakType.DbKonverterer.class)
    private FagsakType type;

    @Column(name = "status", nullable = false)
    @Convert(converter = FagsakStatus.DbKonverterer.class)
    private FagsakStatus status;

    @Column(name = "registrert_dato", nullable = false, updatable = false)
    private LocalDateTime registrertDato;

    @Column(name = "endret_dato", nullable = false, updatable = false)
    private LocalDateTime endretDato;

    @OneToMany(mappedBy = "fagsak", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private Set<Aktoer> aktører;

    @OneToMany(mappedBy = "fagsak", cascade = CascadeType.ALL)
    private List<Behandling> behandlinger;

    public long getId() {
        return id;
    }

    public String getGsakSaksnummer() {
        return gsakSaksnummer;
    }

    public void setGsakSaksnummer(String gsakSaksnummer) {
        this.gsakSaksnummer = gsakSaksnummer;
    }

    public FagsakType getType() {
        return type;
    }

    public void setType(FagsakType type) {
        this.type = type;
    }

    public FagsakStatus getStatus() {
        return status;
    }

    public void setStatus(FagsakStatus status) {
        this.status = status;
    }

    public LocalDateTime getRegistrertDato() {
        return registrertDato;
    }

    public void setRegistrertDato(LocalDateTime registrertDato) {
        this.registrertDato = registrertDato;
    }

    public LocalDateTime getEndretDato() {
        return endretDato;
    }

    public void setEndretDato(LocalDateTime endretDato) {
        this.endretDato = endretDato;
    }

    public Set<Aktoer> getAktører() {
        return aktører;
    }

    public void setAktører(Set<Aktoer> aktører) {
        this.aktører = aktører;
    }

    public List<Behandling> getBehandlinger() {
        return behandlinger;
    }

    public void setBehandlinger(List<Behandling> behandlinger) {
        this.behandlinger = behandlinger;
    }

    /**
     * Returnerer den aktive behandlingen knyttet til saken eller {@code null} hvis den ikke finnes.
     */
    public Behandling getAktivBehandling() {
        List<Behandling> behandlinger = getBehandlinger().stream()
            .filter(b -> !b.getStatus().equals(BehandlingStatus.AVSLUTTET)).collect(Collectors.toList());
        if (behandlinger.size() > 1) {
            throw new RuntimeException("Det finnes mer enn en aktive behandlinger");
        } else if (behandlinger.size() == 1) {
            return behandlinger.get(0);
        } else{
            return null;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Fagsak)) { // Implisitt nullsjekk
            return false;
        }
        Fagsak that = (Fagsak) o;
        if (this.id != 0 && that.id != 0) { // Begge entiteter er persistert. True hvis samme rad i db.
            return this.id == that.id;
        }
        return Objects.equals(this.gsakSaksnummer, that.gsakSaksnummer);
    }

    @Override
    public int hashCode() {
        return Objects.hash(gsakSaksnummer);
    }

}
