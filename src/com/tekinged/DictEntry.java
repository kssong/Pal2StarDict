package com.tekinged;

import java.util.ArrayList;
import java.util.List;

public class DictEntry {
	public String pal;
	public String pos;
	public String eng;
	public String pdef;
	public int id;
	public List<DictEntry> subs = new ArrayList<DictEntry>();
	public boolean haveImage = false;
	public boolean haveAudio = false;
	public List<String> xrefs = new ArrayList<String>();
	public List<String> examples = new ArrayList<String>();
	public List<String> syns = new ArrayList<String>();
}
