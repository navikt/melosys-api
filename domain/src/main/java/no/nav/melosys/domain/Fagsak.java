package no.nav.melosys.domain;

import java.util.*;
import javax.persistence.*;

import no.nav.melosys.domain.kodeverk.*;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.TekniskException;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.util.Assert;

import static no.nav.melosys.domain.kodeverk.Aktoersroller.*;

@Entity
@Table(name = "fagsak")
@EntityListeners(AuditingEntityListener.class)
public class Fagsak extends RegistreringsInfo {

    private static final String FINNER_IKKE_BEHANDLINGER_FOR_FAGSAK = "Finner ikke behandlinger for fagsak ";

    @Id
    @Column(name = "saksnummer", nullable = false)
    private String saksnummer;

    @Column(name = "gsak_saksnummer")
    private Long gsakSaksnummer;

    @Enumerated(EnumType.STRING)
    @Column(name = "fagsak_type", nullable = false)
    private Sakstyper type;

    @Enumerated(EnumType.STRING)
    @Column(name = "tema", nullable = false)
    private Sakstemaer tema;

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

    public Sakstemaer getTema() {
        return tema;
    }

    public void setTema(Sakstemaer tema) {
        this.tema = tema;
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

    public String getSaksnummer() {
        return saksnummer;
    }

    public void setSaksnummer(String saksnummer) {
        this.saksnummer = saksnummer;
    }

    public boolean harAktivBehandling() {
        return hentAktivBehandling() != null;
    }

    public boolean harMinstEnBehandlingAvType(Behandlingstyper behandlingstype) {
        return behandlinger.stream()
            .anyMatch(behandling -> behandling.getType() == behandlingstype);
    }

    public Behandling hentAktivBehandling() {
        List<Behandling> aktiveBehandlinger = getBehandlinger().stream().filter(Behandling::erAktiv).toList();
        if (aktiveBehandlinger.size() > 1) {
            throw new TekniskException("Det finnes mer enn en aktiv behandling for sak " + saksnummer);
        } else if (aktiveBehandlinger.size() == 1) {
            return aktiveBehandlinger.get(0);
        } else {
            return null;
        }
    }

    public Behandling hentTidligstRegistrertBehandling() {
        return getBehandlinger().stream()
            .min(Comparator.comparing(Behandling::getRegistrertDato))
            .orElseThrow(() -> new IkkeFunnetException(FINNER_IKKE_BEHANDLINGER_FOR_FAGSAK + saksnummer));
    }

    public List<Behandling> hentBehandlingerSortertSynkendePåRegistrertDato() {
        return getBehandlinger().stream()
            .sorted(Comparator.comparing(RegistreringsInfo::getRegistrertDato).reversed())
            .toList();
    }

    public Behandling hentSistRegistrertBehandling() {
        return hentBehandlingerSortertSynkendePåRegistrertDato()
            .stream()
            .findFirst()
            .orElseThrow(() -> new IkkeFunnetException(FINNER_IKKE_BEHANDLINGER_FOR_FAGSAK + saksnummer));
    }

    public Behandling hentSistOppdatertBehandling() {
        return getBehandlinger().stream()
            .max(Comparator.comparing(Behandling::getEndretDato))
            .orElseThrow(() -> new IkkeFunnetException(FINNER_IKKE_BEHANDLINGER_FOR_FAGSAK + saksnummer));
    }

    public Behandling hentSistAktivBehandling() {
        return Optional.ofNullable(hentAktivBehandling()).orElse(hentSistOppdatertBehandling());
    }

    public Optional<Behandling> finnTidligstInaktivBehandling() {
        return getBehandlinger().stream()
            .filter(Behandling::erInaktiv)
            .min(Comparator.comparing(RegistreringsInfo::getRegistrertDato));
    }

    public Behandling hentTidligstInaktivBehandling() {
        return finnTidligstInaktivBehandling().orElseThrow(
            () -> new FunksjonellException("Finner ingen inaktiv behandling på fagsak " + saksnummer));
    }

    public Aktoer hentBruker() {
        return hentAktørMedRolleType(BRUKER);
    }

    public String hentBrukersAktørID() {
        if (hentBruker() == null) {
            throw new FunksjonellException("Finner ikke bruker på fagsak " + saksnummer);
        }
        return hentBruker().getAktørId();
    }

    public Optional<String> finnBrukersAktørID() {
        return Optional.ofNullable(hentBruker()).map(Aktoer::getAktørId);
    }

    public List<Aktoer> hentMyndigheter() {
        return hentAktørerMedRolleType(TRYGDEMYNDIGHET);
    }

    /**
     * Henter arbeidsgiver i tilfeller hvor det er forventet at det kun finnes en eller ingen
     */
    public Aktoer hentUnikArbeidsgiver() {
        return hentAktørMedRolleType(ARBEIDSGIVER);
    }

    public Aktoer hentVirksomhet() {
        return hentAktørMedRolleType(VIRKSOMHET);
    }

    public Optional<String> finnVirksomhetsOrgnr() {
        return Optional.ofNullable(hentVirksomhet()).map(Aktoer::getOrgnr);
    }

    private Aktoer hentAktørMedRolleType(Aktoersroller rolleType) {
        Collection<Aktoer> kandidater = hentAktørerMedRolleType(rolleType);
        if (kandidater.size() > 1) {
            throw new TekniskException("Det finnes mer enn en aktør med rollen " + rolleType.getBeskrivelse() + " for sak " + saksnummer);
        }
        return kandidater.stream().findFirst().orElse(null);
    }

    private List<Aktoer> hentAktørerMedRolleType(Aktoersroller rolleType) {
        if (rolleType == null) {
            return Collections.emptyList();
        }
        return aktører.stream()
            .filter(a -> rolleType == a.getRolle())
            .toList();
    }

    public boolean harAktørMedRolleType(Aktoersroller rolleType) {
        return !hentAktørerMedRolleType(rolleType).isEmpty();
    }

    /**
     * Henter myndighetens landkode fra institusjonsID som har format landkode:institusjonskode.
     */
    public Land_iso2 hentMyndighetLandkode() {
        return hentMyndighet().hentMyndighetLandkode();
    }

    public Aktoer hentMyndighet() {
        List<Aktoer> myndigheter = hentMyndigheter();
        if (myndigheter.isEmpty()) {
            throw new TekniskException("Finner ingen aktør med rolle " + TRYGDEMYNDIGHET + " for fagsak " + saksnummer);
        }
        if (myndigheter.size() > 1) {
            throw new TekniskException("Kan ikke hente landkode for en bestemt myndighet fordi finnes flere myndigheter");
        }
        return myndigheter.get(0);
    }

    /**
     * Henter representanten som representerer angitt {@link Representerer} eller {@code null} hvis ingen finnes.
     */
    private Optional<Aktoer> finnRepresentant(Representerer representerer) {
        Assert.notNull(representerer, "Representerer trengs for å hente representant.");
        return aktører.stream().filter(a -> REPRESENTANT.equals(a.getRolle()))
            .filter(a -> (representerer.equals(a.getRepresenterer()) || Representerer.BEGGE.equals(a.getRepresenterer())))
            .findFirst();
    }

    public Optional<Aktoer> finnRepresentantEllerFullmektig(Representerer representerer) {
        if (representerer == Representerer.BRUKER) {
            return finnRepresentant(Representerer.BRUKER).or(() -> finnFullmektig(Fullmaktstype.FULLMEKTIG_SØKNAD));
        } else if (representerer == Representerer.ARBEIDSGIVER) {
            return finnRepresentant(Representerer.ARBEIDSGIVER).or(() -> finnFullmektig(Fullmaktstype.FULLMEKTIG_ARBEIDSGIVER));
        } else {
            throw new FunksjonellException("Æææææh!");
        }
    }

    public Optional<Aktoer> finnFullmektig(Fullmaktstype fullmaktstype) {
        return aktører.stream()
            .filter(a -> FULLMEKTIG.equals(a.getRolle()))
            .filter(a -> a.getFullmakter().stream().anyMatch(fullmakt -> fullmakt.getType() == fullmaktstype))
            .findFirst();
    }

    public boolean kanEndreTypeOgTema() {
        return harAktivBehandling() && getBehandlinger().size() == 1;
    }

    public boolean erSakstypeEøs() {
        return erSakstypeEøs(type);
    }

    public static boolean erSakstypeEøs(Sakstyper sakstype) {
        return Sakstyper.EU_EOS == sakstype;
    }

    public boolean erSakstypeTrygdeavtale() {
        return erSakstypeTrygdeavtale(type);
    }

    public static boolean erSakstypeTrygdeavtale(Sakstyper sakstype) {
        return Sakstyper.TRYGDEAVTALE == sakstype;
    }

    public boolean erSakstypeFtrl() {
        return erSakstypeFtrl(type);
    }

    public static boolean erSakstypeFtrl(Sakstyper sakstype) {
        return Sakstyper.FTRL == sakstype;
    }

    public Aktoersroller getHovedpartRolle() {
        if (harAktørMedRolleType(BRUKER)) {
            return BRUKER;
        } else if (harAktørMedRolleType(VIRKSOMHET)) {
            return VIRKSOMHET;
        } else {
            throw new FunksjonellException("Fagsak må ha hovedpart - enten BRUKER eller VIRKSOMHET");
        }
    }

    public Boolean harBrukerRepresentant() {

        return aktører
            .stream()
            .filter(a -> a.getRolle() == REPRESENTANT)
            .anyMatch(aktoer -> aktoer.getRepresenterer() == Representerer.BRUKER || aktoer.getRepresenterer() == Representerer.BEGGE);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Fagsak that)) { // Implisitt nullsjekk
            return false;
        }
        return saksnummer != null && this.saksnummer.equals(that.saksnummer);
    }

    @Override
    public int hashCode() {
        return 31;
    }

}
