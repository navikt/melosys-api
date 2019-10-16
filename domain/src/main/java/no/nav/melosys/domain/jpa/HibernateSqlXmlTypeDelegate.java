package no.nav.melosys.domain.jpa;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLXML;
import java.sql.Types;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

public class HibernateSqlXmlTypeDelegate implements HibernateXmlType.HibernateXMLTypeDelegate {

    public static final HibernateXmlType.HibernateXMLTypeDelegate INSTANCE = new HibernateSqlXmlTypeDelegate();

    @Override
    public int[] sqlTypes() {
        return new int[] { Types.SQLXML };
    }

    @Override
    public Object nullSafeGet(ResultSet rs, String[] names, SharedSessionContractImplementor session, Object owner) throws SQLException {
        SQLXML sqlxml = rs.getSQLXML(names[0]);
        if (sqlxml != null) {
            return sqlxml.getString();
        }
        return null;
    }

    @Override
    public void nullSafeSet(PreparedStatement st, Object value, int index, SharedSessionContractImplementor session) throws SQLException {
        SQLXML sqlxml = st.getConnection().createSQLXML();
        sqlxml.setString((String) value);
        st.setObject(index, sqlxml);
    }
}