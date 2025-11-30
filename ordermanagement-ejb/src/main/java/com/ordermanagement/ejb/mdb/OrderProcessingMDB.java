package com.ordermanagement.ejb.mdb;

import com.ordermanagement.entity.OrderEntity;
import jakarta.ejb.ActivationConfigProperty;
import jakarta.ejb.MessageDriven;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageListener;
import jakarta.jms.TextMessage;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.time.LocalDateTime;
import java.util.logging.Level;
import java.util.logging.Logger;

@MessageDriven(
        activationConfig = {
                @ActivationConfigProperty(
                        propertyName = "destinationLookup",
                        propertyValue = "java:/jms/queue/OrderQueue"
                ),
                @ActivationConfigProperty(
                        propertyName = "destinationType",
                        propertyValue = "jakarta.jms.Queue"
                )
        }
)
public class OrderProcessingMDB implements MessageListener {

    private static final Logger LOGGER = Logger.getLogger(OrderProcessingMDB.class.getName());

    @PersistenceContext(unitName = "OrderPU")
    private EntityManager entityManager;


    @Override
    public void onMessage(Message message) {
        try {
            if (!(message instanceof TextMessage textMessage)) {
                LOGGER.warning("Received non-TextMessage, ignoring: " + message);
                return;
            }

            String payload = textMessage.getText();
            LOGGER.info("OrderProcessingMDB received message payload: " + payload);

            Long orderId = Long.valueOf(payload);

            OrderEntity order = entityManager.find(OrderEntity.class, orderId);
            if (order == null) {
                LOGGER.warning("No order found with id " + orderId);
                return;
            }

            // Simulate some processing
            if (!"NEW".equalsIgnoreCase(order.getStatus())) {
                LOGGER.info("Skipping order id=" + orderId +
                        " because status is " + order.getStatus() + " (expected NEW)");
                return;
            }

            LOGGER.info("Processing order id=" + orderId +
                    " for customer=" + order.getCustomerName());

            // Simulate processing here if needed
             Thread.sleep(1000);

            order.setStatus("PROCESSED");
            order.setProcessedAt(LocalDateTime.now());

            LOGGER.info("Order id=" + orderId + " marked as PROCESSED.");

        } catch (NumberFormatException e) {
            LOGGER.log(Level.SEVERE, "Invalid order id in message payload", e);
            // Optionally mark transaction for rollback
            // mdbContext.setRollbackOnly();
        } catch (JMSException e) {
            LOGGER.log(Level.SEVERE, "Error reading JMS message", e);
            // mdbContext.setRollbackOnly();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error when processing JMS message", e);
            // mdbContext.setRollbackOnly();
        }
    }
}
