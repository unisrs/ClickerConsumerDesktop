import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneLayout;


// TODO: Auto-generated Javadoc
/**
 * The Class ConsumerSelectionBox.
 */
public class ConsumerSelectionBox extends JPanel {
	
	/** The COLO n_ separator. */
	private static String COLON_SEPARATOR      = "`/:";
	
	/** The parent. */
	IMPVisual parent;
	
	/** The check boxes. */
	HashMap<String,JCheckBox> checkBoxes;
	
	/** The enabled. */
	HashMap<String,Boolean> enabled;
	
	/** The my layout. */
	GridLayout myLayout;
	
	/**
	 * Instantiates a new consumer selection box.
	 *
	 * @param v the v
	 */
	public ConsumerSelectionBox(IMPVisual v){
		super();
		parent = v;
		myLayout = new GridLayout();
		checkBoxes = new HashMap<String,JCheckBox>();
		enabled = new HashMap<String,Boolean>();
		setLayout(myLayout);
		buildCheckBoxes(parent.getConsumers());
	}
	
	/**
	 * Builds the check boxes.
	 *
	 * @param consumers the consumers
	 */
	private void buildCheckBoxes(ArrayList<String> consumers) {
		myLayout.setColumns(1);
		myLayout.setRows(consumers.size()+1);
		
		refreshCheckBoxes(consumers);
		
		Iterator<String> i = checkBoxes.keySet().iterator();
		String current;
		JPanel p;
		p = new JPanel();
		JLabel l = new JLabel();
		l.setText("Enabled Consumers");
		p.add(l);
		this.add(p);
		while (i.hasNext()){
			p = new JPanel();
			p.setLayout(new GridLayout(1,2));
			current = i.next();
			p.add(new JLabel(current.split(COLON_SEPARATOR)[0]));	
			p.add(checkBoxes.get(current));
			this.add(p);
		}
		
		
		
		
	}
	
	/**
	 * Refresh check boxes.
	 *
	 * @param consumers the consumers
	 */
	private void refreshCheckBoxes(ArrayList<String> consumers) {
		Iterator<String> i = consumers.iterator();
		checkBoxes.clear();
		String s;
		while (i.hasNext()){
			s = i.next();
			checkBoxes.put(s, new JCheckBox());
			checkBoxes.get(s).doClick();
			enabled.put(s, true);
		}		
	}
	
	
	
	
	
}
