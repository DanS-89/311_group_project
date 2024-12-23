package org.compi.csc311group3;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.fxml.FXML;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.text.Text;
import jdk.jfr.Category;
import org.compi.csc311group3.service.UserService;
import org.compi.csc311group3.view.controllers.LoginController;

import java.awt.event.MouseEvent;
import java.io.IOException;
import java.time.LocalDate;

import static org.compi.csc311group3.HelloApplication.ChangeScreen;
import org.compi.csc311group3.database.DbConnection;
import org.compi.csc311group3.database.ExpenseDAO;
import org.compi.csc311group3.service.UserService;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.List;

import static org.compi.csc311group3.view.controllers.SettingsController.currencyController; //import currencyController instance from settings

/**
 * A controller class for managing the UI
 * Handles functions such as adding, editing, deleting, and creating new categories
 */
public class ExpenseController {


    /**
     * Expense UI declarations
     * */
    public Button dashboardLink;
    public Button analyticsLink;
    public Button addExpenseLink;
    public Button addDepositLink;
    public Button settingsLink;
    @FXML
    private TableView<Expense> expenseTableView;
    @FXML
    private TableColumn<Expense, String> descriptionColumn;
    @FXML
    private TableColumn<Expense, String> categoryColumn;
    @FXML
    private TableColumn<Expense, LocalDateTime> dateTimeColumn;
    @FXML
    private TableColumn<Expense, Double> amountColumn;
    @FXML
    private TableColumn<Expense, Integer> idColumn;
    @FXML
    private TextField descriptionField;
    @FXML
    private DatePicker dateTimeField;
    @FXML
    private TextField amountField;
    @FXML
    private ComboBox<String> categoryComboBox;
    @FXML
    private Button addButton;
    @FXML
    private Button deleteButton;
    @FXML
    private Button editButton;
    @FXML
    private Button newCategoryButton;
    

    private UserService userService = UserService.getInstance();

    private final DbConnection dbConnection = new DbConnection();
    private final ExpenseDAO expenseDAO = new ExpenseDAO();

    /**
     * Initializes the expense controller, sets up the table view and buttons
     * Populates the category combo box and sets cell factories
     * @throws SQLException If a database access error occurs
     * @throws ClassNotFoundException If the database driver class is not found
     */
    public void initialize() throws SQLException, ClassNotFoundException {

        Connection conn = dbConnection.getConnection();
        dbConnection.initializeTables(conn);
        loadExpenses();

        List<String> categoryList = expenseDAO.getUniqueCategories();
        categoryComboBox.setItems(FXCollections.observableArrayList(categoryList));

        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        dateTimeColumn.setCellValueFactory(new PropertyValueFactory<>("date_time"));
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        categoryColumn.setCellValueFactory(new PropertyValueFactory<>("category"));
        amountColumn.setCellValueFactory(new PropertyValueFactory<>("amount"));

        addButton.setOnAction(e -> addExpense());
        editButton.setOnAction(e -> editExpense());
        deleteButton.setOnAction(e -> deleteExpense());
        newCategoryButton.setOnAction(e -> createNewCategory());
    }


