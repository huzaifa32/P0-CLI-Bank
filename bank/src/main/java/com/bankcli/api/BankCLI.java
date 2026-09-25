package com.bankcli.api;

import java.util.*;
import com.bankcli.domain.Account;
import com.bankcli.domain.Transaction;
import com.bankcli.service.BankService;

public class BankCLI {
    private final BankService service;
    private final Scanner scanner = new Scanner(System.in);
    private Account currentAccount;

    public BankCLI(BankService service) {
        this.service = service;
    }

    public void run() {
            

            System.out.println("-----------------------");
            System.out.println("Welcome to The CLI Bank");
            System.out.println("-----------------------");
            String loginRegister = readString("1. Login \n2. Register \n3. Exit \nChoose an option. ");
            switch (loginRegister) {
                case "1":

                    login();

                    if(currentAccount == null){
                        System.out.println("authentication failed. exiting. ");
                        return;
                    }

                    System.out.println("Authentication Successfull. Welcome Account #"+ currentAccount.getAccountId());
                    showMenu();

                    System.out.println("Goodbye. ");
                            
                    break;
                case "2":
                    Register();
                    System.out.println("returning to menu. ");
                    run();
                    break;
                case "3":
                    System.out.println("exiting");
                    break;

            
                default:
                    System.out.println("Invalid option. ");
                    run();
                    break;
                    
            }

    
    }
    

    public void login(){

        int id = readInt("Enter an ID. ");
        String pin = readString("Enter your PIN. ");

        if((service.authenticate(id, pin)) ){

            currentAccount = service.getAccount(id);

        }
        

    }

    public void Register(){

        
        String pin = readString("Enter your PIN. ");
        double balance = readDouble("Deposit initial balance. ");
        if (balance<0){
            System.out.println("Initial balance must be greater than 0. ");
            return;
        }

        currentAccount = service.openAccount(pin, balance);
        
        System.out.println("Your Account ID is: "+currentAccount.getAccountId());
        

    }

    private void showMenu() {
        boolean running = true;

        while (running) {
            System.out.println();
            System.out.println("1. Check balance");
            System.out.println("2. Deposit");
            System.out.println("3. Withdraw");
            System.out.println("4. Transfer");
            System.out.println("5. View History");
            System.out.println("6. Logout / Exit");
            
            int choice = readInt("Choose an option: ");

            switch (choice) {
                case 1 -> checkBalance();
                case 2 -> deposit();
                case 3 -> withdraw();
                case 4 -> transfer();
                case 5 -> showHistory();
                case 6 -> running = false;
                default -> System.out.println("Invalid option, try again.");
            }
        }
    }

    private void checkBalance() {
        Account refreshed = service.getAccount(currentAccount.getAccountId());
        System.out.println("Current balance: $" + refreshed.getBalance());
    }

    private void showHistory() {
    List<Transaction> transactions = service.getTransactionHistoryById(currentAccount.getAccountId());
    for (Transaction entry : transactions) {
        System.out.printf("[#%d] %s: $%.2f (%s)%n",
            entry.getTransactionId(), entry.getType(), entry.getAmount(), entry.getTimestamp());
    }
}

    private void deposit() {
        double amount = readDouble("Enter Amount: ");

        try {
            Account updated = service.deposit(currentAccount.getAccountId(), amount);
            currentAccount = updated;
            System.out.println("Deposit successful. New balance: $" + updated.getBalance());
        } catch (Exception e) {
            System.out.println("Deposit failed: " + e.getMessage());
        }
    }

    private void withdraw() {
        double amount = readDouble("Enter withdrawal amount: ");

        try {
            Account updated = service.withdraw(currentAccount.getAccountId(), amount);
            currentAccount = updated;
            System.out.println("Withdrawal successful. New balance: $" + updated.getBalance());
        } catch (Exception e) {
            System.out.println("Withdrawal failed: " + e.getMessage());
        }
    }

    private void transfer() {
        int toAccountId = readInt("Enter recipient account ID: ");

        double amount = readDouble("Enter transfer amount: ");

        try {
            service.transfer(currentAccount.getAccountId(), toAccountId, amount);
            currentAccount = service.getAccount(currentAccount.getAccountId());
            System.out.println("Transfer successful. New balance: $" + currentAccount.getBalance());
        } catch (Exception e) {
            System.out.println("Transfer failed: " + e.getMessage());
        }
    }

    private int readInt(String prompt) {
        System.out.print(prompt);
        return Integer.parseInt(scanner.nextLine().trim());
    }

    private String readString(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }

    private double readDouble(String prompt) {
        System.out.print(prompt);
        return Double.parseDouble(scanner.nextLine().trim());
    }

}
