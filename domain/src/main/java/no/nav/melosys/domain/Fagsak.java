package no.nav.melosys.domain;


import java.util.*;
import java.util.stream.Collectors;
import javax.persistence.*;

import no.nav.melosys.domain.kodeverk.*;
import no.nav.melosys.exception.FunksjonellException;
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

    @Enumerated(EnumType.STRING)
    @Column(name = "fagsak_type")
    private Sakstyper type;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
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
    public Behandling hentAktivBehandling() throws TekniskException {
        List<Behandling> behandlingListe = getBehandlinger().stream()
            .filter(Behandling::erAktiv).collect(Collectors.toList());
        if (behandlingListe.size() > 1) {
            throw new TekniskException("Det finnes mer enn en aktiv behandling for sak " + saksnummer);
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
            .filter(Behandling::erInaktiv)
            .min(Comparator.comparing(RegistreringsInfo::getRegistrertDato))
            .orElse(null);
    }

    public Behandling hentSistAktiveBehandling() throws TekniskException, FunksjonellException {
        return Optional.ofNullable(hentAktivBehandling()).orElse(getSistOppdaterteBehandling());
    }

    public Aktoer hentBruker() throws TekniskException {
        return hentAktørMedRolleTypeBruker();
    }

    public List<Aktoer> hentMyndigheter() {
        return hentAktørerMedRolleType(MYNDIGHET);
    }

    /**
     * Returnerer den sist oppdaterte behandlingen knyttet til saken
     */
    public Behandling getSistOppdaterteBehandling() throws FunksjonellException {
        return getBehandlinger().stream()
            .max(Comparator.comparing(Behandling::getEndretDato))
            .orElseThrow(() -> new FunksjonellException("Finner ikke behandlinger for fagsak " + saksnummer));
    }

    private Aktoer hentAktørMedRolleTypeBruker() throws TekniskException {
        Collection<Aktoer> kandidater = hentAktørerMedRolleType(Aktoersroller.BRUKER);
        if (kandidater.size() > 1) {
            throw new TekniskException("Det finnes mer enn en aktør med rollen " + Aktoersroller.BRUKER.getBeskrivelse() + " for sak " + saksnummer);
        }
        return kandidater.stream().findFirst().orElse(null);
    }

    private List<Aktoer> hentAktørerMedRolleType(Aktoersroller rolleType) {
        if (rolleType == null) {
            return Collections.emptyList();
        }
        return aktører.stream()
            .filter(a -> rolleType == a.getRolle())
            .collect(Collectors.toList());
    }

    public boolean harAktørMedRolleType(Aktoersroller rolleType) {
        return !hentAktørerMedRolleType(rolleType).isEmpty();
    }

    /**
     * Henter myndighetens landkode fra institusjonsID som har format landkode:institusjonskode.
     */
    public Landkoder hentMyndighetLandkode() throws TekniskException {
        return hentMyndighet().hentMyndighetLandkode();
    }

    public Aktoer hentMyndighet() throws TekniskException {
        List<Aktoer> myndigheter = hentMyndigheter();
        if (myndigheter.isEmpty()) {
            throw new TekniskException("Finner ingen aktør med rolle " + MYNDIGHET + " for fagsak " + saksnummer);
        }
        if (myndigheter.size() > 1) {
            throw new TekniskException("Kan ikke hente landkode for en bestemt myndighet fordi finnes flere myndigheter");
        }
        return myndigheter.get(0);
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
