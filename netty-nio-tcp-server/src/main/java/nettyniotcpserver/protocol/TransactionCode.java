package nettyniotcpserver.protocol;

/**
 * Created by yjlee on 2018-09-10.
 */
public enum TransactionCode {

    USER(1000),DEPOSIT(2000),WITHDRAW(3000);

    int code;
    TransactionCode(int code){
        this.code = code;
    }
}
