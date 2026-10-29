package qatest;
import javax.persistence.*;
@Entity @Table(name="qa_test_pick")
public class TransactionProbe {
    @Id public Long id;
    public Long quantity;
    public TransactionProbe(){}
    public TransactionProbe(long id,long quantity){this.id=id;this.quantity=quantity;}
}
