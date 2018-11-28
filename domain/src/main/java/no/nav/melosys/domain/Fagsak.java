package no.nav.melosys.domain;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.persistence.*;

import no.nav.melosys.exception.TekniskException;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "fagsak")
@EntityListeners(AuditingEntityListener.class)
public class Fagsak extends RegistreringsInfo {

    // FIXME (farjam): Ikke tatt med fra logisk modell: tema, virkemiddel og registreringsmetainfo

    @Id
    @Column(name = "saksnummer", nullable = false)
    private String saksnummer;

    @Column(name = "gsak_saksnummer")
    private Long gsakSaksnummer;

    @Column(name = "fagsak_type")
    @Convert(converter = Fagsakstype.DbKonverterer.class)
    private Fagsakstype type;

    @Column(name = "status", nullable = false)
    @Convert(converter = Fagsaksstatus.DbKonverterer.class)
    private Fagsaksstatus status;

    @OneToMany(mappedBy = "fagsak", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private Set<Aktoer> aktører;

    // FIXME Diskutere strategi for eager/lazy loading
    @OneToMany(mappedBy = "fagsak", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<Behandling> behandlinger;

    public Long getGsakSaksnummer() {
        return gsakSaksnummer;
    }

    public void setGsakSaksnummer(Long gsakSaksnummer) {
        this.gsakSaksnummer = gsakSaksnummer;
    }

    public Fagsakstype getType() {
        return type;
    }

    public void setType(Fagsakstype type) {
        this.type = type;
    }

    public Fagsaksstatus getStatus() {
        return status;
    }

    public void setStatus(Fagsaksstatus status) {
        this.status = status;
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
    public Behandling getAktivBehandling() throws TekniskException {
        List<Behandling> behandlinger = getBehandlinger().stream()
            .filter(b -> !b.getStatus().equals(Behandlingsstatus.AVSLUTTET)).collect(Collectors.toList());
        if (behandlinger.size() > 1) {
            throw new TekniskException("Det finnes mer enn en aktive behandlinger for sak " + saksnummer);
        } else if (behandlinger.size() == 1) {
            return behandlinger.get(0);
        } else {
            return null;
        }
    }

    /**
     * Returnerer en aktør med angitt {@link RolleType} knyttet til saken eller {@code null} hvis ingen finnes.
     */
    public Aktoer hentAktørMedRolleType(RolleType rolleType) throws TekniskException {
        if (rolleType == null || aktører == null || aktører.isEmpty()) {
            return null;
        }
        List<Aktoer> kandidater = aktører.stream().filter(a -> rolleType.equals(a.getRolle())).collect(Collectors.toList());

        if (kandidater == null || kandidater.isEmpty()) {
            return null;
        } else if (kandidater.size() > 1) {
            throw new TekniskException("Det finnes mer enn en aktør med rollen " + rolleType.getBeskrivelse() + " for sak " + saksnummer);
        } else {
            return kandidater.get(0);
        }
    }

    public String getSaksnummer() {
        return saksnummer;
    }

    public void setSaksnummer(String saksnummer) {
        this.saksnummer = saksnummer;
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
        if (this.saksnummer == null) {
            throw new RuntimeException("Fagsak.equals ble kalt før fagsak har fått saksnummer");
        }
        return this.saksnummer.equals(that.saksnummer);
    }

    @Override
    public int hashCode() {
        if (this.saksnummer == null) {
            throw new RuntimeException("Fagsak.hashCode ble kalt før fagsak har fått saksnummer");
        }
        return saksnummer.hashCode();
    }
}
