import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class BuyWineTest {

	private ControllerLogin loginobj;

	@ParameterizedTest
	@CsvSource({ "user@user.com, pwd", "employee@employee.com, pwd", "admin@admin.com, pwd", "asd@asd.com, pwd",
			", asd", "asd@asd.com," })
	public void procedure(String mail, String pass){
		login(mail, pass);
	}

	/**
	 * Test the login with an user, an employee and an admin. Also checks with a non
	 * registered user, and some with empty parameters.
	 * TODO finish this javadoc
	 * @param mail mail of the user
	 * @param pass password of the user
	 */
	@ParameterizedTest
	public void login(String mail, String pass) {
		loginobj = new ControllerLogin();
		try {
			int res = loginobj.login(mail, pass);
			if(res == -3){
				fail("Server is unreachable");
			}
			System.out.format("\nmail: %s pass: %s\nres: %d\n",mail, pass, res);
			switch (res) {

				case 1:
					// user login
					assertAll(() -> assertNotNull(mail), () -> assertNotNull(pass));
					assertEquals(1, res);
					break;

				case 2:
					// employee login
					assertAll(() -> assertNotNull(mail), () -> assertNotNull(pass));
					assertEquals(2, res);
					break;

				case 3:
					// admin login
					assertAll(() -> assertNotNull(mail), () -> assertNotNull(pass));
					assertEquals(3, res);
					break;

				case -1:
					// email is not valid (not an email)
					assertEquals(false, loginobj.isMail(mail));
					break;

				case -2:
					if(pass == null){
						assertNull(pass);
					} else if(mail == null){
						assertNull(mail);
					} else {
						assertAll(() -> assertNull(mail), () -> assertNull(pass));
					}
					break;

				case -4:
					fail("returned object is not a User object");
					break;
			}
		} catch (IOException e) {
			fail("IOException");
		}
	}
}