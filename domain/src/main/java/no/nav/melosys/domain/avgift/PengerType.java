package no.nav.melosys.domain.avgift;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Objects;
import java.util.Properties;

import no.nav.melosys.exception.TekniskException;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.BigDecimalType;
import org.hibernate.type.StringType;
import org.hibernate.type.Type;
import org.hibernate.usertype.CompositeUserType;
import org.hibernate.usertype.DynamicParameterizedType;

public class PengerType implements CompositeUserType, DynamicParameterizedType {

    private String[] propertyNames;

    @Override
    public void setParameterValues(Properties parameters) {
        this.propertyNames = new String[]{
            parameters.getProperty("verdiPropertyName", "verdi"),
            parameters.getProperty("valutaPropertyName", "valuta")
        };
    }

    @Override
    public String[] getPropertyNames() {
        return propertyNames;
    }

    @Override
    public Type[] getPropertyTypes() {
        return new Type[]{BigDecimalType.INSTANCE, StringType.INSTANCE};
    }

    @Override
    public Object getPropertyValue(Object component, int property) throws HibernateException {
        Penger penger = (Penger) component;

        if (property == 0) {
            return penger.getVerdi();
        } else if (property == 1) {
            return penger.getValuta();
        }
        throw new TekniskException(property + " er en ugyldig property index for PengerType");
    }

    @Override
    public void setPropertyValue(Object component, int property, Object value) throws HibernateException {
        Penger penger = (Penger) component;

        if (property == 0) {
            penger.setVerdi((BigDecimal) value);
        } else if (property == 1) {
            penger.setValuta((String) value);
        } else {
            throw new TekniskException(property + " er en ugyldig property index for PengerType");
        }
    }

    @Override
    public Class returnedClass() {
        return Penger.class;
    }

    @Override
    public boolean equals(Object x, Object y) throws HibernateException {
        if (x == y)
            return true;

        if (Objects.isNull(x) || Objects.isNull(y))
            return false;

        return x.equals(y);
    }

    @Override
    public int hashCode(Object x) throws HibernateException {
        return x.hashCode();
    }

    @Override
    public Object nullSafeGet(ResultSet rs, String[] names, SharedSessionContractImplementor session, Object owner) throws HibernateException, SQLException {
        Penger penger = new Penger(rs.getBigDecimal(names[0]), rs.getString(names[1]));

        if (rs.wasNull())
            return null;

        return penger;
    }

    @Override
    public void nullSafeSet(PreparedStatement st, Object value, int index, SharedSessionContractImplementor session) throws HibernateException, SQLException {
        if (Objects.isNull(value)) {
            st.setNull(index, Types.DECIMAL);
            st.setNull(index + 1, Types.VARCHAR);
        } else {
            Penger penger = (Penger) value;
            st.setBigDecimal(index, penger.getVerdi());
            st.setString(index + 1, penger.getValuta());
        }
    }

    @Override
    public Object deepCopy(Object value) throws HibernateException {
        if (Objects.isNull(value))
            return null;

        Penger oldPenger = (Penger) value;

        return new Penger(oldPenger.getVerdi(), oldPenger.getValuta());
    }

    @Override
    public boolean isMutable() {
        return true;
    }

    @Override
    public Serializable disassemble(Object value, SharedSessionContractImplementor session) throws HibernateException {
        return (Serializable) deepCopy(value);
    }

    @Override
    public Object assemble(Serializable cached, SharedSessionContractImplementor session, Object owner) throws HibernateException {
        return deepCopy(cached);
    }

    @Override
    public Object replace(Object original, Object target, SharedSessionContractImplementor session, Object owner) throws HibernateException {
        return original;
    }
}
