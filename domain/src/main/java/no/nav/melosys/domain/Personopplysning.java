package no.nav.melosys.domain;

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

    public Personopplysning() {
    }

    public Personopplysning(Behandling behandling, Person person) {
        this.behandling = behandling;
        this.person = person;
    }
}
