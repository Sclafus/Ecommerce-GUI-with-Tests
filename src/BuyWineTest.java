import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.util.ArrayList;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class BuyWineTest {

	private ControllerLogin loginobj;
	private ControllerHomepageUser homepageobj;
	private ControllerCart cartobj;

	@ParameterizedTest
	@CsvSource({ "user@user.com, pwd, 20, wine1, producer1, 2015, notes1, 1, grapes1",
			"employee@employee.com, pwd, 21, wine2, producer2, 2017, notes2, 1, grapes2",
			"admin@admin.com, pwd, 22, wine3, producer3, 2010, notes3, 1, grapes3", })
	/*
	 * "employee@employee.com, pwd", "admin@admin.com, pwd", "asd@asd.com, pwd",
	 * ", asd", "asd@asd.com," })
	 */
	public void procedure(String mail, String pass, int wine_id, String wine_name, String wine_producer, int wine_year,
			String wine_notes, int wine_quantity, String wine_grapes) {
		int permission = login(mail, pass);
		User user = new User("Placeholder", "Placeholder", mail, pass, permission);

		if (permission > 0) {
			Wine wine = new Wine(wine_id, wine_name, wine_producer, wine_year, wine_notes, wine_quantity, wine_grapes);
			int addResult = addWine(wine, user);

			if (addResult == 0) {
				// wine has been added successfully, let's continue
				System.out.format("Added %d to %s's cart, %d unit(s)\n", wine_id, mail, wine_quantity);
				int submitResult = submitOrder(user);
				if (submitResult == 0) {
					System.out.println("let's gooo");
				}

			} else {
				// addresult < 0 only if the wine is already in the cart
				fail("Wine " + wine_id + " is already present in the cart for user" + mail);
			}
		} else if (permission == 0) {
			// permission = 0 if the user does not exist.
			System.out.format("%s does not exist in the database\n", mail);
		} else {
			// permission < 0 only if mail or pass are null
			assertTrue((mail == null) || (pass == null));
		}
	}

	/**
	 * Test the login with an user, an employee and an admin. Also checks with a non
	 * registered user, and some with empty parameters. TODO finish this javadoc
	 * 
	 * @param mail mail of the user
	 * @param pass password of the user
	 * @return 1, 2 or 3 in case of success, based on the permission of the user,
	 *         else a number < 0
	 */
	@ParameterizedTest
	// @CsvSource({ "user@user.com, pwd", "employee@employee.com, pwd",
	// "admin@admin.com, pwd", "asd@asd.com, pwd",
	// ", asd", "asd@asd.com," })
	public int login(String mail, String pass) {
		loginobj = new ControllerLogin();
		try {
			int res = loginobj.login(mail, pass);
			if (res == -3) {
				fail("Server is unreachable");
			}
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

				case 0:
					assertAll(() -> assertNotNull(mail), () -> assertNotNull(pass));
					assertEquals(0, res);
					break;

				case -1:
					// email is not valid (not an email)
					assertEquals(false, loginobj.isMail(mail));
					break;

				case -2:
					if (pass == null) {
						assertNull(pass);
					} else if (mail == null) {
						assertNull(mail);
					} else {
						assertAll(() -> assertNull(mail), () -> assertNull(pass));
					}
					break;

				case -4:
					fail("returned object is not a User object");
					break;

				default:
					System.out.println(res);
					fail("Unknown output");
					break;
			}
			return res;
		} catch (IOException e) {
			fail("IOException");
			return -1;
		}
	}

	/**
	 * TODO this javadoc
	 * 
	 * @param wine
	 * @param quantity
	 * @param user
	 */
	@ParameterizedTest
	public int addWine(Wine wine, User user) {
		homepageobj = new ControllerHomepageUser();
		ArrayList<Wine> wines = homepageobj.initData(user);

		if (wines.isEmpty()) {
			fail("No wines available");
		}

		try {
			int result = homepageobj.addToCart(wine, wine.getQuantity());
			switch (result) {
				case 0:
					// everything is ok
					assertAll(() -> assertTrue(wine.getQuantity() > 0), () -> assertTrue(user.getPermission() > 0));
					break;

				case -1:
					// user does not have the sufficient permissions
					assertTrue(user.getPermission() < 1);
					break;

				case -2:
					// quantity error
					fail("Incorrect format for quantity");
					break;

				case -3:
					// quantity is negative
					assertTrue(wine.getQuantity() <= 0);
					break;

				case -4:
					// unexpected conversion error
					fail("Unexpected response from server");
					break;

				case -5:
					// selected wine does not exists
					assertTrue(!wines.contains(wine));
					break;

				default:
					fail("Unexpected result: " + result);
					break;
			}
			return result;
		} catch (IOException e) {
			e.printStackTrace();
			return -2;
		}
	}

	/**
	 * TODO this javadoc
	 * 
	 * @return
	 */
	@ParameterizedTest
	public int submitOrder(User user) {
		cartobj = new ControllerCart();
		ArrayList<Wine> cart = cartobj.initData(user);
		ArrayList<Wine> result = new ArrayList<Wine>();

		if (!cart.isEmpty()) {
			// cart not empty
			try {
				result = cartobj.buy();

				if (result.size() == 0) {
					// Order submitted
					assertTrue(user.getPermission() > 0);
				} else if (result.get(0).equals(new Wine())) {
					// catched by ClassNotFound Exception
					fail("Unexpected response from server");
				} else if(result.get(0).getProductId() == 0){
					// insufficient permissions
					assertTrue(user.getPermission() < 1);
				} else {
					fail("idk bro");
				}

				// return result;
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			// cart empty
			fail("Cart is empty for " + user.getEmail());
		}
		return 0;
	}
}