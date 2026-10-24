package com.wildfiresync.server.store;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.springframework.stereotype.Component;

/**
 * One transaction at a time against Postgres, the server's counterpart of the tablet's
 * {@code LocalDatabase.transaction}.
 *
 * <p>Plain JDBC rather than JPA: the events table is the source of truth and the other tables are
 * rebuilt from it, so there is no object graph to map, and the merge has to read a record's whole
 * event list and rewrite its rows in one transaction. The work the tablet does in
 * {@code ReportService} is the same work, so it is written the same way.
 */
@Component
public class Database {

	private final DataSource dataSource;

	public Database(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	/** Runs the block in one transaction, committing it or rolling all of it back. */
	public <T> T inTransaction(Work<T> work) {
		try (Connection connection = dataSource.getConnection()) {
			boolean autoCommit = connection.getAutoCommit();
			connection.setAutoCommit(false);
			try {
				T result = work.run(connection);
				connection.commit();
				return result;
			}
			catch (RuntimeException | SQLException ex) {
				connection.rollback();
				throw ex;
			}
			finally {
				connection.setAutoCommit(autoCommit);
			}
		}
		catch (SQLException ex) {
			// the consumer turns this into a nak, so the broker redelivers the event
			throw new IllegalStateException("the database refused the transaction: " + ex.getMessage(), ex);
		}
	}

	@FunctionalInterface
	public interface Work<T> {

		T run(Connection connection) throws SQLException;

	}

}
