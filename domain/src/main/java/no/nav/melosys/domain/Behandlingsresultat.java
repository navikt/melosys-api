package no.nav.melosys.domain;

import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.persistence.*;

import no.nav.melosys.domain.avklartefakta.Avklartefakta;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Utfallregistreringunntak;
import no.nav.melosys.domain.kodeverk.Vilkaar;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "behandlingsresultat")
@EntityListeners(AuditingEntityListener.class)
public class Behandlingsresultat extends RegistreringsInfo {
    // Populeres av Hibernate med behandling.id
    @Id
    private Long id;

    @MapsId
    @OneToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "behandling_id")
    private Behandling behandling;

    @Enumerated(EnumType.STRING)
    @Column(name = "behandlingsmaate", nullable = false)
    private Behandlingsmaate behandlingsmåte;

    @Enumerated(EnumType.STRING)
    @Column(name = "resultat_type", nullable = false)
    private Behandlingsresultattyper type;

    @Enumerated(EnumType.STRING)
    @Column(name = "fastsatt_av_land")
    private Landkoder fastsattAvLand;

    @Column(name = "begrunnelse_fritekst")
    private String begrunnelseFritekst;

    @OneToOne(mappedBy = "behandlingsresultat", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private VedtakMetadata vedtakMetadata;

    @Enumerated(EnumType.STRING)
    @Column(name = "utfall_registrering_unntak")
    private Utfallregistreringunntak utfallRegistreringUnntak;

    @OneToMany(mappedBy = "behandlingsresultat", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private Set<Avklartefakta> avklartefakta = new HashSet<>(1);

    @OneToMany(mappedBy = "behandlingsresultat", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private Set<Lovvalgsperiode> lovvalgsperioder = new HashSet<>(1);

    @OneToMany(mappedBy = "behandlingsresultat", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private Set<Anmodningsperiode> anmodningsperioder = new HashSet<>(1);

    @OneToMany(mappedBy = "behandlingsresultat", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private Set<Vilkaarsresultat> vilkaarsresultater = new HashSet<>(1);

    @OneToMany(mappedBy = "behandlingsresultat", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private Set<Kontrollresultat> kontrollresultater = new HashSet<>(1);

    @OneToMany(mappedBy = "behandlingsresultat", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    private Set<BehandlingsresultatBegrunnelse> behandlingsresultatBegrunnelser = new HashSet<>(1);

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Behandling getBehandling() {
        return behandling;
    }

    public void setBehandling(Behandling behandling) {
        this.behandling = behandling;
    }

    public Behandlingsmaate getBehandlingsmåte() {
        return behandlingsmåte;
    }

    public void setBehandlingsmåte(Behandlingsmaate behandlingsmåte) {
        this.behandlingsmåte = behandlingsmåte;
    }

    public Behandlingsresultattyper getType() {
        return type;
    }

    public void setType(Behandlingsresultattyper type) {
        this.type = type;
    }

    public Landkoder getFastsattAvLand() {
        return fastsattAvLand;
    }

    public void setFastsattAvLand(Landkoder fastsattAvLand) {
        this.fastsattAvLand = fastsattAvLand;
    }

    public String getBegrunnelseFritekst() {
        return begrunnelseFritekst;
    }

    public void setBegrunnelseFritekst(String begrunnelseFritekst) {
        this.begrunnelseFritekst = begrunnelseFritekst;
    }

    public VedtakMetadata getVedtakMetadata() {
        return vedtakMetadata;
    }

    public void setVedtakMetadata(final VedtakMetadata vedtakMetadata) {
        this.vedtakMetadata = vedtakMetadata;
    }

    public Utfallregistreringunntak getUtfallRegistreringUnntak() {
        return utfallRegistreringUnntak;
    }

    public void setUtfallRegistreringUnntak(Utfallregistreringunntak utfallRegistreringUnntak) {
        this.utfallRegistreringUnntak = utfallRegistreringUnntak;
    }

    public Set<Lovvalgsperiode> getLovvalgsperioder() {
        return lovvalgsperioder;
    }

    public void setLovvalgsperioder(Set<Lovvalgsperiode> lovvalgsperioder) {
        this.lovvalgsperioder = lovvalgsperioder;
    }

    public Set<Anmodningsperiode> getAnmodningsperioder() {
        return anmodningsperioder;
    }

    public void setAnmodningsperioder(Set<Anmodningsperiode> anmodningsperioder) {
        this.anmodningsperioder = anmodningsperioder;
    }

    public Set<Vilkaarsresultat> getVilkaarsresultater() {
        return vilkaarsresultater;
    }

    public void setVilkaarsresultater(Set<Vilkaarsresultat> vilkaarsresultater) {
        this.vilkaarsresultater = vilkaarsresultater;
    }

    public Set<Avklartefakta> getAvklartefakta() {
        return avklartefakta;
    }

    public void setAvklartefakta(Set<Avklartefakta> avklartefakta) {
        this.avklartefakta = avklartefakta;
    }

    public Set<BehandlingsresultatBegrunnelse> getBehandlingsresultatBegrunnelser() {
        return behandlingsresultatBegrunnelser;
    }

    public void setBehandlingsresultatBegrunnelser(Set<BehandlingsresultatBegrunnelse> behandlingsresultatBegrunnelser) {
        this.behandlingsresultatBegrunnelser = behandlingsresultatBegrunnelser;
    }

    public Set<Kontrollresultat> getKontrollresultater() {
        return kontrollresultater;
    }

    public void setKontrollresultater(Set<Kontrollresultat> kontrollresultater) {
        this.kontrollresultater = kontrollresultater;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Behandlingsresultat)) {
            return false;
        }
        Behandlingsresultat that = (Behandlingsresultat) o;
        return Objects.equals(this.type, that.type)
            && Objects.equals(this.behandling, that.behandling);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, behandling);
    }

    public boolean erAvslag() {
        if (type == Behandlingsresultattyper.AVSLAG_MANGLENDE_OPPL) {
            return true;
        }
        Lovvalgsperiode lovvalgsperiode = hentValidertLovvalgsperiode();
        return type == Behandlingsresultattyper.FASTSATT_LOVVALGSLAND
            && lovvalgsperiode.erAvslått();
    }

    public boolean erAnmodningOmUnntak() {
        return type == Behandlingsresultattyper.ANMODNING_OM_UNNTAK;
    }

    public boolean erInnvilgelse() {
        if (type == Behandlingsresultattyper.FASTSATT_LOVVALGSLAND) {
            Lovvalgsperiode lovvalgsperiode = hentValidertLovvalgsperiode();
            return lovvalgsperiode.erInnvilget();
        } else {
            return false;
        }
    }

    public boolean erInnvilgelseFlereLand() {
        if (type == Behandlingsresultattyper.FASTSATT_LOVVALGSLAND) {
            Lovvalgsperiode lovvalgsperiode = hentValidertLovvalgsperiode();
            return lovvalgsperiode.erInnvilget() && lovvalgsperiode.erArtikkel13();
        } else {
            return false;
        }
    }

    public boolean erArtikkel16MedSendtAnmodningOmUnntak() {
        return anmodningsperioder.stream().anyMatch(Anmodningsperiode::erSendtUtland);
    }

    public boolean erArt16EtterUtlandMedRegistrertSvar() {
        if (anmodningsperioder.isEmpty()) {
            return false;
        }

        Anmodningsperiode anmodningsperiode = hentValidertAnmodningsperiode();
        return anmodningsperiode.getAnmodningsperiodeSvar() != null
            && anmodningsperiode.getAnmodningsperiodeSvar().getAnmodningsperiodeSvarType() != null;
    }

    // Medl skal ikke oppdateres ved avslag.
    public boolean medlOppdateres() {
        return harMedlPeriode() || !erAvslag();
    }

    private boolean harMedlPeriode() {
        return lovvalgsperioder.stream().anyMatch(l -> l.getMedlPeriodeID() != null);
    }

    public boolean harMedlemskapsperiode() {
        return !lovvalgsperioder.isEmpty() || !anmodningsperioder.isEmpty();
    }

    public Medlemskapsperiode hentValidertMedlemskapsperiode() {
        if (!lovvalgsperioder.isEmpty()) {
            return hentValidertLovvalgsperiode();
        } else {
            return hentValidertAnmodningsperiode();
        }
    }

    public Lovvalgsperiode hentValidertLovvalgsperiode() {
        if (lovvalgsperioder.isEmpty()) {
            throw new NoSuchElementException("Ingen lovvalgsperiode finnes for behandlingsresultat " + id);
        }
        if (lovvalgsperioder.size() > 1) {
            throw new UnsupportedOperationException("Flere enn en lovvalgsperiode er ikke støttet");
        }
        return lovvalgsperioder.iterator().next();
    }

    public Anmodningsperiode hentValidertAnmodningsperiode() {
        if (anmodningsperioder.isEmpty()) {
            throw new NoSuchElementException("Ingen anmodningsperioder finnes for behandlingsresultat " + id);
        }
        if (anmodningsperioder.size() > 1) {
            throw new UnsupportedOperationException("Flere enn en anmodningsperiode er ikke støttet");
        }
        return anmodningsperioder.iterator().next();
    }

    public Set<VilkaarBegrunnelse> hentVilkaarbegrunnelser(Vilkaar vilkaarType) {
        return getVilkaarsresultater().stream()
            .filter(vr -> vr.getVilkaar() == vilkaarType)
            .flatMap(vr -> vr.getBegrunnelser().stream())
            .collect(Collectors.toSet());
    }

    public boolean erAutomatisert() {
        return behandlingsmåte == Behandlingsmaate.AUTOMATISERT
            || behandlingsmåte == Behandlingsmaate.DELVIS_AUTOMATISERT;
    }

    public boolean harVedtak() {
        return vedtakMetadata != null;
    }

    @Override
    public String toString() {
        return "Behandlingsresultat{" +
            "id=" + id +
            ", type=" + type +
            '}';
    }
}
