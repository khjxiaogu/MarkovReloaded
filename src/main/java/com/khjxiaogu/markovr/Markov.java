package com.khjxiaogu.markovr;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;


public class Markov{
	public static class ResultData {
		boolean fit;
		String reply;
		float repeat_rate;
		String state_next;

		public ResultData(boolean suit, float study, String data) {
			super();
			this.fit = suit;
			this.reply = data;
			this.repeat_rate = study;
		}

		public ResultData(boolean suit, String data) {
			super();
			this.fit = suit;
			this.reply = data;
			this.repeat_rate = 0;
		}

		public ResultData(float study, String data) {
			super();
			this.reply = data;
			this.fit = true;
			this.repeat_rate = study;
		}

		public String getSecond() {
			return reply;
		}

		public boolean getFirst() {
			return fit;
		}

		public boolean isSuit() {
			return fit;
		}

		public String getData() {
			return reply;
		}

		public float getStudy() {
			return repeat_rate;
		}
	}

	String createPoM = "CREATE TABLE IF NOT EXISTS link4 (" + "roll CHAR(3), " + "window CHAR(1)," + "count LONGINT,"
			+ " PRIMARY KEY(roll,window)" + ") WITHOUT ROWID;";
	Connection database;
	

	public Markov() {
		super();
		try {
			Class.forName("org.sqlite.JDBC");
		} catch (ClassNotFoundException e) {
			return;
		}
		try {
			database = DriverManager
					.getConnection("jdbc:sqlite:" + new File( "markov.db"));
			database.createStatement().execute(createPoM);
			database.createStatement().execute("PRAGMA synchronous = OFF");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private final int rank = 3;

	static class State {
		String state = "";
		Map<String, Long> cached = new HashMap<>();
	}

	private void train2(char c, State s) {
		s.state += c;
		if (s.state.length() < rank + 1)
			return;
		incRollCached(s);
		s.state = s.state.substring(1);
	}


	public String train(String text,String oldstate) {
		State s = new State();
		for (char c : text.replaceAll("(?:\\n|\\r|\\t|\"|“|”|「|」)", "").toCharArray())
			train2(c, s);
		commitCache(s);
		return s.state;
	}
	private synchronized void commitCache(State s) {
			
			try (PreparedStatement ps = database
					.prepareStatement("INSERT INTO link4 VALUES (?,?,?) ON CONFLICT DO UPDATE SET count=count+?;")) {
				
				s.cached.entrySet().removeIf(i -> {
					try {
						ps.setString(1, i.getKey().substring(0, rank));
						ps.setString(2, i.getKey().substring(i.getKey().length() - 1));
						ps.setLong(3, i.getValue());
						ps.setLong(4, i.getValue());
						ps.addBatch();
					} catch (SQLException e) {

					}
					return true;
				});
					database.createStatement().execute("BEGIN TRANSACTION");
				try {
				ps.executeBatch();
				}finally {
					database.createStatement().execute("END TRANSACTION");
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}

	}

	private void incRollCached(State s) {
		s.cached.compute(s.state, (k, n) -> n == null ? 1 : n + 1);
	}

	private Map<String, Long> getRoll(String roll) {
		try (PreparedStatement ps = database.prepareStatement("SELECT window,count FROM link4 WHERE roll = ?")) {
			ps.setString(1, roll);
			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {
					try {
						Map<String, Long> gen = new HashMap<>();
						do {
							gen.put(rs.getString(1), rs.getLong(2));
						} while (rs.next());
						return gen;
					} catch (Exception e) {
						e.printStackTrace();
						return null;
					}
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}
	private synchronized ResultData gen(String text) {
		String ans = text;
		String roll = text;
		int tit = 0;
		int pit = 0;
		while (true) {
			tit++;
			Map<String, Long> q = getRoll(roll);
			if (q == null)
				return new ResultData(false, pit * 1F / tit, ans);
			double tvalue = q.values().stream().reduce(0L, (i, j) -> i + j);
			if (q.size() < 3)
				pit++;
			long n = (long) Math.ceil(Math.random() * tvalue);
			String c = "";
			for (Entry<String, Long> i : q.entrySet()) {
				n -= i.getValue();
				if (n <= 0) {
					c = i.getKey();
					break;
				}
			}
			if ("。？！".contains(c)||(c.equals(" ")&&(ans.endsWith(".")||ans.endsWith("?")||ans.endsWith("!"))))
				return new ResultData(pit * 1F / tit, ans);
			ans += c;
			roll = (roll + c).substring(1);
			if (ans.length() > 20)
				return new ResultData(false, pit * 1F / tit, ans);
		}
	}
	public synchronized ResultData genLarge(String text,int len) {
		String ans = text;
		String roll = text;
		int tit = 0;
		int pit = 0;
		while (true) {
			tit++;
			Map<String, Long> q = getRoll(roll);
			if (q == null)
				return new ResultData(false, pit * 1F / tit, ans);
			double tvalue = q.values().stream().reduce(0L, (i, j) -> i + j);
			if (q.size() < 3)
				pit++;
			long n = (long) Math.ceil(Math.random() * tvalue);
			String c = "";
			for (Entry<String, Long> i : q.entrySet()) {
				n -= i.getValue();
				if (n <= 0) {
					c = i.getKey();
					break;
				}
			}
			if(ans.length()>len) {
				if ("。？！".contains(c)||(c.equals(" ")&&(ans.endsWith(".")||ans.endsWith("?")||ans.endsWith("!"))))
					return new ResultData(pit*1.0F/tit, ans);
				else if(ans.length()>len+500)
					return new ResultData(false, pit * 1F / tit, ans);
			}
			ans += c;
			roll = (roll + c).substring(1);
			
		}
	}

	public ResultData ret(String input,String state) {
		String text = input + "。";
		String ns=train(text,state);
		if (text.length() < 3)
			return null;
		ResultData g = gen(text.substring((int) Math.floor(Math.random() * (text.length() - rank))).substring(0, rank));
		g.state_next=ns;
		return g;
	}
	public ResultData retl(String input,int len) {
		String text = input + "。";

		if (text.length() < 3)
			return null;
		ResultData g = genLarge(text.substring((int) Math.floor(Math.random() * (text.length() - rank))).substring(0, rank),len);
		return g;
	}

	public String fret(String input) {
		if (input.length() < 3)
			return "too short!";
		ResultData g = gen(
				input.substring((int) Math.floor(Math.random() * (input.length() - rank))).substring(0, rank));
		return (g.getFirst() ? "fin|" : "err|") + g.getSecond();
	}

}
