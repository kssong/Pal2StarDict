package com.tekinged;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

public class Pal2StarDict {

	private static Connection conn;
	private static DictWriter dict;

	private static boolean verbose = false;
	private static boolean addImage = true;
	private static boolean addAudio = true;
	private static boolean ignore_download = false;

	public static void main(String[] args) throws Exception {
		if (args.length < 4) {
			System.out
					.println("Usage : java -jar Pal2StarDict.jar mysql-jdbc-url user pass filename [options]");
			System.out
					.println("\teg) java -jar Pal2StarDict.jar jdbc:mysql://127.0.0.1/palauan root pass /home/kssong/palauan-20150525");
			System.out.println("\t-n : no resources (image, mp3)");
			System.out.println("\t-i : ignore resource download (image, mp3)");
			System.out.println("\t-v : verbose");
			System.exit(-1);
		}
		boolean no_resources = false;
		for (int i = 4; i < args.length; i++) {
			String a = args[i];
			if (a.equals("-n"))
				no_resources = true;
			if (a.equals("-i"))
				ignore_download = true;
			if (a.equals("-v"))
				verbose = true;
		}

		System.setProperty("file.encoding", "UTF-8");
		System.setProperty("line.separator", "\n");

		addImage = no_resources ? false : true;
		addAudio = no_resources ? false : true;

		Class.forName("com.mysql.jdbc.Driver");
		conn = DriverManager.getConnection(args[0], args[1], args[2]);
		dict = new DictWriter(args[3]);

		if (addAudio)
			dict.addResourceEntry("/play.png");
		{
			Map<String, String> posMap = new Hashtable<String, String>();
			Statement stmt = conn.createStatement();
			String sql = "select part,explanation from pos";
			ResultSet rs = stmt.executeQuery(sql);
			while (rs.next()) {
				posMap.put(rs.getString("part"), rs.getString("explanation"));
			}
			rs.close();
			stmt.close();
			dict.setPosMap(posMap);
		}
		{
			int total = selectCount("select count(*) from all_words3 where id=stem");
			int current = 0;
			Statement stmt = conn.createStatement();
			String sql = "select pal,pos,eng,pdef,id,origin,oword from all_words3 where id=stem";
			ResultSet rs = stmt.executeQuery(sql);
			while (rs.next()) {
				DictEntry e = new DictEntry();
				e.pal = rs.getString("pal");
				e.pos = rs.getString("pos");
				e.eng = rs.getString("eng");
				e.pdef = rs.getString("pdef");
				e.id = rs.getInt("id");
				e.origin = rs.getString("origin");
				e.oword = rs.getString("oword");
				if (verbose)
					System.out.println("processing entry " + (++current) + "/"
							+ total + ";" + "#" + e.id + "='" + e.pal + "'");
				if (addImage)
					addImage(dict, e);
				if (addAudio)
					addAudio(dict, e);
				addCrossRefs(e.id, e.xrefs);
				addExamples(e.id, e.examples);
				addProverbs(e.id, e.proverbs);
				addSynonyms(e.id, e.pal, e, e.syns);
				{
					Statement stmt2 = conn.createStatement();
					String sql2 = "select pal,pos,eng,pdef,id,origin,oword from all_words3 where stem="
							+ e.id + " and id<>stem";
					ResultSet rs2 = stmt2.executeQuery(sql2);
					while (rs2.next()) {
						DictEntry e2 = new DictEntry();
						e2.pal = rs2.getString("pal");
						e2.pos = rs2.getString("pos");
						e2.eng = rs2.getString("eng");
						e2.pdef = rs2.getString("pdef");
						e2.id = rs2.getInt("id");
						e2.origin = rs2.getString("origin");
						e2.oword = rs2.getString("oword");
						if (addImage)
							addImage(dict, e2);
						if (addAudio)
							addAudio(dict, e2);
						addCrossRefs(e2.id, e2.xrefs);
						addExamples(e2.id, e2.examples);
						addProverbs(e2.id, e2.proverbs);
						if (verbose) {
							if (e2.examples.size() > 0) {
								System.out.println("Example in sub entry : "
										+ e2.id);
							}
							if (e2.proverbs.size() > 0) {
								System.out.println("Proverb in sub entry : "
										+ e2.id);
							}
						}
						addSynonyms(e.id, e2.pal, e2, e2.syns);
						if (e2.pos == null || e2.pos.indexOf("var.") < 0)
							e.subs.add(e2);
					}
					rs2.close();
					stmt2.close();
				}
				dict.addEntry(e);
			}
			rs.close();
			stmt.close();
		}
		dict.wrapUp();
	}

