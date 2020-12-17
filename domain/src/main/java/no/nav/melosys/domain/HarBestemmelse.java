package no.nav.melosys.domain;

import no.nav.melosys.domain.kodeverk.Bestemmelse;

public interface HarBestemmelse<T extends Bestemmelse> {
    T getBestemmelse();
}
