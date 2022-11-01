package no.nav.melosys.domain;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import javax.persistence.*;

import no.nav.melosys.domain.adresse.StrukturertAdresse;
import no.nav.melosys.domain.kodeverk.Land_iso2;

@Entity
@Table(name = "utenlandsk_myndighet")
public class UtenlandskMyndighet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    public String institusjonskode;

    public String navn;

    public String gateadresse_1;

    public String gateadresse_2;

    public String postnummer;

    public String poststed;

    public String land;

    @ManyToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinTable(
        name = "utenlandsk_myndighet_pref",
        joinColumns = @JoinColumn(name = "utenlandsk_myndighet_id", updatable = false),
        inverseJoinColumns = @JoinColumn(name = "preferanse_id", updatable = false)
    )
    public Set<Preferanse> preferanser = new HashSet<>();

    @Enumerated(EnumType.STRING)
    public Land_iso2 landkode;

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
        if (gateadresse_2 == null || gateadresse_2.isEmpty()) {
            return gateadresse_1;
        }
        return "%s, %s".formatted(gateadresse_1,gateadresse_2);
    }

    public List<String> getGateadresseAsList() {
        var gateadresse = List.of(gateadresse_1);
        if (gateadresse_2 != null) {
            gateadresse.add(gateadresse_2);
        }
        return gateadresse;
    }

    public String hentInstitusjonID() {
        if (institusjonskode == null) {
            return landkode.getKode();
        }
        return landkode + ":" + institusjonskode;
    }

    public static Land_iso2 konverterInstitusjonIdTilLandkode(String institusjonID) {
        return Land_iso2.valueOf(institusjonID.split(":")[0].replaceAll("[^a-zA-Z]", ""));
    }
}
