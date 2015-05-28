package com.tekinged;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class DictWriter {
	private File path;
	private String filename;
	private File dir;
	private OutputStream dictOut;
	private File res;

	public DictWriter(String filename) throws IOException {
		dir = new File(filename);
		String parent = dir.getParent();
		if (parent == null)
			path = new File(".");
		else {
			path = new File(parent);
			filename = dir.getName();
		}
		if (!(dir.exists() && dir.isDirectory()))
			if (!dir.mkdirs())
				throw new IOException("create directory failed '"
						+ dir.getAbsolutePath() + "'");
		File dictFile = new File(dir, filename + ".dict");
		dictOut = new FileOutputStream(dictFile);
		files.add(new ArchiveEntry(dictFile.getName(), dictFile));
		this.filename = filename;
	}

	public File getResDir() throws IOException {
		if (res == null) {
			res = new File(dir, "res");
			if (!(res.exists() && res.isDirectory()))
				if (!res.mkdirs())
					throw new IOException("create directory failed '"
							+ res.getAbsolutePath() + "'");
		}
		return res;
	}

	public void setPosMap(Map<String, String> posMap) throws IOException {
		this.posMap = posMap;
	}

	private Map<String, String> posMap;

	private static class Idx {
		public String pal;
		public int start;
		public int length;
		public List<Syn> syns = new ArrayList<Syn>();
	}

	private static class Syn {
		public String pal;
		public int idx;
	}

	private List<Idx> idxList = new ArrayList<Idx>();

	private int offset = 0;

	public void addEntry(DictEntry e) throws IOException {
		ByteArrayOutputStream baOut = new ByteArrayOutputStream();
		PrintStream out = new PrintStream(baOut);
		out.println("<script>function show_tip(t,s){t.innerHTML=s;}</script>");
		writeEntry(out, e);
		Idx idx = new Idx();
		for (int i = 0; i < e.subs.size(); i++) {
			DictEntry x = e.subs.get(i);
			if (x.pos == null || x.pos.indexOf("var.") < 0)
				writeEntry(out, x);
			Syn syn = new Syn();
			syn.pal = x.pal;
			idx.syns.add(syn);
		}
		out.println("");
		out.flush();
		byte[] buf = baOut.toByteArray();
		dictOut.write(buf);
		idx.pal = e.pal;
		idx.start = offset;
		idx.length = buf.length;
		idxList.add(idx);
		offset += buf.length;
	}

	private void writeEntry(PrintStream out, DictEntry e) throws IOException {
		out.println("<br><b>" + e.pal);
		for (String syn : e.syns)
			out.println(" /" + syn);
		out.println("</b><br>");
		if (e.haveImage) {
			out.println("<img src=\"" + e.id
					+ ".jpg\" style=\"width:100%\"/>");
			File imgFile = new File(getResDir(), "" + e.id + ".jpg");
			files.add(new ArchiveEntry("res/" + imgFile.getName(), imgFile));
		}
		if (e.haveAudio) {
			out.println("<audio src=\"" + e.id
					+ ".mp3\" controls=\"true\"></audio><br>");
			File mp3File = new File(getResDir(), "" + e.id + ".mp3");
			files.add(new ArchiveEntry("res/" + mp3File.getName(), mp3File));
		}
		if (e.pos != null) {
			out.println("<font color=\"green\"><i><u><div onclick=\"show_tip(this,'"
					+ posMap.get(e.pos)
					+ "');\">"
					+ e.pos
					+ "</div></u></i></font>");
			out.println("<br>");
		}
		if (e.eng != null) {
			out.println(e.eng.replaceAll("\n", "<br>"));
			out.println("<br>");
		}
		if (e.pdef != null) {
			out.println("<font color=\"blue\">"
					+ e.pdef.replaceAll("\n", "<br>") + "</font>");
			out.println("<br>");
		}
		if (e.xrefs.size() > 0) {
			out.println("<b>See also :</b>");
			for (String xref : e.xrefs) {
				out.println("<a href=\"bword://" + xref + "\">" + xref
						+ "</a> ");
			}
			out.println("<br>");
		}
		if (e.examples.size() > 0) {
			for (String ex : e.examples) {
				out.println("<font color=\"purple\">" + ex + "</font>");
				out.println("<br>");
			}
		}
	}

	public void wrapUp() throws IOException {
		dictOut.close();

		Collections.sort(idxList, new Comparator<Idx>() {
			public int compare(Idx a, Idx b) {
				return a.pal.compareToIgnoreCase(b.pal);
			}
		});
		File idxFile = new File(dir, filename + ".idx");
		DataOutputStream idxOut = new DataOutputStream(
				new FileOutputStream(idxFile));
		files.add(new ArchiveEntry(idxFile.getName(), idxFile));
		for (Idx idx : idxList) {
			byte[] buf = idx.pal.getBytes("UTF-8");
			idxOut.write(buf);
			idxOut.write(0);
			idxOut.writeInt(idx.start);
			idxOut.writeInt(idx.length);
		}
		int idxfilesize = idxOut.size();
		idxOut.close();

		List<Syn> syns = new ArrayList<Syn>();
		for (int i = 0; i < idxList.size(); i++) {
			for (Syn syn : idxList.get(i).syns) {
				syn.idx = i;
				syns.add(syn);
			}
		}
		if (syns.size() > 0) {
			Collections.sort(syns, new Comparator<Syn>() {
				public int compare(Syn a, Syn b) {
					return a.pal.compareToIgnoreCase(b.pal);
				}
			});
			File synFile = new File(dir, filename + ".syn");
			DataOutputStream synOut = new DataOutputStream(
					new FileOutputStream(synFile));
			files.add(new ArchiveEntry(synFile.getName(), synFile));
			for (Syn syn : syns) {
				byte[] buf = syn.pal.getBytes("UTF-8");
				synOut.write(buf);
				synOut.write(0);
				synOut.writeInt(syn.idx);
			}
			synOut.close();
		}

		File ifoFile = new File(dir, filename + ".ifo");
		PrintStream ifoOut = new PrintStream(new FileOutputStream(ifoFile));
		files.add(new ArchiveEntry(ifoFile.getName(), ifoFile));
		ifoOut.println("StarDict's dict ifo file");
		ifoOut.println("version=2.4.2");
		ifoOut.println("wordcount=" + idxList.size());
		if (syns.size() > 0)
			ifoOut.println("synwordcount=" + syns.size());
		ifoOut.println("idxfilesize=" + idxfilesize);
		ifoOut.println("bookname=" + filename);
		ifoOut.println("sametypesequence=h");
		ifoOut.close();

		archive(files, new File(path, filename + ".zip"));
	}

	private static class ArchiveEntry {
		public String entry;
		public File file;

		public ArchiveEntry(String entry, File file) {
			this.entry = entry;
			this.file = file;
		}
	}

	private List<ArchiveEntry> files = new ArrayList<ArchiveEntry>();

	private void archive(List<ArchiveEntry> fileList, File target)
			throws IOException {
		byte[] buf = new byte[1024];
		ZipOutputStream out = new ZipOutputStream(new FileOutputStream(
				target));
		for (int i = 0; i < fileList.size(); i++) {
			FileInputStream in = new FileInputStream(fileList.get(i).file);
			out.putNextEntry(new ZipEntry(fileList.get(i).entry));
			int len;
			while ((len = in.read(buf)) > 0) {
				out.write(buf, 0, len);
			}
			out.closeEntry();
			in.close();
		}
		out.close();
	}
}
