package no.nav.melosys.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
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

    @Column(name = "vedtak_dato")
    private Instant vedtaksdato;

    @Column(name = "vedtak_klagefrist")
    private LocalDate vedtakKlagefrist;

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

    public Instant getVedtaksdato() {
        return vedtaksdato;
    }

    public void setVedtaksdato(Instant vedtaksdato) {
        this.vedtaksdato = vedtaksdato;
    }

    public LocalDate getVedtakKlagefrist() {
        return vedtakKlagefrist;
    }

    public void setVedtakKlagefrist(LocalDate vedtakKlagefrist) {
        this.vedtakKlagefrist = vedtakKlagefrist;
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

    // Medl skal ikke oppdateres ved avslag.
    public boolean medlOppdateres() {
        return harMedlPeriode() || !erAvslag();
    }

    private boolean harMedlPeriode() {
        return lovvalgsperioder.stream().anyMatch(l -> l.getMedlPeriodeID() != null);
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
            throw new UnsupportedOperationException("Flere enn en"
                + " lovvalgsperiode er ikke støttet i første leveranse");
        }
        return lovvalgsperioder.iterator().next();
    }

    public Anmodningsperiode hentValidertAnmodningsperiode() {
        if (anmodningsperioder.isEmpty()) {
            throw new NoSuchElementException("Ingen anmodningsperioder finnes for behandlingsresultat " + id);
        }
        if (anmodningsperioder.size() > 1) {
            throw new UnsupportedOperationException("Flere enn en"
                + " anmodningsperiode er ikke støttet i første leveranse");
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
}
