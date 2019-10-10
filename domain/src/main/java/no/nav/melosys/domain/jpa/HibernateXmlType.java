package no.nav.melosys.domain.jpa;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.usertype.UserType;

/**
 * Klasse som beskriver mappingen fra {@link String} til en {@link Types}. Hvilken type beskrives av delegaten. Dette fordi det
 * var vanskelig å registrere nye hibernate typer utenom {@link org.hibernate.annotations.TypeDef} i Entitetene.
 * <p>
 *
 */
public class HibernateXmlType implements UserType, Serializable {

    private static final long serialVersionUID = 4432961273119959013L;

    /**
     * Delegate for setting the JDBC type and means of setting and getting the object. Set another to use ex. with Oracle
     * XmlType.
     * <p>
     */
    public static HibernateXMLTypeDelegate delegate = HibernateSqlXmlTypeDelegate.INSTANCE;

    public HibernateXmlType() {
    }

    @Override
    public int[] sqlTypes() {
        return delegate.sqlTypes();
    }

    @Override
    public Class returnedClass() {
        return String.class;
    }

    @Override
    public boolean equals(Object x, Object y) {
        if ((x == null) && (y == null)) {
            return true;
        } else {
            return (x != null) && x.equals(y);
        }
    }

    @Override
    public int hashCode(Object x) {
        return x.hashCode();
    }

    @Override
    public Object nullSafeGet(ResultSet rs, String[] names, SharedSessionContractImplementor session, Object owner) throws HibernateException, SQLException {
        return delegate.nullSafeGet(rs, names, session, owner);
    }

    @Override
    public void nullSafeSet(PreparedStatement st, Object value, int index, SharedSessionContractImplementor session) throws HibernateException, SQLException {
        delegate.nullSafeSet(st, value, index, session);
    }

    @Override
    public Object deepCopy(Object value) {
        if (value == null) {
            return null;
        } else {
            return value;
        }
    }

    @Override
    public boolean isMutable() {
        return false;
    }

    @Override
    public Serializable disassemble(Object value) {
        try {
            return (Serializable) value;
        } catch (Exception e) {
            throw new HibernateException("Could not disassemble Document to Serializable", e);
        }
    }

    @Override
    public Object assemble(Serializable cached, Object owner) {
        try {
            return cached;
        } catch (Exception e) {
            throw new HibernateException("Could not assemble String to Document", e);
        }
    }

    @Override
    public Object replace(Object original, Object target, Object owner) {
        return original;
    }

    public interface HibernateXMLTypeDelegate {

        int[] sqlTypes();

        Object nullSafeGet(ResultSet rs, String[] names, SharedSessionContractImplementor session, Object owner) throws SQLException;

        void nullSafeSet(PreparedStatement st, Object value, int index, SharedSessionContractImplementor session) throws SQLException;
    }
}