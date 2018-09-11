package nettyniotcpserver.protocol;

/**
 * Created by yjlee on 2018-09-10.
 */
public class Deposit extends AbstractProtocol {

    String guid;
    double depositAmount;

    public Deposit(TransactionCode trsactionCode, int bodyLength, String guid, double depositAmount) {
        super(trsactionCode, bodyLength);
        this.guid = guid;
        this.depositAmount = depositAmount;
    }

    @Override
    public TransactionCode getTrCode() {
        return null;
    }
}
