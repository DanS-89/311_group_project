package org.compi.csc311group3.database;

import org.compi.csc311group3.Expense;
import org.compi.csc311group3.service.ExpensesWithTotal;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.sql.Timestamp;
import java.util.Map;

public class ExpenseDAO {
    private final DbConnection dbConnection = new DbConnection();


    public List<Expense> getAllExpenses() throws SQLException, ClassNotFoundException {
        String query = "SELECT * FROM expenses ORDER BY date_time DESC";
        List<Expense> expenses = new ArrayList<>();

        try(Connection connection = dbConnection.getConnection();
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                Expense expense = new Expense(
                        rs.getInt("id"),
                        rs.getTimestamp("date_time").toLocalDateTime(),
                        rs.getString("description"),
                        rs.getString("category"),
                        rs.getDouble("amount")
                );
                expenses.add(expense);
            }
        }
        return expenses;
    }

    public List<String> getUniqueCategories() throws SQLException, ClassNotFoundException {
        String query = "SELECT DISTINCT name FROM categories";
        List<String> categories = new ArrayList<>();
        try(Connection connection = dbConnection.getConnection();
            PreparedStatement stmt = connection.prepareStatement(query);
            ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                categories.add(rs.getString("name"));
            }
        }
        return categories;
    }

    public int addExpense(Expense expense) throws SQLException, ClassNotFoundException {
        String query = "INSERT INTO expenses (date_time, description, category, amount) VALUES (?, ?, ?, ?)";
        int generatedId = -1;

        try(Connection connection = dbConnection.getConnection();
        PreparedStatement stmt = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setTimestamp(1, Timestamp.valueOf(expense.getDate_time()));
            stmt.setString(2, expense.getDescription());
            stmt.setString(3, expense.getCategory());
            stmt.setDouble(4, expense.getAmount());
            stmt.executeUpdate();

            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                generatedId = rs.getInt(1);
            }
        }
        return generatedId ;
    }

    public void deleteExpense(int id) throws SQLException, ClassNotFoundException {
        String query = "DELETE FROM expenses WHERE id = ?";
        try(Connection connection = dbConnection.getConnection();
        PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        }
    }

    public void updateExpense(Expense expense) throws SQLException, ClassNotFoundException {
        String query = "UPDATE expenses SET date_time = ?, description = ?, category = ?, amount = ? WHERE id = ?";

        try(Connection connection = dbConnection.getConnection();
        PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setTimestamp(1, Timestamp.valueOf(expense.getDate_time()));
            stmt.setString(2, expense.getDescription());
            stmt.setString(3, expense.getCategory());
            stmt.setDouble(4, expense.getAmount());
            stmt.setInt(5, expense.getId());
            stmt.executeUpdate();
        }
    }

    public boolean categoryExists(String category) throws SQLException, ClassNotFoundException {
        String query = "SELECT COUNT(*) FROM categories WHERE name = ?";
        try (Connection connection = dbConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, category);
            try (ResultSet rs = stmt.executeQuery()) {
                if(rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    public void addCategory(String category) throws SQLException, ClassNotFoundException {
        String query = "INSERT INTO categories (name) VALUES (?)";
        try(Connection connection = dbConnection.getConnection();
        PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, category);
            stmt.executeUpdate();
        }
    }

    public double getExpenseForPeriod(LocalDate startDate, LocalDate endDate, String category) throws SQLException {
        String query = "SELECT SUM(amount) AS total FROM expenses WHERE category = ? AND date_time BETWEEN ? AND ?";

        double total = 0.0;

        try(Connection connection = dbConnection.getConnection();
            PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, category);
            stmt.setDate(2, Date.valueOf(startDate));
            stmt.setDate(3, Date.valueOf(endDate));
            ResultSet rs = stmt.executeQuery();
            if(rs.next()){
                total = rs.getDouble("total");
            }
            return total;
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }


    /*
    * This method loops through all expenses in the database.
    * It puts all expenses in a list and calculates the total amount of expenses and stores it in a variable
    *
    * @return an ExpenseWithTotal object which contains list of all expenses and a variable with expense total amount
    * */
    public ExpensesWithTotal getAllExpensesInAnObject() throws SQLException, ClassNotFoundException {
        String query = "SELECT * FROM expenses";
        List<Expense> expenses = new ArrayList<>();
        double totalExpenseAmount = 0; //resets expenses to zero before looping through and recalculating.
        Map<LocalDate, Double> dailyExpenses = new HashMap<>(); //to store daily total expenses

        try(Connection connection = dbConnection.getConnection();
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                Expense expense = new Expense(
                        rs.getInt("id"),
                        rs.getTimestamp("date_time").toLocalDateTime(),
                        rs.getString("description"),
                        rs.getString("category"),
                        rs.getDouble("amount")
                );
                expenses.add(expense);

                totalExpenseAmount += expense.getAmount(); //add the amount of this expense to the totalAmount

                LocalDate expenseDate = expense.getDate_time().toLocalDate(); //get the date of the expense, not including the time part

                dailyExpenses.put(expenseDate, dailyExpenses.getOrDefault(expenseDate, 0.0) + expense.getAmount()); //update the daily total in the map



            }
        }
        return new ExpensesWithTotal(expenses, totalExpenseAmount, dailyExpenses);
    }


}
