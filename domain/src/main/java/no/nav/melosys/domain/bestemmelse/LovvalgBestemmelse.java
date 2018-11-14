package no.nav.melosys.domain.bestemmelse;

import no.nav.melosys.domain.Kodeverk;

public interface LovvalgBestemmelse extends Kodeverk {

    String name();

    // Til Hibernate https://hibernate.atlassian.net/browse/HHH-10858
    boolean equals(Object other);

    // Til Hibernate https://hibernate.atlassian.net/browse/HHH-10858
    int hashCode();
}
