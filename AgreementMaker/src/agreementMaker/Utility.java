package agreementMaker;

import java.text.NumberFormat;

import javax.swing.JOptionPane;

import agreementMaker.application.mappingEngine.AbstractMatcher;
import agreementMaker.userInterface.table.MyTableModel;

public class Utility {
	
	public static String[] getPercentStringList() {
		int min = 0;
		int max = 100;
		int spin = 1;
		String[] s = new String[(max/spin) +1];
		String current;
		for(int i =min, j =0; i <= max && j<s.length; i+=spin, j++) {
			current = i+"%";
			s[j] = current;
		}
		return s;
	}
	
	public static double getDoubleFromPercent(String s) {
		String s2 = s.substring(0,s.length()-1);//remove last char %
		double d = Double.parseDouble(s2);
		return d/100;
	}
	
	public static String getPercentFromDouble(double d) {
		double p;
		if(0 <= d && d<= 1)
			p = d * 100;
		else throw new RuntimeException("Developer Error, the value passed to getPercentFromDouble(dobule d) should be between 0 and 1");
		return p+"%";
	}
	
	public static String getNoFloatPercentFromDouble(double d) {
		int i = (int)(d*100);
		return i+"%";
	}
	
	public static String[] getNumRelList() {
		int min = 1;
		int max = 100;
		int spin = 1;
		String[] list  = new String[(max/spin)+1];
		String any = MyTableModel.ANY;
		for(int i =min, j =0; i <= max && j<list.length-1; i+=spin, j++) {
			list[j] = i+"";
		}
		list[list.length-1] = any;
		return list;
	}
	
	public static int getIntFromNumRelString(String n) {
		int i;
		try {
			i = Integer.parseInt(n);
		}
		catch(Exception e) {
			//the value is the string any
			i = AbstractMatcher.ANY_INT;
		}
		return i;
	}
	
	public static String getStringFromNumRelInt(int n) {
		String s;
		if(n ==AbstractMatcher.ANY_INT)
			s = MyTableModel.ANY;
		else s = n+"";
		return s;
	}
	
	public static String getYesNo(boolean b) {
		if(b)
			return "yes";
		else return "no";
	}
	
	/**
	 * This function displays the JOptionPane with title and descritpion
	 *
	 * @param desc 		thedescription you want to display on option pane
	 * @param title 	the tile you want to display on option pane
	 */
	public  static void displayMessagePane(String desc, String title) {
		if(title == null)
			title = "Message Dialog";
		JOptionPane.showMessageDialog(null, desc, title, JOptionPane.PLAIN_MESSAGE);
	}
	
	public static void dysplayErrorPane(String desc, String title) {
		if(title == null)
			title = "Error";
		JOptionPane.showMessageDialog(null, desc,title, JOptionPane.ERROR_MESSAGE);
	}
	
	
	public static boolean displayConfirmPane(String desc, String title) {
		if(title == null)
			title = "Confirmation required";
		int res =  JOptionPane.showConfirmDialog(null,
			    desc,
			    title,
			    JOptionPane.YES_NO_OPTION);	
		if(res == JOptionPane.YES_OPTION)
			return true;
		else return false;
		}
}
