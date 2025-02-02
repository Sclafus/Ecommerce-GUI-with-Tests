import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.AnchorPane;

/**
 * Controller for the User Homepage.
 */
public class ControllerHomepageUser {

	private User currentUser;

	@FXML
	private AnchorPane rootPane;

	@FXML
	private TextField searchboxName;

	@FXML
	private TextField searchboxYear;

	@FXML
	private TreeView<String> treeView;

	@FXML
	private TableView<Wine> tableView;

	@FXML
	private TableColumn<Wine, String> nameColumn;

	@FXML
	private TableColumn<Wine, Integer> yearColumn;

	@FXML
	private TableColumn<Wine, String> producerColumn;

	@FXML
	private TableColumn<Wine, String> grapesColumn;

	@FXML
	private TableColumn<Wine, String> notesColumn;

	@FXML
	private TextField quantity;

	/**
	 * Initialize {@code this.currentUser} with the passed value. This method is
	 * made to be called from another controller, using the {@code load} method in
	 * {@code Loader} class. It also adds all the wines to display to the TableView
	 * and handles the process to display the notifications. This method has been
	 * modified for testing purposes.
	 * 
	 * @param user the {@code User} we want to pass. [User]
	 * @see Loader
	 * @see Wine
	 * @return the list of all the {@Wine}. [ArrayList of Wine]
	 */
	@SuppressWarnings("unchecked")
	public ArrayList<Wine> initData(User user) {
		this.currentUser = user;
		ArrayList<Wine> wines = new ArrayList<Wine>();

		try {
			// Fill the frontpage with wines.
			Socket socket = new Socket("localhost", 4316);

			// client -> server
			OutputStream outputStream = socket.getOutputStream();
			ObjectOutputStream out = new ObjectOutputStream(outputStream);
			String[] toBeSent = { "get_wines" };
			out.writeObject(toBeSent);

			// server ->client
			InputStream inputStream = socket.getInputStream();
			ObjectInputStream in = new ObjectInputStream(inputStream);
			wines = (ArrayList<Wine>) in.readObject();
			socket.close();
			return wines;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return wines;
	}

	/**
	 * Loads the specified ArrayList of Wines in the table view. This method will
	 * override the previous content of the table.
	 * 
	 * @param wines the content that needs to be displayed on the table. [Arraylist
	 *              of Wine]
	 * @see Wine
	 */
	public void addToTable(ArrayList<Wine> wines) {
		// set up the columns in the table
		nameColumn.setCellValueFactory(new PropertyValueFactory<Wine, String>("Name"));
		yearColumn.setCellValueFactory(new PropertyValueFactory<Wine, Integer>("Year"));
		producerColumn.setCellValueFactory(new PropertyValueFactory<Wine, String>("Producer"));
		grapesColumn.setCellValueFactory(new PropertyValueFactory<Wine, String>("Grapewines"));
		notesColumn.setCellValueFactory(new PropertyValueFactory<Wine, String>("Notes"));
		ObservableList<Wine> oListWine = FXCollections.observableArrayList(wines);
		// load data
		tableView.setItems(oListWine);
	}

	/**
	 * Displays an alert when a wine is restocked, from an ArrayList of Wines. This
	 * method is automatically called whenever the homepage of the user is loaded.
	 * 
	 * @param wines the content that needs to be displayed on the alert. [Arraylist
	 *              of Wine]
	 * @see Wine
	 */
	public void displayNotifications(ArrayList<Wine> wines) {
		if (wines.size() > 0) {
			Alert alert = new Alert(AlertType.INFORMATION);
			alert.setTitle("Some wines have been restocked");
			alert.setHeaderText("These wines have been restocked:");
			StringBuilder winesSb = new StringBuilder();

			for (Wine wine : wines) {
				winesSb.append(String.format("%s (%d)\n", wine.getName(), wine.getYear()));
			}
			String winesString = winesSb.toString();
			alert.setContentText(winesString);
			alert.showAndWait();
		}
	}

	/**
	 * Allows the {@code User} to add the wines to his cart. This method has been
	 * modified for testing purposes.
	 * 
	 * @param wine The wine to add in the cart. [Wine]
	 * @param quantity The quantity for the specified wine. [int]
	 * @throws UnknownHostException if the IP address of the host could not be
	 *                              determined.
	 * @throws IOException          if an I/O error occurs when creating the socket.
	 * @return
	 *         <ul>
	 *         <li>0 if the {@code Wine} has been added successfully to the
	 *         cart</li>
	 *         <li>-1 if the {@code User} permission is insufficient.</li>
	 *         <li>-2 if the quantity can't be converted to String (it should never occur).</li>
	 *         <li>-3 if the quantity is negative.</li>
	 *         <li>-4 if the server responds in an unexpected way.</li>
	 *         <li>-5 if the wine can't be added to the cart.</li>
	 *         </ul>
	 * @see User
	 * 
	 */
	public int addToCart(Wine wine, int quantity) throws UnknownHostException, IOException {
		// permission check, guests can't add to cart
		if (this.currentUser.getPermission() > 0) {
			Socket socket = new Socket("localhost", 4316);

			try {
				// quantity check
				if (quantity > 0) {
					// client -> server
					OutputStream outputStream = socket.getOutputStream();
					ObjectOutputStream out = new ObjectOutputStream(outputStream);
					String[] toBeSent = { "add_to_cart", this.currentUser.getEmail(),
							String.valueOf(wine.getProductId()), String.valueOf(quantity) };
					out.writeObject(toBeSent);

					// server -> client
					InputStream inputStream = socket.getInputStream();
					ObjectInputStream in = new ObjectInputStream(inputStream);
					Boolean addResult = (Boolean) in.readObject();

					if (addResult) {
						// operation addToCart was successful
						return 0;
					} else {
						// operation addToCart was not successful
						return -5;
					}
				} else {
					// quantity is negative
					socket.close();
					return -3;
				}
			} catch (ClassNotFoundException e) {
				// unexpected response from server
				socket.close();
				return -4;
			} catch (NumberFormatException e) {
				// the quantity was non inserted correctly
				socket.close();
				return -2;
			}
		} else {
			// the User is a guest, so he needs to login first
			return -1;
		}
	}

	/**
	 * Allows anyone to search for wines.
	 * 
	 * @param event GUI event. [ActionEvent]
	 * @throws UnknownHostException if the IP address of the host could not be
	 *                              determined.
	 * @throws IOException          if an I/O error occurs when creating the socket.
	 */
	@FXML
	@SuppressWarnings("unchecked")
	public void search(ActionEvent event) throws IOException, ClassNotFoundException {
		Socket socket = new Socket("localhost", 4316);

		// client -> server
		OutputStream outputStream = socket.getOutputStream();
		ObjectOutputStream out = new ObjectOutputStream(outputStream);
		String[] toBeSent = { "search", searchboxName.getText(), searchboxYear.getText() };
		out.writeObject(toBeSent);

		// server -> client
		InputStream inputStream = socket.getInputStream();
		ObjectInputStream in = new ObjectInputStream(inputStream);
		ArrayList<Wine> searchResult = (ArrayList<Wine>) in.readObject();

		// displays the result in the tableview
		addToTable(searchResult);
		socket.close();
	}

	/**
	 * Goes to the cart page. It also checks the permission of the {@code User},
	 * onlu users with permission > 0 can access to the cart page (users, employees,
	 * administrators but not guests).
	 * 
	 * @param event GUI event. [ActionEvent]
	 * @throws IOException if the file can't be accessed.
	 */
	@FXML
	public void showCart(ActionEvent event) throws IOException {
		if (this.currentUser.getPermission() > 0) {
			Loader loader = new Loader(this.currentUser, this.rootPane);
			loader.load("cart");
		} else {
			Alert alert = new Alert(AlertType.INFORMATION);
			alert.setTitle("Please login");
			alert.setHeaderText("You need to login to perform this action.");
			alert.showAndWait();
		}
	}

	/**
	 * Displays all the orders made by the {@code User} in the TreeView.
	 * 
	 * @throws UnknownHostException if the IP address of the host could not be
	 *                              determined.
	 * @throws IOException          if an I/O error occurs when creating the socket.
	 * @see Order
	 * @see User
	 */
	@SuppressWarnings("unchecked")
	public void displayOrders() throws IOException {
		// user is authorized to perform the action
		Socket socket = new Socket("localhost", 4316);

		// client -> server
		OutputStream outputStream = socket.getOutputStream();
		ObjectOutputStream out = new ObjectOutputStream(outputStream);
		String[] toBeSent = { "get_orders_user", this.currentUser.getEmail() };
		out.writeObject(toBeSent);

		// server ->client
		InputStream inputStream = socket.getInputStream();
		ObjectInputStream in = new ObjectInputStream(inputStream);

		try {
			// receives the ArrayList of orders from the server
			ArrayList<Order> orders = (ArrayList<Order>) in.readObject();
			// creates the TreeView's root
			TreeItem<String> rootItem = new TreeItem<String>("Orders");

			for (Order order : orders) {
				// fills the TreeView with the orders
				TreeItem<String> rootOrder = new TreeItem<String>(Integer.toString(order.getId()));
				TreeItem<String> id = new TreeItem<String>("Order ID: " + order.getId());
				TreeItem<String> status = new TreeItem<String>("Status: " + order.getStatus());
				TreeItem<String> customer = new TreeItem<String>("Customer: " + order.getCustomer());
				rootOrder.getChildren().addAll(id, status, customer);

				// for each order it displays each wine of the order
				for (Wine wine : order.getWines()) {
					TreeItem<String> rootProduct = new TreeItem<String>(
							String.format("%d - %s %s", wine.getProductId(), wine.getName(), wine.getYear()));
					TreeItem<String> quantity = new TreeItem<String>("Quantity: " + wine.getQuantity());
					rootProduct.getChildren().add(quantity);
					rootOrder.getChildren().add(rootProduct);
				}
				rootItem.getChildren().add(rootOrder);
			}
			treeView.setRoot(rootItem);
			treeView.setShowRoot(false);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		socket.close();
	}

	/**
	 * Goes back to the login page.
	 * 
	 * @param event GUI event. [ActionEvent]
	 * @throws IOException if the filename cannot be read.
	 */
	@FXML
	public void logout(ActionEvent event) throws IOException {
		AnchorPane pane = FXMLLoader.load(getClass().getResource("./login.fxml"));
		rootPane.getChildren().setAll(pane);
	}
}