	private static int selectCount(String sql) throws Exception {
		int result = 0;
		Statement stmt = conn.createStatement();
		ResultSet rs = stmt.executeQuery(sql);
		if (rs.next()) {
			result = rs.getInt(1);
		}
		rs.close();
		stmt.close();
		return result;
	}

	private static int selectSingleInteger(String sql) throws Exception {
		int result = -1;
		Statement stmt = conn.createStatement();
		ResultSet rs = stmt.executeQuery(sql);
		if (rs.next()) {
			result = rs.getInt(1);
		}
		rs.close();
		stmt.close();
		return result;
	}

	private static void saveUrl(final String filename, final String urlString)
			throws MalformedURLException, IOException {
		if (ignore_download) {
			File f = new File(filename);
			if (f.exists())
				return;
			else
				throw new FileNotFoundException("File not found : " + filename);
		}
		if (verbose)
			System.out.println("saving '" + filename + "'...");
		BufferedInputStream in = null;
		FileOutputStream fout = null;
		try {
			in = new BufferedInputStream(new URL(urlString).openStream());
			fout = new FileOutputStream(filename);
			final byte data[] = new byte[1024];
			int count;
			while ((count = in.read(data, 0, 1024)) != -1) {
				fout.write(data, 0, count);
			}
		} finally {
			if (in != null) {
				in.close();
			}
			if (fout != null) {
				fout.close();
			}
		}
		if (verbose)
			System.out.println("file '" + filename + "' saved...");
	}

	private static void addImage(DictWriter dict, DictEntry e)
			throws MalformedURLException, IOException, Exception {
		if (selectCount("select count(*) from pictures where allwid=" + e.id
				+ " and uploaded=1") > 0) {
			File imgFile = new File(dict.getResDir(), "" + e.id + ".jpg");
			if (!imgFile.exists())
				saveUrl(imgFile.getAbsolutePath(),
						"http://tekinged.com/uploads/pics/" + e.id + ".jpg");
			e.haveImage = true;
		}
	}

	private static void addAudio(DictWriter dict, DictEntry e)
			throws MalformedURLException, IOException, Exception {
		if (selectCount("select count(*) from upload_audio where uploaded=1 and externalid="
				+ e.id
				+ " and externaltable='all_words3' and externalcolumn='pdef'") > 0) {
			File mp3File = new File(dict.getResDir(), "" + e.id + ".mp3");
			if (!mp3File.exists()) {
				try {
					saveUrl(mp3File.getAbsolutePath(),
							"http://tekinged.com/uploads/mp3s/all_words3.pdef/"
									+ e.id + ".mp3");
					e.haveAudio = true;
				} catch (Exception ex) {
					if (verbose)
						ex.printStackTrace();
				}
			} else
				e.haveAudio = true;
		}
	}

	private static void addCrossRefs(int id, List<String> xrefs)
			throws Exception {
		Statement stmt = conn.createStatement();
		String sql = "select a.pal, b.b a from all_words3 a, cf b where a.id=b.b and a="
				+ id
				+ " union all select a.pal, b.a a from all_words3 a, cf b where a.id=b.a and b="
				+ id;
		ResultSet rs = stmt.executeQuery(sql);
		while (rs.next()) {
			xrefs.add(rs.getString("pal"));
		}
		rs.close();
		stmt.close();
	}

