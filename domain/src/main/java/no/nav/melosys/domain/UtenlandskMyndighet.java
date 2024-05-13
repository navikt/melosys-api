package no.nav.melosys.domain;

import java.util.*;
import jakarta.persistence.*;

import no.nav.melosys.domain.adresse.StrukturertAdresse;
import no.nav.melosys.domain.kodeverk.Land_iso2;

@Entity
@Table(name = "utenlandsk_myndighet")
public class UtenlandskMyndighet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String institusjonskode;

    private String navn;

    @Column(name="gateadresse_1")
    private String gateadresse1;

    @Column(name="gateadresse_2")
    private String gateadresse2;

    private String postnummer;

    private String poststed;

    private String land;

    @ManyToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinTable(
        name = "utenlandsk_myndighet_pref",
        joinColumns = @JoinColumn(name = "utenlandsk_myndighet_id",insertable = false, updatable = false),
        inverseJoinColumns = @JoinColumn(name = "preferanse_id", insertable = false, updatable = false)
    )
    private Set<Preferanse> preferanser = new HashSet<>();

    @Enumerated(EnumType.STRING)
    private Land_iso2 landkode;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UtenlandskMyndighet that)) return false;
        return institusjonskode.equals(that.institusjonskode) &&
            land.equals(that.land);
    }

    @Override
    public int hashCode() {
        return Objects.hash(institusjonskode, land);
    }

    public StrukturertAdresse getAdresse() {
        StrukturertAdresse adresse = new StrukturertAdresse();
        adresse.setGatenavn(getKombinertGateadresse());
        adresse.setPostnummer(postnummer);
        adresse.setPoststed(poststed);
        adresse.setLandkode(landkode.getKode());
        return adresse;
    }

    public String getKombinertGateadresse() {
        if (gateadresse2 == null || gateadresse2.isEmpty()) {
            return gateadresse1;
        }
        return "%s, %s".formatted(gateadresse1, gateadresse2);
    }

    public List<String> getGateadresseAsList() {
        List<String> gateadresse = new ArrayList<>();
        if (gateadresse1 != null) {
            gateadresse.add(gateadresse1);
        }
        if (gateadresse2 != null) {
            gateadresse.add(gateadresse2);
        }
        return gateadresse;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getInstitusjonskode() {
        return institusjonskode;
    }

    public void setInstitusjonskode(String institusjonskode) {
        this.institusjonskode = institusjonskode;
    }

    public String getNavn() {
        return navn;
    }

    public void setNavn(String navn) {
        this.navn = navn;
    }

    public String getGateadresse1() {
        return gateadresse1;
    }

    public void setGateadresse1(String gateadresse1) {
        this.gateadresse1 = gateadresse1;
    }

    public String getGateadresse2() {
        return gateadresse2;
    }

    public void setGateadresse2(String gateadresse2) {
        this.gateadresse2 = gateadresse2;
    }

    public String getPostnummer() {
        return postnummer;
    }

    public void setPostnummer(String postnummer) {
        this.postnummer = postnummer;
    }

    public String getPoststed() {
        return poststed;
    }

    public void setPoststed(String poststed) {
        this.poststed = poststed;
    }

    public String getLand() {
        return land;
    }

    public void setLand(String land) {
        this.land = land;
    }

    public Set<Preferanse> getPreferanser() {
        return preferanser;
    }

    public void setPreferanser(Set<Preferanse> preferanser) {
        this.preferanser = preferanser;
    }

    public Land_iso2 getLandkode() {
        return landkode;
    }

    public void setLandkode(Land_iso2 landkode) {
        this.landkode = landkode;
    }

    public String hentInstitusjonID() {
        if (institusjonskode == null) {
            return landkode.getKode();
        }
        return landkode + ":" + institusjonskode;
    }

    public static Land_iso2 konverterInstitusjonIdTilLandkode(String institusjonID) {
        return Land_iso2.valueOf(institusjonID.split(":")[0].replaceAll("[^a-zA-Z_]", ""));
    }
}
