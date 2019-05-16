package no.nav.melosys.domain;


import java.util.*;
import java.util.stream.Collectors;
import javax.persistence.*;

import no.nav.melosys.domain.kodeverk.*;
import no.nav.melosys.exception.TekniskException;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.util.Assert;

import static no.nav.melosys.domain.kodeverk.Aktoersroller.MYNDIGHET;
import static no.nav.melosys.domain.kodeverk.Aktoersroller.REPRESENTANT;

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
        List<Behandling> behandlingListe = getBehandlinger().stream()
            .filter(Behandling::isAktiv).collect(Collectors.toList());
        if (behandlingListe.size() > 1) {
            throw new TekniskException("Det finnes mer enn en aktive behandling for sak " + saksnummer);
        } else if (behandlingListe.size() == 1) {
            return behandlingListe.get(0);
        } else {
            return null;
        }
    }

    /**
     * Returnerer den inaktive behandlingen knyttet til saken eller {@code null} hvis den ikke finnes.
     */
    public Behandling getTidligsteInaktiveBehandling() {
        return getBehandlinger().stream()
            .filter(b -> !b.isAktiv())
            .min(Comparator.comparing(RegistreringsInfo::getRegistrertDato))
            .orElse(null);
    }

    /**
     * Returnerer en aktør med angitt {@link Aktoersroller} knyttet til saken eller {@code null} hvis ingen finnes.
     */
    public Aktoer hentAktørMedRolleType(Aktoersroller rolleType) throws TekniskException {
        if (rolleType == null) {
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

    public boolean harAktørMedRolleType(Aktoersroller rolleType) {
        return aktører.stream().filter(a -> rolleType.equals(a.getRolle())).count() > 0;
    }

    /**
     * Henter myndighetens landkode fra institusjonsID som har format landkode:institusjonskode.
     */
    public Landkoder hentMyndighetLandkode() throws TekniskException {
        Aktoer myndighet = hentAktørMedRolleType(MYNDIGHET);
        if (myndighet == null) {
            throw new TekniskException("Finnes ingen aktør med rolle " + MYNDIGHET + " for fagsak" + saksnummer);
        }
        return myndighet.hentMyndighetLandkode();
    }

    /**
     * Henter representanten som representerer angitt {@link Representerer} eller {@code null} hvis ingen finnes.
     */
    public Optional<Aktoer> hentRepresentant(Representerer representerer) {
        Assert.notNull(representerer, "Representerer trengs for å hente representant.");
        return aktører.stream().filter(a -> REPRESENTANT.equals(a.getRolle()))
            .filter(a -> (representerer.equals(a.getRepresenterer()) || Representerer.BEGGE.equals(a.getRepresenterer())))
            .findFirst();
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
