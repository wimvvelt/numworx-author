package nl.numworx.author;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.felix.framework.Logger;

public class FelixLogger extends Logger {
	
	static class Entry {
		int level;
		String msg;
		Throwable throwable;
		Entry(int level, String msg, Throwable throwable) {
			this.level = level;
			this.msg = msg;
			this.throwable = throwable;
		}
	}
	
	Queue<Entry> queue;
	boolean m_logger;

	public FelixLogger() {
		queue = new ConcurrentLinkedQueue<>();
	}

	void flush() {
		while (m_logger && !queue.isEmpty()) {
			Entry pop = queue.remove();
			this.doLog(pop.level, pop.msg, pop.throwable);
		}
	}

	@Override
	public void setLogger(Object logger) {
		m_logger = logger != null;
		super.setLogger(logger);
	}

	@Override
	protected void doLogOut(int level, String s, Throwable throwable) {
		queue.add(new Entry(level,s, throwable));
	}
	
}
