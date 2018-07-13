package test.examples.SimpleTest;

import static org.junit.Assert.*;

import controller.exploration.JUnit4MCRRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(JUnit4MCRRunner.class)

public class SimpleMainTest {

	@Test
	public void test() {
		try {
			Main.main(null);
		} catch (Exception e) {
			System.out.println("error detected");
			fail();
		}
	}

}
