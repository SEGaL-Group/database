package db;

import java.io.IOException;
import java.io.Reader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import db.util.AExecutionItem;
import db.util.ISetter;
import db.util.ISetter.StringSetter;
import db.util.PreparedStatementExecutionItem;

public abstract class DbConnection {
	private Queue<AExecutionItem> executionQueue = new ConcurrentLinkedQueue<AExecutionItem>();
	private String dbName;
	private int queueSize = 4;
	private boolean stopWorkers = false;
	private List<QueueWorker> queueWorkers = new ArrayList<QueueWorker>();
	private int	queueLimit = 10000;
	private int maxBatchSize = 10000;
	
	protected DbConnection() 
	{
		try 
		{
			Class.forName("org.postgresql.Driver").newInstance();
		} 
		catch (InstantiationException e) 
		{
			e.printStackTrace();
		} 
		catch (IllegalAccessException e) 
		{
			e.printStackTrace();
		} 
		catch (ClassNotFoundException e) 
		{
			e.printStackTrace();
		}
	}

	public boolean connect(String dbName) {
		this.dbName = dbName;
		this.stopWorkers = false;
		this.startWorkers(queueSize);
		return true;
	}
	
	public boolean connect(String dbName, int numQueueWorkers) {
		this.queueSize = numQueueWorkers;
		return this.connect(dbName);
	}
	
	public boolean connect(String dbNAme, int numQueueWorkers, int queueLimit) {
		this.queueLimit = queueLimit;
		return this.connect(dbNAme, numQueueWorkers);
	}
	
	public boolean close() {
		try {
			this.stopWorkers();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return true;
	}
	
	public void runScript(Reader isr) throws IOException, SQLException {
		Connection conn = getConnection(this.dbName);
		ScriptRunner sr = new ScriptRunner(conn, false, true);
		sr.setLogWriter(null);
		sr.runScript(isr);
		conn.close();
	}
	
	public void setConnectionString(String dbName) {
		this.dbName = dbName;
	}
	
	private synchronized void waiting(long ms) {
		try
		{
			this.wait(ms);
		}
		catch (InterruptedException e)
		{
			e.printStackTrace();
		}
	}
	
	public boolean addExecutionItem(AExecutionItem ei) {
		while (this.queueLimit  < this.executionQueue.size()) this.waiting(1);
		return this.executionQueue.add(ei);
	}
	
	private Connection getConnection(String dbName) {
		Connection conn = null;
		try {
			conn = DriverManager.getConnection(Resources.dbUrl + dbName.toLowerCase(), Resources.dbUser, Resources.dbPassword);
		} 
		catch (Exception e) 
		{
			e.printStackTrace();
		}
		return conn;
	}
	
	public void startWorkers(int numberOfThreads) {
		for (int i = 0; i < numberOfThreads; ++i) {
			QueueWorker qw = new QueueWorker(getConnection(this.dbName));
			qw.start();
			this.queueWorkers.add(qw);
		}
	}
	
	private class QueueWorker extends Thread {
		private Connection conn = null;
		
		public QueueWorker(Connection connection) {
			this.conn = connection;
		}
		
		private synchronized void waiting(long ms) {
			try {
				this.wait(ms);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		public void run() {
			AExecutionItem itemToBeExecuted = null;
			//int currentBatchSize = 0;
			while (!executionQueue.isEmpty() || !stopWorkers) {
				//currentBatchSize = 0;
				/*if (itemToBeExecuted == null)*/ itemToBeExecuted = executionQueue.poll();
				if (itemToBeExecuted != null) {
					//AExecutionItem nextItem = executionQueue.poll();
					//boolean combined = itemToBeExecuted.combine(nextItem);
					//while (combined && currentBatchSize < maxBatchSize) {
					//	nextItem = executionQueue.poll();
					//	currentBatchSize++;
					//	combined = itemToBeExecuted.combine(nextItem);
					//}
					itemToBeExecuted.execute(this.conn);
					//itemToBeExecuted = nextItem;
					//if (combined) itemToBeExecuted = null;
				}
				//if (executionQueue.isEmpty() && itemToBeExecuted == null) this.waiting(1);
				if (executionQueue.isEmpty()) this.waiting(1);
			}
			try {
				conn.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}
		
	public void stopWorkers() throws InterruptedException {
		this.stopWorkers = true;
		for (QueueWorker qWorker : queueWorkers) {
			qWorker.join();
		}
	}
}
