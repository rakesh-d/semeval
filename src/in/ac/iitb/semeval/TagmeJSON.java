package in.ac.iitb.semeval;

import java.io.Serializable;

public class TagmeJSON {
	
	
	
	public static class Annotation implements Serializable {
		/**
		 * 
		 */
		private static final long serialVersionUID = -7036092208557523233L;
		
		
		
		private int _id;
		private String _title;
		private int _start;
		private int _end;
		private String _rho;
		private String _spot;
		
		
		
		public int getId() { return _id;}
		public String getTitle() { return _title;}
		public int getStart() { return _start; }
		public int getEnd() { return _end; }
		public String getRho() { return _rho;}
		public String getSpot() { return _spot; }
		
		public void setId(int id) { _id = id;}
		public void setTitle(String t) { _title = t;}
		public void setStart(int s) { _start = s; }
		public void setEnd(int e) { _end = e; }
		public void setRho(String r) { _rho = r;}
		public void setSpot(String s) { _spot = s; }
	}
	
	
	private String _timestamp;
	private double _time;
	private String _api;
	private String _lang;
	private Annotation[] _annotations;
	
	
	
	public String getTimestamp() { return _timestamp; }
	public double getTime() { return _time;}
	public String getApi() { return _api;}
	public Annotation[] getAnnotations() { return _annotations;}
	public String getLang() { return _lang;}
	

	public void setTimestamp(String t){ _timestamp = t; }
	public void setTime(double t) { _time = t;}
	public void setApi(String a) { _api = a;}
	public void setAnnotations(Annotation[] a) { _annotations = a;}
	public void setLang(String lan) { _lang = lan;}
	

}
