package com.tekinged;

import java.util.ArrayList;
import java.util.List;

public class DictEntry {
	public String pal;
	public String pos;
	public String origin;
	public String oword;
	public String eng;
	public String pdef;
	public int id;
	public List<DictEntry> subs = new ArrayList<DictEntry>();
	public boolean haveImage = false;
	public boolean haveAudio = false;
	public List<String> syns = new ArrayList<String>();
	public List<String> xrefs = new ArrayList<String>();
	public List<Ex> examples = new ArrayList<Ex>();
	public List<Prov> proverbs = new ArrayList<Prov>();

	public static class Ex {
		public String palauan;
		public String english;
		public int audio = -1;
	}

	public static class Prov {
		public String palauan;
		public String english;
		public String explanation;
		public int audio = -1;
	}
}
