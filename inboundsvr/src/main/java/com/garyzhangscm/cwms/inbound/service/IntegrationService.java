package com.garyzhangscm.cwms.inbound.service;

import com.garyzhangscm.cwms.inbound.clients.KafkaSender;
import com.garyzhangscm.cwms.inbound.model.CustomerReturnOrder;
import com.garyzhangscm.cwms.inbound.model.CustomerReturnOrderConfirmation;
import com.garyzhangscm.cwms.inbound.model.Receipt;
import com.garyzhangscm.cwms.inbound.model.ReceiptConfirmation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
public class IntegrationService {

    private static final Logger logger = LoggerFactory.getLogger(IntegrationService.class);

    @Autowired
    private ReceiptService receiptService;
    @Autowired
    private KafkaSender kafkaSender;
    // Add/ change item
    public void process(Receipt receipt) {





        // Setup the receipt line so it can be serialized along with the receipt
        receipt.getReceiptLines().forEach(receiptLine -> {
            receiptLine.setReceipt(receipt);
        });
        receiptService.processIntegration(receipt);
        logger.debug(">> receipt information saved!");
    }

    public void sendReceiptCompleteData(Receipt receipt) {


        ReceiptConfirmation receiptConfirmation = new ReceiptConfirmation(receipt);

        logger.debug("Will send receipt confirmation\n {}", receiptConfirmation);
        kafkaSender.send(receiptConfirmation);

    }


    public void sendCustomerReturnOrderCompleteData(CustomerReturnOrder customerReturnOrder) {


        CustomerReturnOrderConfirmation customerReturnOrderConfirmation
                = new CustomerReturnOrderConfirmation(customerReturnOrder);

        logger.debug("Will send customer return order confirmation\n {}", customerReturnOrderConfirmation);
        kafkaSender.send(customerReturnOrderConfirmation);

    }



}
