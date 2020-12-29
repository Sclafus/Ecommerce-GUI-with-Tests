import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.util.ArrayList;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

/**
 * Test class. This class contains the sufficient methods to login, add a wine
 * to the cart and submit an order.
 */
public class BuyWineTest {

	private ControllerLogin loginObj;
	private ControllerHomepageUser homepageObj;
	private ControllerCart cartObj;

	/**
	 * Sistematic procedure to login with the specified credentials, adding a wine
	 * with the specified paramters to the cart and submitting the order.
	 * 
	 * @param mail         Mail of the {@code User}. [String]
	 * @param pass         Pass of the {@code User}. [String]
	 * @param wineId       Id of the {@code Wine}. [int]
	 * @param wineName     Name of the {@code Wine}. [String]
	 * @param wineProducer Producer of the {@code Wine}. [String]
	 * @param wineYear     Year of the {@code Wine}. [int]
	 * @param wineNotes    Notes of the {@code Wine}. [String]
	 * @param wineQuantity Quantity of the {@code Wine}. [int]
	 * @param wineGrapes   Grapes of the {@code Wine}. [String]
	 */
	@ParameterizedTest
	@CsvFileSource(resources = "./testSet.csv", numLinesToSkip = 1)
	public void procedure(String mail, String pass, int wineId, String wineName, String wineProducer, int wineYear,
			String wineNotes, int wineQuantity, String wineGrapes) {

		System.out.format("\n---Initializing test for user %s, wine %d, quantity %d---\n", mail, wineId, wineQuantity);
		int permission = login(mail, pass);
		User user = new User("Placeholder", "Placeholder", mail, pass, permission);

		if (permission > 0) {
			Wine wine = new Wine(wineId, wineName, wineProducer, wineYear, wineNotes, wineQuantity, wineGrapes);
			int addResult = addWine(wine, user);

			switch (addResult) {
				case 0:
					// wine has been added successfully, let's continue
					int submitResult = submitOrder(user);

					if (submitResult == 0) {
						// Order placed successfully
						System.out.format("Order with wine %d, %d unit(s) has been placed for user %s\n", wineId,
								wineQuantity, mail);
					} else {
						System.out.format("Order with wine %d, %d unit(s) has NOT been placed for user %s\n", wineId,
								wineQuantity, mail);
					}
					break;

				case -3:
					// specified quantity is negative
					assertTrue(wineQuantity <= 0);
					System.out.format("Inserted quantity is negative: %d\n", wineQuantity);
					break;

				case -5:
					// wine does not exist
					System.out.format("Wine %d doesn't exist or the specified quantity is not in stock.\n", wineId);
					break;

				default:
					fail("Result in addWine not handled.");
					break;
			}
		} else if (permission == 0) {
			// permission = 0 if the user does not exist.
			System.out.format("%s is not present in the database\n", mail);
		} else {
			// permission < 0 only if mail or pass are null
			assertTrue((mail == null) || (pass == null));
			System.out.println("mail or password are nulls");
		}
	}

	/**
	 * Test method for login.
	 * 
	 * @param mail Mail of the {@code User}. [String]
	 * @param pass Password of the {@code User}. [String]
	 * @return 1, 2 or 3 in case of success, based on the permission of the
	 *         {@code User}, else a number <= 0 that indicates a non valid input.
	 */
	@ParameterizedTest
	public int login(String mail, String pass) {
		loginObj = new ControllerLogin();

		try {
			int res = loginObj.login(mail, pass);

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
					// invalid credentials
					assertAll(() -> assertNotNull(mail), () -> assertNotNull(pass));
					assertEquals(0, res);
					break;

				case -1:
					// email is not valid (not an email)
					assertEquals(false, loginObj.isMail(mail));
					break;

				case -2:
					// either password or mail are null
					if (pass == null) {
						assertNull(pass);
					} else if (mail == null) {
						assertNull(mail);
					} else {
						assertAll(() -> assertNull(mail), () -> assertNull(pass));
					}
					break;

				case -4:
					// unexpected response from server
					fail("Unexpected response from server");
					break;

				default:
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
	 * Test method to add a Wine to the cart.
	 * 
	 * @param wine The {@code Wine} we want to add to the cart. [Wine]
	 * @param user The {@code User} that wants to add the {@code Wine}. [User]
	 * @return
	 *         <ul>
	 *         <li>0 if the {@code Wine} has been added successfully to the
	 *         cart</li>
	 *         <li>-1 if the {@code User} permission is insufficient.</li>
	 *         <li>-2 if the quantity can't be converted to String (it should never
	 *         occur) or there's an unexpected error.</li>
	 *         <li>-3 if the quantity is negative.</li>
	 *         <li>-4 if the server responds in an unexpected way.</li>
	 *         <li>-5 if the wine can't be added to the cart.</li>
	 *         </ul>
	 * @see User
	 * @see Wine
	 */
	@ParameterizedTest
	public int addWine(Wine wine, User user) {
		homepageObj = new ControllerHomepageUser();
		ArrayList<Wine> wines = homepageObj.initData(user);

		if (wines.isEmpty()) {
			fail("No wines available");
		}

		try {
			int result = homepageObj.addToCart(wine, wine.getQuantity());

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
	 * Test method to submit an Order.
	 * 
	 * @param user The {@code User} that wants to submit a new {@code Order}. [User]
	 * @return 0 if the order has been submitted successfully, else -1.
	 * @see Order
	 * @see User
	 */
	@ParameterizedTest
	public int submitOrder(User user) {
		cartObj = new ControllerCart();
		ArrayList<Wine> cart = cartObj.initData(user);
		ArrayList<Wine> result = new ArrayList<Wine>();

		if (!cart.isEmpty()) {
			// cart not empty
			try {
				result = cartObj.buy();

				if (result.size() > 0) {
					// Order submitted
					if (result.get(0).equals(new Wine())) {
						// catched by ClassNotFound Exception
						fail("Unexpected response from server");
					} else if (result.get(0).getProductId() == 0) {
						// insufficient permissions
						assertTrue(user.getPermission() < 1);
					} else {
						// order submitted successfully
						return 0;
					}
					assertTrue(user.getPermission() > 0);
				} else {
					Wine wine = cart.get(0);
					System.out.format("Wine %d quantity is too high (%d)", wine.getProductId(), wine.getQuantity());
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			// cart empty
			fail("Cart is empty for " + user.getEmail());
		}
		return -1;
	}
}