	private static void addExamples(int id, List<DictEntry.Ex> examples)
			throws Exception {
		Statement stmt = conn.createStatement();
		String sql = "select palauan,english,id from examples where stem=" + id;
		ResultSet rs = stmt.executeQuery(sql);
		while (rs.next()) {
			DictEntry.Ex ex = new DictEntry.Ex();
			ex.palauan = rs.getString("palauan");
			ex.english = rs.getString("english");
			if (addAudio) {
				int extid = rs.getInt("id");
				if (selectSingleInteger("select id from upload_audio where uploaded=1 and externalid="
						+ extid
						+ " and externaltable like 'examples' and externalcolumn like 'palauan'") > 0) {
					File exDir = new File(dict.getResDir(), "ex");
					if ((exDir.exists() && exDir.isDirectory())
							|| exDir.mkdirs()) {
						File mp3File = new File(exDir, "" + extid + ".mp3");
						if (!mp3File.exists()) {
							try {
								saveUrl(mp3File.getAbsolutePath(),
										"http://tekinged.com/uploads/mp3s/examples.palauan/"
												+ extid + ".mp3");
								ex.audio = extid;
							} catch (Exception exc) {
								if (verbose)
									exc.printStackTrace();
							}
						} else
							ex.audio = extid;
					} else {
						if (verbose)
							System.out
									.println("examples dir creation faield : "
											+ exDir.getAbsolutePath());
					}
				}
			}
			examples.add(ex);
		}
		rs.close();
		stmt.close();
	}

	private static void addProverbs(int id, List<DictEntry.Prov> provs)
			throws Exception {
		Statement stmt = conn.createStatement();
		String sql = "select palauan,english,explanation,id from proverbs where stem="
				+ id;
		ResultSet rs = stmt.executeQuery(sql);
		while (rs.next()) {
			DictEntry.Prov prov = new DictEntry.Prov();
			prov.palauan = rs.getString("palauan");
			prov.english = rs.getString("english");
			prov.explanation = rs.getString("explanation");
			if (addAudio) {
				int extid = rs.getInt("id");
				if (selectSingleInteger("select id from upload_audio where uploaded=1 and externalid="
						+ extid
						+ " and externaltable like 'proverbs' and externalcolumn like 'palauan'") > 0) {
					File provDir = new File(dict.getResDir(), "prov");
					if ((provDir.exists() && provDir.isDirectory())
							|| provDir.mkdirs()) {
						File mp3File = new File(provDir, "" + extid + ".mp3");
						if (!mp3File.exists()) {
							try {
								saveUrl(mp3File.getAbsolutePath(),
										"http://tekinged.com/uploads/mp3s/proverbs.palauan/"
												+ extid + ".mp3");
								prov.audio = extid;
							} catch (Exception exc) {
								if (verbose)
									exc.printStackTrace();
							}
						} else
							prov.audio = extid;
					} else {
						if (verbose)
							System.out
									.println("proverbs dir creation faield : "
											+ provDir.getAbsolutePath());
					}
				}
			}
			provs.add(prov);
		}
		rs.close();
		stmt.close();
	}

	private static void addSynonyms(int id, String pal, DictEntry de,
			List<String> syns) throws Exception {
		Statement stmt = conn.createStatement();
		String sql = "select pal,pos,eng,pdef,id from all_words3 where stem="
				+ id + " and id<>stem and pos like 'var.' and eng='" + pal
				+ "'";
		ResultSet rs = stmt.executeQuery(sql);
		while (rs.next()) {
			DictEntry e = new DictEntry();
			e.pal = rs.getString("pal");
			e.pos = rs.getString("pos");
			e.eng = rs.getString("eng");
			e.pdef = rs.getString("pdef");
			e.id = rs.getInt("id");
			syns.add(e.pal);
			if (e.pdef != null)
				de.pdef = de.pdef + " / " + e.pdef;
			addExamples(e.id, de.examples);
			addProverbs(e.id, de.proverbs);
		}
	}
}
