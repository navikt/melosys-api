package no.nav.melosys.tjenester.gui.graphql;

import graphql.schema.CoercingSerializeException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ScalarConfigTest {

    @Test
    void dateCoercing_parserStringMedRettDatoFormat() {
        var dateCoercing = ScalarConfig.dateCoercing();
        var serializedValue = dateCoercing.serialize("2019-08-03");

        assertThat(serializedValue).isInstanceOf(String.class).isEqualTo("2019-08-03");
    }


    @Test
    void dateCoercing_parserStringMedFeilDatoFormat_kasterException() {
        var dateCoercing = ScalarConfig.dateCoercing();

        assertThatThrownBy(() -> dateCoercing.serialize("20190101"))
            .isInstanceOf(CoercingSerializeException.class);
    }

    @Test
    void dateCoercing_parserStringMedTekst_kasterException() {
        var dateCoercing = ScalarConfig.dateCoercing();

        assertThatThrownBy(() -> dateCoercing.serialize("Tekst"))
            .isInstanceOf(CoercingSerializeException.class);
    }
}
