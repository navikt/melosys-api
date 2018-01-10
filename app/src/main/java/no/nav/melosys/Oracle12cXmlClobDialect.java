package no.nav.melosys;

import org.hibernate.dialect.Oracle12cDialect;

/**
 * Hvis en XMLType har mer enn 4000 tegn gir Hibernate med Oracle12cDialect en Oracle feil ORA-01461.
 * Den klassen tvinger Hibernate til å bruke CLOB slik at vi unngår feilen.
 */
public class Oracle12cXmlClobDialect extends Oracle12cDialect {

    /**
     * Tvinger Hibernate til å bruke CLOB binding i stedet for PreparedStatement::setBinaryStream
     */
    @Override
    public boolean useInputStreamToInsertBlob() {
        return false;
    }
}
