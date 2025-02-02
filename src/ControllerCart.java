import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.AnchorPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

/**
 * Controller for Cart, page accessible by {@code User} with permission > 0 (aka
 * everyone)
 */
public class ControllerCart {

	private User currentUser;

	@FXML
	private AnchorPane rootPane;

	@FXML
	private TableView<Wine> tableView;

	@FXML
	private TableColumn<Wine, String> nameColumn;

	@FXML
	private TableColumn<Wine, Integer> yearColumn;

	@FXML
	private TableColumn<Wine, String> producerColumn;

	@FXML
	private TableColumn<Wine, Integer> quantityColumn;

	/**
	 * Initialize {@code this.currentUser} with the passed value. This method is
	 * made to be called from another controller, using the {@code load} method in
	 * {@code Loader} class. This method has been modified for testing purposes.
	 * 
	 * @param user the {@code User} we want to pass. [User]
	 * @see Loader
	 * @return the list of {@Wine} in the cart. [ArrayList of Wine]
	 */
	@SuppressWarnings("unchecked")
	public ArrayList<Wine> initData(User user) {
		this.currentUser = user;
		ArrayList<Wine> cartResult = new ArrayList<Wine>();

		try {
			Socket socket = new Socket("localhost", 4316);

			// client -> server
			OutputStream outputStream = socket.getOutputStream();
			ObjectOutputStream out = new ObjectOutputStream(outputStream);
			String[] toBeSent = { "display_cart", this.currentUser.getEmail() };
			out.writeObject(toBeSent);

			// server -> client
			InputStream inputStream = socket.getInputStream();
			ObjectInputStream in = new ObjectInputStream(inputStream);

			cartResult = (ArrayList<Wine>) in.readObject();
			socket.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return cartResult;
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
		this.nameColumn.setCellValueFactory(new PropertyValueFactory<Wine, String>("Name"));
		this.yearColumn.setCellValueFactory(new PropertyValueFactory<Wine, Integer>("Year"));
		this.producerColumn.setCellValueFactory(new PropertyValueFactory<Wine, String>("Producer"));
		this.quantityColumn.setCellValueFactory(new PropertyValueFactory<Wine, Integer>("Quantity"));
		ObservableList<Wine> oListWine = FXCollections.observableArrayList(wines);
		// load data
		tableView.setItems(oListWine);
	}

	/**
	 * Goes back to the employee homepage.
	 * 
	 * @param event GUI event. [ActionEvent]
	 * @throws IOException if the file can't be accessed.
	 */
	@FXML
	public void back(ActionEvent event) throws IOException {
		Loader loader = new Loader(this.currentUser, this.rootPane);
		loader.load("homepage_user");
	}

	/**
	 * Allows the {@code User} to buy the items in his cart. 
	 * This method has been modified for testing purposes.
	 * 
	 * @param event GUI event. [ActionEvent]
	 * @throws UnknownHostException if the IP address of the host could not be
	 *                              determined.
	 * @throws IOException          if an I/O error occurs when creating the socket.
	 * @return The {@Wine} that have been bought. [ArrayList of Wine]
	 */
	public ArrayList<Wine> buy() throws IOException, UnknownHostException {
		ArrayList<Wine> winesAfterOrder = new ArrayList<Wine>();

		if (this.currentUser.getPermission() > 0) {
			// user is authorized to perform the action
			Socket socket = new Socket("localhost", 4316);

			// client -> server
			OutputStream outputStream = socket.getOutputStream();
			ObjectOutputStream out = new ObjectOutputStream(outputStream);
			String[] toBeSent = { "new_order", this.currentUser.getEmail() };
			out.writeObject(toBeSent);

			// server -> client
			InputStream inputStream = socket.getInputStream();
			ObjectInputStream in = new ObjectInputStream(inputStream);

			try {
				Order newOrder = (Order) in.readObject();
				socket.close();
				winesAfterOrder = newOrder.getWines();
				return winesAfterOrder;
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
				socket.close();
				winesAfterOrder.add(new Wine());
				return winesAfterOrder;
			}
		} else {
			// user is not authorized to perform the action
			winesAfterOrder.add(new Wine(0, "", "", 0, "", 0, ""));
			return winesAfterOrder;
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
	public void displayCart(ActionEvent event) throws IOException {
		Socket socket = new Socket("localhost", 4316);

		// client -> server
		OutputStream outputStream = socket.getOutputStream();
		ObjectOutputStream out = new ObjectOutputStream(outputStream);
		String[] toBeSent = { "display_cart", this.currentUser.getEmail() };
		out.writeObject(toBeSent);

		// server -> client
		InputStream inputStream = socket.getInputStream();
		ObjectInputStream in = new ObjectInputStream(inputStream);

		try {
			ArrayList<Wine> cartResult = (ArrayList<Wine>) in.readObject();
			addToTable(cartResult);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		socket.close();
	}

	/**
	 * Allows the {@code User} to remove wines from his cart.
	 * 
	 * @param event GUI event. [ActionEvent]
	 * @throws UnknownHostException if the IP address of the host could not be
	 *                              determined.
	 * @throws IOException          if an I/O error occurs when creating the socket.
	 * @see User
	 */
	@FXML
	public void removeFromCart(ActionEvent event) throws UnknownHostException, IOException {
		Socket socket = new Socket("localhost", 4316);
		try {
			// getting selection of the tableview
			Wine wine = tableView.getSelectionModel().getSelectedItem();

			// client -> server
			OutputStream outputStream = socket.getOutputStream();
			ObjectOutputStream out = new ObjectOutputStream(outputStream);
			String[] toBeSent = { "remove_from_cart", this.currentUser.getEmail(),
					String.valueOf(wine.getProductId()) };
			out.writeObject(toBeSent);

			// server -> client
			InputStream inputStream = socket.getInputStream();
			ObjectInputStream in = new ObjectInputStream(inputStream);
			Boolean removeResult = (Boolean) in.readObject();

			if (removeResult) {
				// if result is true, the wine has been correctly removed from cart
				initData(this.currentUser);
				Alert alert = new Alert(AlertType.INFORMATION);
				alert.setTitle(String.format("Removed from cart"));
				alert.setHeaderText(String.format("Removed %s from cart.", wine.getName()));
				alert.showAndWait();
			} else {
				// else, the wine has not been correctly removed from cart
				Alert alert = new Alert(AlertType.WARNING);
				alert.setTitle(String.format("Select a wine"));
				alert.setHeaderText("You have to click on a Wine and then Remove.");
				alert.showAndWait();
			}
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		socket.close();
	}
}