package grith.gridsession;

public class ExampleGridClient extends GridClient {

	public static void main(String[] args) {

		ExampleGridClient egc = new ExampleGridClient();
		execute(egc);

	}

	@Override
	public void run() {
		System.out.println("My example grid client.");
		System.out.println(getSession().status());
	}

}
