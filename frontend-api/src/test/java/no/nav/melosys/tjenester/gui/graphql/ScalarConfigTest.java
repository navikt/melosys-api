package no.nav.melosys.tjenester.gui.graphql;

import graphql.schema.CoercingSerializeException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ScalarConfigTest {
    @Test
    void dateCoercing_forsøkSerializeString_kasterException() {
        var dateCoercing = ScalarConfig.dateCoercing();

        assertThatThrownBy(() -> dateCoercing.serialize("2019-01-01"))
            .isInstanceOf(CoercingSerializeException.class);
    }

    @Test
    void dateCoercing_parserStringMedTekst_kasterException() {
        var dateCoercing = ScalarConfig.dateCoercing();

        assertThatThrownBy(() -> dateCoercing.serialize("Tekst"))
            .isInstanceOf(CoercingSerializeException.class);
    }
}
