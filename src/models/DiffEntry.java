package models;

import db.Resources;
import db.TechnicalDb;
public class DiffEntry
{
	private String file_id;
	private String new_commit_id;
	private String old_commit_id;
	private String diff_text;
	private int char_start;
	private int char_end;
	private diff_types diff_type;
	
	private int		line_start;
	private int		line_end;
	
	public enum diff_types{
		DIFF_DELETE,
		DIFF_ADD,
		DIFF_MODIFYINSERT,
		DIFF_MODIFYDELETE,
		DIFF_EQUAL,
		DIFF_UNKNOWN
	}
	
	public DiffEntry() {
		super();
		
		line_start = -1;
		line_end = -1;
	}
	public DiffEntry(String file_id, String new_commit_id, String old_commit_id, String diff_text, int chart_start, int char_end, String diff_type) {
		super();
		this.file_id = file_id;
		this.new_commit_id = new_commit_id;
		this.old_commit_id = old_commit_id;
		this.diff_text = diff_text;
		this.char_start = chart_start;
		this.char_end = char_end;
		this.diff_type = convertDiffType(diff_type);
		
		this.line_end = -1;
		this.line_start = -1;
	}
	public String getFile_id() {
		return file_id;
	}
	public void setFile_id(String file_id) {
		this.file_id = file_id;
	}
	public String getNewCommit_id() {
		return new_commit_id;
	}
	public void setNewCommit_id(String commit_id) {
		this.new_commit_id = commit_id;
	}
	public String getOldCommit_id() {
		return old_commit_id;
	}
	public void setOldCommit_id(String commit_id) {
		this.old_commit_id = commit_id;
	}
	public String getDiff_text() {
		return diff_text;
	}
	public void setDiff_text(String diff_text) {
		this.diff_text = diff_text;
	}
	public int getChar_start() {
		return char_start;
	}
	public void setChar_start(int char_start) {
		this.char_start = char_start;
	}
	public int getChar_end() {
		return char_end;
	}
	public void setChar_end(int char_end) {
		this.char_end = char_end;
	}
	public diff_types getDiff_type() {
		return diff_type;
	}
	public void setDiff_type(diff_types diff_type) {
		this.diff_type = diff_type;
	}	
	public void setDiff_type(String diff_type)
	{
		this.diff_type = convertDiffType(diff_type);
	}
	public diff_types convertDiffType(String type)
	{
		if(type.equals("DIFF_ADD"))
		return diff_types.DIFF_ADD;
		else if(type.equals("DIFF_DELETE"))
		return diff_types.DIFF_DELETE;
		else if(type.equals("DIFF_EQUAL"))
		return diff_types.DIFF_EQUAL;
		else if(type.equals("DIFF_MODIFYDELETE"))
		return diff_types.DIFF_MODIFYDELETE;
		else if(type.equals("DIFF_MODIFYINSERT"))
		return diff_types.DIFF_MODIFYINSERT;
		
		return diff_types.DIFF_UNKNOWN;
	}
	
	public int getLine_start(TechnicalDb tDB) {
		if(line_start != -1)
			return line_start;
		else 
			setLines(tDB);
		return line_start;
	}
	
	public int getLine_end(TechnicalDb tDB) {
		if(line_end != -1)
			return line_end;
		else
			setLines(tDB);
		return line_end;
	}
	
	private void setLines(TechnicalDb tDB) {
		String rawFile = tDB.getRawFileFromDiffTree(file_id, new_commit_id, 
				tDB.getCommitPathToRoot(new_commit_id));
		
		this.line_start = Resources.convertCharToLine(char_start, rawFile);
		this.line_end	= Resources.convertCharToLine(char_end, rawFile);
	}
	
}
