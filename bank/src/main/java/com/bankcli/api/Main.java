package com.bankcli.api;
import com.bankcli.persistence.*;
import com.bankcli.service.BankService;
import com.bankcli.service.BankServiceimpl;
import com.bankcli.persistence.ConnectionFactory;

public class Main {
    public static void main(String[] args) {

        ConnectionFactory factory = ConnectionFactory.getConnectionFactory();
        System.out.println("Connection successful: " + factory.testConnection());

        AccountDAO aDao = new AccountDAOimpl(factory);
        TransactionDAO tDao = new TransactionDAOimpl(factory);

        BankService service = new BankServiceimpl(aDao, tDao, factory);
        
        new BankCLI(service).run();

        
        
    }
    
}