    /**
     * Adds a new expense object to the database
     * Validates the input fields
     * Displays errors alerts if validation fails
     */
    public void addExpense() {
        LocalDateTime date_time = dateTimeField.getValue() != null ? dateTimeField.getValue().atStartOfDay() : null;
        String description = descriptionField.getText();
        String category = categoryComboBox.getValue();
        String amountText = amountField.getText();

        if(description.isEmpty() || category == null || amountText.isEmpty() || date_time == null){
        showErrorAlert("Please fill in all fields");
        return;
        }

        double amount;
        try{
            //amount = Double.parseDouble(amountText);
            double amountInUserCurrency = Double.parseDouble(amountText); //amount in user selected currency
            amount = currencyController.convertToUSD(amountInUserCurrency);//converted from user currency to USD before saved in database
        } catch(NumberFormatException e){
            showErrorAlert("Please enter a valid amount");
            return;
        }

        Expense expense = new Expense(date_time, description, category, amount);
        try{
            int generateId = expenseDAO.addExpense(expense);
            loadExpenses();
            clearFields();
        } catch (SQLException | ClassNotFoundException e){
            showErrorAlert("Error adding expense: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Edits an existing expense in the database.
     * Updates the selected expense with new values entered by the user
     * Validates inputs
     */
    public void editExpense(){
        Expense selectedExpense = expenseTableView.getSelectionModel().getSelectedItem();
        if(selectedExpense != null){
           String description = descriptionField.getText();
           String category = categoryComboBox.getValue();
           String amountText = amountField.getText();
           LocalDateTime date_time = dateTimeField.getValue().atStartOfDay();

           if(description.isEmpty() || category == null || amountText.isEmpty() || date_time == null ){
               showErrorAlert("Please fill in all fields before editing");
               return;
           }
           double amount;
           try{
               amount = Double.parseDouble(amountText);
           } catch (NumberFormatException e){
                   showErrorAlert("Please enter a valid amount");
                   return;
           }
           selectedExpense.setDate_time(date_time);
           selectedExpense.setDescription(description);
           selectedExpense.setCategory(category);
           selectedExpense.setAmount(amount);

           try {
               expenseDAO.updateExpense(selectedExpense);
               loadExpenses();
               clearFields();
           } catch (SQLException | ClassNotFoundException e){
               showErrorAlert("Error editing expense: " + e.getMessage());
               e.printStackTrace();
           }
        }
    }

    /**
     * Displays an error alert with the specified message
     * @param message The specified message to display in the alert
     */
    private void showErrorAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Deletes the selected expense from the database
     * Prompts the user to confirm
     * Displays error alerts if an expense isn't selected or an error occurs during deletion
     */
    private void deleteExpense(){
        Expense selectedExpense = expenseTableView.getSelectionModel().getSelectedItem();
        if(selectedExpense != null){
            try{
                expenseDAO.deleteExpense(selectedExpense.getId());
                loadExpenses();
            } catch (SQLException | ClassNotFoundException e){
                showErrorAlert("Error deleting expense: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * Creates a new expense category
     * Checks for duplicate categories
     */
    private void createNewCategory(){
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("New category");
        dialog.setHeaderText("Add a new category");
        dialog.setContentText("Enter the name of the new category");
        dialog.showAndWait().ifPresent(category -> {
            try{
            if(!expenseDAO.categoryExists(category)){
                expenseDAO.addCategory(category);
                List<String> categoryList = expenseDAO.getUniqueCategories();
                categoryComboBox.setItems(FXCollections.observableArrayList(categoryList));
            } else {
                showErrorAlert("Category already exists");
                }
            } catch (SQLException | ClassNotFoundException e) {
                    showErrorAlert("Error adding category: " + e.getMessage());
                    e.printStackTrace();
                }
        });
    }

    /**
     * Clears all the input fields
     */
    private void clearFields(){
        descriptionField.clear();
        categoryComboBox.getSelectionModel().clearSelection();
        amountField.clear();
        dateTimeField.setValue(null);
    }

    /**
     * Loads expenses from the database and populates the table view
     * Converts the amounts to the correct current currency using the currency controller
     * Displays an error if there is an issue getting the expenses from the database
     */
    private void loadExpenses(){
        try{
            List<Expense> expenseList = expenseDAO.getAllExpenses();

            //convert the amount values to the correct currency
            for (Expense expense : expenseList){
                double convertedAmount = currencyController.convertCurrency(expense.getAmount()); //converts amount to correct currency selected by user in settings
                expense.setAmount(convertedAmount);
            }

            expenseTableView.setItems(FXCollections.observableArrayList(expenseList));
        } catch (SQLException | ClassNotFoundException e){
            showErrorAlert("Error loading expenses: " + e.getMessage());
            e.printStackTrace();
        }
    }


    @FXML
    void dashboardLinkClicked(ActionEvent event) throws IOException {
        ChangeScreen("dashboard-view.fxml", 850, 560, dashboardLink);
    }
    @FXML
    void analyticsLinkClicked(ActionEvent event) {

        // TODO: Implement functionality to navigate to analytics page

    }
    @FXML
    void addExpenseLinkClicked(ActionEvent event) throws IOException {
        ChangeScreen("Expense.fxml", 850, 560, addExpenseLink);
    }
    @FXML
    void addDepositLinkClicked(ActionEvent event) throws IOException {

        ChangeScreen("deposit-view.fxml", 850, 560, addDepositLink);
    }
    @FXML
    void settingsLinkClicked(ActionEvent event) throws IOException {
        ChangeScreen("settings.fxml", 850, 560, settingsLink);
    }
}
