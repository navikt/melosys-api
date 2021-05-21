package no.nav.melosys.domain.dokument.jaxb;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class LocalDateXmlAdapterTest {

    private LocalDateXmlAdapter adapter = new LocalDateXmlAdapter();

    @Test
    public void unmarshal() throws Exception {
        String s = "1987-08-25+02:00";
        LocalDate unmarshal = adapter.unmarshal(s);
        assertThat(unmarshal).isBefore(LocalDate.of(2016, 10, 1));
    }

    @Test
    public void marshal() throws Exception {
        LocalDate date = LocalDate.of(2017, 9, 21);
        String marshal = adapter.marshal(date);
        assertThat(marshal).isEqualTo("2017-09-21");
    }

}
