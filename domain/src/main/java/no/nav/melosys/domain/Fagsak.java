package no.nav.melosys.domain;


import java.util.*;
import java.util.stream.Collectors;
import javax.persistence.*;

import no.nav.melosys.domain.kodeverk.*;
import no.nav.melosys.exception.TekniskException;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import static no.nav.melosys.domain.kodeverk.Aktoersroller.MYNDIGHET;

@Entity
@Table(name = "fagsak")
@EntityListeners(AuditingEntityListener.class)
public class Fagsak extends RegistreringsInfo {

    @Id
    @Column(name = "saksnummer", nullable = false)
    private String saksnummer;

    @Column(name = "gsak_saksnummer")
    private Long gsakSaksnummer;

    @Column(name = "fagsak_type")
    @Convert(converter = Sakstyper.DbKonverterer.class)
    private Sakstyper type;

    @Column(name = "status", nullable = false)
    @Convert(converter = Saksstatuser.DbKonverterer.class)
    private Saksstatuser status;

    @OneToMany(mappedBy = "fagsak", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private Set<Aktoer> aktører = new HashSet<>(1);

    @OneToMany(mappedBy = "fagsak", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<Behandling> behandlinger = new ArrayList<>(1);

    public Long getGsakSaksnummer() {
        return gsakSaksnummer;
    }

    public void setGsakSaksnummer(Long gsakSaksnummer) {
        this.gsakSaksnummer = gsakSaksnummer;
    }

    public Sakstyper getType() {
        return type;
    }

    public void setType(Sakstyper type) {
        this.type = type;
    }

    public Saksstatuser getStatus() {
        return status;
    }

    public void setStatus(Saksstatuser status) {
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
        if (getBehandlinger() == null) {
            return null;
        }
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
     * Returnerer den inaktive behandlingen knyttet til saken eller {@code null} hvis den ikke finnes.
     */
    public Behandling getTidligsteInaktiveBehandling() {
        return getBehandlinger().stream()
            .filter(b -> b.getStatus().equals(Behandlingsstatus.AVSLUTTET))
            .min(Comparator.comparing(RegistreringsInfo::getRegistrertDato))
            .orElse(null);
    }

    /**
     * Returnerer en aktør med angitt {@link Aktoersroller} knyttet til saken eller {@code null} hvis ingen finnes.
     */
    public Aktoer hentAktørMedRolleType(Aktoersroller rolleType) throws TekniskException {
        if (rolleType == null || aktører == null || aktører.isEmpty()) {
            return null;
        }
        List<Aktoer> kandidater = aktører.stream().filter(a -> rolleType.equals(a.getRolle())).collect(Collectors.toList());

        if (kandidater.isEmpty()) {
            return null;
        } else if (kandidater.size() > 1) {
            throw new TekniskException("Det finnes mer enn en aktør med rollen " + rolleType.getBeskrivelse() + " for sak " + saksnummer);
        } else {
            return kandidater.get(0);
        }
    }

    public Landkoder hentMyndighetLandkode() throws TekniskException {
        Aktoer myndighet = hentAktørMedRolleType(MYNDIGHET);
        String[] split = myndighet.getInstitusjonId().split(":");
        return Landkoder.valueOf(split[0]);
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
        return saksnummer != null && this.saksnummer.equals(that.saksnummer);
    }

    @Override
    public int hashCode() {
        return 31;
    }
}
