import java.util.Map;

// TODO: Auto-generated Javadoc
/**
 * The Interface ClickerConsumerInterface.
 */
public interface ClickerConsumerInterface {
	
	/**
	 * Sets the iD.
	 *
	 * @param id the new iD
	 */
	public void setID(String id);
	
	/**
	 * Declare consumptions.
	 *
	 * @return the string
	 */
	public String declareConsumptions();
	
	/**
	 * Sets the active status.
	 *
	 * @param status the new active status
	 */
	public void setActiveStatus(boolean status);
	
	/**
	 * Gets the active status.
	 *
	 * @return the active status
	 */
	public boolean getActiveStatus();
	
	/**
	 * Input data.
	 *
	 * @param input the input
	 */
	public void inputData(Map<String, Map<String, String>> input);//  Person name, index#, value
	
	/**
	 * Sets the question.
	 *
	 * @param question the new question
	 */
	public void setQuestion(String question);
}
