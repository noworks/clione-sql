package tetz42.util;

import java.io.Closeable;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import tetz42.clione.exception.UnsupportedTypeException;
import tetz42.util.exception.IORuntimeException;
import tetz42.util.exception.ResourceClosingException;
import tetz42.util.exception.SQLRuntimeException;
import tetz42.util.exception.WrapException;

public abstract class Using<T> {

	private List<Closeable> ioList = new ArrayList<Closeable>();
	private List<Closeable> rsList = new ArrayList<Closeable>();
	private List<Closeable> stmtList = new ArrayList<Closeable>();
	private List<Connection> cons = new ArrayList<Connection>();

	public Using(Object... resources) {
		addResources(resources);
	}

	protected final void addResources(Object... resources) {
		for (final Object res : resources) {
			if (res instanceof Closeable) {
				this.ioList.add((Closeable) res);
			} else if (res instanceof ResultSet) {
				final ResultSet rs = (ResultSet) res;
				this.rsList.add(new Closeable() {
					@Override
					public void close() {
						try {
							rs.close();
						} catch (SQLException e) {
							throw new ResourceClosingException(e);
						}
					}
				});
			} else if (res instanceof Statement) {
				final Statement stmt = (Statement) res;
				this.stmtList.add(new Closeable() {
					@Override
					public void close() {
						try {
							stmt.close();
						} catch (SQLException e) {
							throw new ResourceClosingException(e);
						}
					}
				});
			} else if (res instanceof Connection) {
				this.cons.add((Connection) res);
			} else {
				throw new UnsupportedTypeException("Using does not support "
						+ res.getClass().getName());
			}
		}
	}

	public final T invoke() {
		RuntimeException re = null;
		Error err = null;
		try {
			return execute();
		} catch (RuntimeException e) {
			throw re = e;
		} catch (SQLException e) {
			throw re = new SQLRuntimeException(e);
		} catch (IOException e) {
			throw re = new IORuntimeException(e);
		} catch (Exception e) {
			throw re = new WrapException(e);
		} catch (Error e) {
			throw err = e;
		} finally {
			// Exception from execute method
			Throwable t = re != null ? re : err;

			ResourceClosingException rce = null;

			// close
			rce = close(ioList, rce);
			rce = close(rsList, rce);
			rce = close(stmtList, rce);

			for (Connection con : cons) {
				if (t != null) {
					// Connection should be rolled back if exception has occurred.
					try {
						con.rollback();
					} catch (Throwable e) {
						rce = coalsceRce(rce, new ResourceClosingException(e));
					}
				}
				try {
					con.close();
				} catch (Throwable e) {
					rce = coalsceRce(rce, new ResourceClosingException(e));
				}
			}

			if (rce != null) {
				if (t == null) {
					// execute did not fail and close process fail case
					throw rce;
				} else {
					// both fail case
					coalsce(t, rce);
				}
			}

			// normal end or the exception thrown by execute throws if it is
			// available
		}
	}

	private ResourceClosingException close(List<Closeable> resources,
			ResourceClosingException rce) {
		for (Closeable res : resources) {
			try {
				res.close();
			} catch (Throwable e) {
				ResourceClosingException newRce;
				if (e instanceof ResourceClosingException)
					newRce = (ResourceClosingException) e;
				else
					newRce = new ResourceClosingException(e);
				rce = coalsceRce(rce, newRce);
			}
		}
		return rce;
	}

	private ResourceClosingException coalsceRce(ResourceClosingException src,
			ResourceClosingException dst) {
		if (src == null)
			return dst;
		if (dst == null)
			return src;
		coalsce(src, dst);
		return src;
	}

	private void coalsce(Throwable src, RuntimeException dst) {
		Throwable tmp = src;
		while (tmp.getCause() != null) {
			tmp = tmp.getCause();
		}
		tmp.initCause(dst);
	}

	protected abstract T execute() throws Exception;
}