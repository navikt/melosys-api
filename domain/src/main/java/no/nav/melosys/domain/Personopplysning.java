package no.nav.melosys.domain;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.persistence.*;

import no.nav.melosys.domain.dokument.person.Person;
import no.nav.melosys.domain.dokument.person.PersonConverter;

@Entity
@Table(name = "personopplysning")
public class Personopplysning {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @OneToOne
    @JoinColumn(name = "behandling_id")
    public Behandling behandling;

    @Convert(converter = PersonConverter.class)
    public Person person;

    @OneToMany(mappedBy = "personopplysning", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private Set<PersonopplysningKilde> kilder = new HashSet<>(1);

    public Personopplysning() {
    }

    public Personopplysning(Behandling behandling, Person person, List<String> kilder) {
        this.behandling = behandling;
        this.person = person;
        this.kilder = kilder.stream()
            .map(kilde -> new PersonopplysningKilde(this, kilde))
            .collect(Collectors.toSet());
    }
}
