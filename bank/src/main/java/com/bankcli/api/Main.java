package com.bankcli.api;
import com.bankcli.persistence.*;
import com.bankcli.service.BankService;
import com.bankcli.service.BankServiceimpl;

public class Main {
    public static void main(String[] args) {

        ConnectionFactory factory = ConnectionFactory.getConnectionFactory();
        System.out.println("Connection successful: " + factory.testConnection());

        AccountDAO aDao = new AccountDAOimpl();
        TransactionDAO tDao = new TransactionDAOimpl();

        BankService service = new BankServiceimpl(aDao, tDao);
        
        new BankCLI(service).run();

        
        
    }
    
}
