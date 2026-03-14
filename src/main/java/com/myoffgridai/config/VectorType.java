package com.myoffgridai.config;

import com.pgvector.PGvector;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.usertype.UserType;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Arrays;

/**
 * Hibernate custom {@link UserType} that maps {@code float[]} Java fields
 * to pgvector {@code vector} columns in PostgreSQL.
 *
 * <p>Uses the pgvector-java library ({@link PGvector}) for JDBC-level
 * conversion between float arrays and the PostgreSQL vector type.</p>
 */
public class VectorType implements UserType<float[]> {

    @Override
    public int getSqlType() {
        return Types.OTHER;
    }

    @Override
    public Class<float[]> returnedClass() {
        return float[].class;
    }

    @Override
    public boolean equals(float[] x, float[] y) {
        return Arrays.equals(x, y);
    }

    @Override
    public int hashCode(float[] x) {
        return Arrays.hashCode(x);
    }

    @Override
    public float[] nullSafeGet(ResultSet rs, int position,
                                SharedSessionContractImplementor session,
                                Object owner) throws SQLException {
        Object obj = rs.getObject(position);
        if (obj == null) {
            return null;
        }
        // The pgvector column returns a PGobject; parse its string value
        String value = obj.toString();
        if (value.isEmpty()) {
            return null;
        }
        try {
            PGvector vector = new PGvector(value);
            return vector.toArray();
        } catch (SQLException e) {
            throw new SQLException("Failed to parse pgvector value: " + value, e);
        }
    }

    @Override
    public void nullSafeSet(PreparedStatement st, float[] value, int index,
                             SharedSessionContractImplementor session) throws SQLException {
        if (value == null) {
            st.setNull(index, Types.OTHER);
        } else {
            PGvector vector = new PGvector(value);
            st.setObject(index, vector);
        }
    }

    @Override
    public float[] deepCopy(float[] value) {
        if (value == null) {
            return null;
        }
        return value.clone();
    }

    @Override
    public boolean isMutable() {
        return true;
    }

    @Override
    public Serializable disassemble(float[] value) {
        return deepCopy(value);
    }

    @Override
    public float[] assemble(Serializable cached, Object owner) {
        if (cached instanceof float[] arr) {
            return deepCopy(arr);
        }
        return null;
    }
}
