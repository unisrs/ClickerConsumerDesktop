

/**
 * The Class ConsumerDriver.
 * 
 * Property of University of Northern Iowa
 * 
 */
public class ConsumerDriver {
	
	/**
	 * 
	 * Simple Driver to get things started.
	 *
	 * @param args the arguments
	 */
	public static void main(String[] args){
//		ClickerIMP consumer = new ClickerIMP();
		
		IMPCommunicationHub communicationHub = IMPCommunicationHub.getInstance();
		IMPVisual visual = IMPVisual.getInstance();
		
		communicationHub.setVisual(visual);
	}
}
