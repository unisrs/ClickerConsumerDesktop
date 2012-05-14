
// TODO: Auto-generated Javadoc
/**
 * The Class IMPModel.
 */
public class IMPModel {

	/** The _instance. */
	private static IMPModel _instance;
	
	/**
	 * Instantiates a new iMP model.
	 */
	private IMPModel(){
		
	}
	
    /**
     * Gets the single instance of IMPModel.
     *
     * @return single instance of IMPModel
     */
    public static synchronized IMPModel getInstance() {
        if (_instance == null) {
            _instance = new IMPModel();
        }
        return _instance;
    }
	
}
