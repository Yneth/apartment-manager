package ua.abond.lab4.util.jdbc;

import ua.abond.lab4.util.jdbc.exception.DataAccessException;
import ua.abond.lab4.util.jdbc.util.ConnectionUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class Jdbc {
    private final DataSource dataSource;

    private boolean userManaged;
    private Connection connection;

    public Jdbc(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public int update(PreparedStatementCreator psc, KeyHolder keyHolder) {
        Connection conn = getConnection();
        try (PreparedStatement ps = psc.create(conn)) {
            int count = ps.executeUpdate();

            ResultSet generatedKeys = ps.getGeneratedKeys();

            while (generatedKeys.next()) {
                keyHolder.setKey(generatedKeys.getLong(1));
            }

            commit(conn);
            return count;
        } catch (SQLException e) {
            rollback(conn);
            throw new DataAccessException("Failed to execute update.", e);
        } finally {
            ConnectionUtils.closeConnection(conn);
        }
    }

    public void execute(String sql, PreparedStatementSetter setter)
            throws DataAccessException {
        Objects.requireNonNull(sql, "Sql should not be null");

        Connection conn = getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            setter.set(ps);

            ps.execute();
            commit(conn);
        } catch (SQLException e) {
            rollback(conn);
            throw new DataAccessException("Failed to execute query.", e);
        } finally {
            close(conn);
        }
    }

    public <T> List<T> query(String sql,
                             PreparedStatementSetter pss,
                             RowMapper<T> rsm)
            throws DataAccessException {
        Objects.requireNonNull(sql, "Sql should not be null");

        List<T> result = null;
        Connection conn = getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            pss.set(ps);

            List<T> values = new ArrayList<>();
            ResultSet resultSet = ps.executeQuery();
            while (resultSet.next()) {
                values.add(rsm.mapRow(resultSet));
            }
            result = values;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to execute query.", e);
        } finally {
            close(conn);
        }
        return result;
    }

    public <T> List<T> query(String sql, RowMapper<T> rsm)
            throws DataAccessException {
        return query(sql, new DefaultPreparedStatementSetter(), rsm);
    }

    public <T> Optional<T> querySingle(String sql, RowMapper<T> rm)
            throws DataAccessException {
        return querySingle(sql, new DefaultPreparedStatementSetter(), rm);
    }

    public <T> Optional<T> querySingle(String sql,
                                       PreparedStatementSetter pss,
                                       RowMapper<T> rsm)
            throws DataAccessException {
        List<T> query = query(sql, pss, rsm);
        return query.stream().findFirst();
    }

    public void beginTransaction() {
        userManaged = true;
    }

    public void endTransaction() {
        userManaged = false;
    }

    public void commit() {
        if (!userManaged || connection == null)
            return;
        try {
            commit(connection);
        } catch (SQLException e) {
            throw new DataAccessException("Failed to commit transaction.");
        }
        close(connection);
    }

    public void rollback() {
        if (!userManaged || connection == null)
            return;
    }

    private Connection getConnection() {
        if (connection != null) {
            return connection;
        }
        return ConnectionUtils.getConnection(dataSource);
    }

    private void commit(Connection connection) throws SQLException {
        if (!userManaged)
            connection.commit();
    }

    private void rollback(Connection connection) {
        if (!userManaged)
            ConnectionUtils.rollback(connection);
    }

    private void close(Connection connection) {
        if (!userManaged)
            ConnectionUtils.closeConnection(connection);
    }

    private static class DefaultPreparedStatementSetter implements PreparedStatementSetter {

        @Override
        public void set(PreparedStatement ps) throws SQLException {

        }
    }
}
