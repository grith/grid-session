package grith.gridsession;


public class ExampleGridClient extends GridClient {

	public static void main(String[] args) throws Exception {

		ExampleGridClient egc = new ExampleGridClient(args);

		egc.run();

	}

	/**
	 * This constructor only works if you don't have additional cli parameters.
	 * 
	 * @param args
	 * @throws Exception
	 */
	public ExampleGridClient(String[] args) throws Exception {
		super(args);
	}

	public void run() {
		System.out.println("My example grid client.");
		System.out.println(getSession().status());

		System.out.println(getCredential().isValid());
	}

}
