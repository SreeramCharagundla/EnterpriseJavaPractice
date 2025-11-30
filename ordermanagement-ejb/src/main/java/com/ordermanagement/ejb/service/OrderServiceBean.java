package com.ordermanagement.ejb.service;

import com.ordermanagement.entity.OrderEntity;
import jakarta.annotation.Resource;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.jms.JMSConnectionFactory;
import jakarta.jms.JMSContext;
import jakarta.jms.Queue;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.time.OffsetDateTime;
import java.util.List;

@Stateless
public class OrderServiceBean {

	@PersistenceContext(unitName = "OrderPU")
	private EntityManager entityManager;

	@Inject
	@JMSConnectionFactory("java:/JmsXA")
	private JMSContext jmsContext;

	@Resource(lookup = "java:/jms/queue/OrderQueue")
	private Queue orderQueue;

	/**
	 * Creates a new order, persists it to the database, and sends a JMS message
	 * with the order ID to the OrderQueue.
	 */
	public OrderEntity createOrder(String customerName, String productName, int quantity) {
		if (customerName == null || customerName.isBlank()) {
			throw new IllegalArgumentException("Customer name must not be empty");
		}
		if (productName == null || productName.isBlank()) {
			throw new IllegalArgumentException("Product name must not be empty");
		}
		if (quantity <= 0) {
			throw new IllegalArgumentException("Quantity must be greater than zero");
		}

		OrderEntity order = new OrderEntity();
		order.setCustomerName(customerName.trim());
		order.setProductName(productName.trim());
		order.setQuantity(quantity);
		order.setStatus("NEW");
		order.setCreatedAt(OffsetDateTime.now()); // or use LocalDateTime if your entity uses that

		entityManager.persist(order);
		entityManager.flush(); // ensures ID is generated before we send the message

		// Send the order ID as the message payload
//		OrderEntity saved = entityManager.merge(order);
		
		jmsContext.createProducer().send(orderQueue, String.valueOf(order.getId()));

		return order;
	}

	/**
	 * Returns all orders from the database ordered by creation time descending.
	 */
	public List<OrderEntity> listAllOrders() {
		return entityManager.createQuery("SELECT o FROM OrderEntity o ORDER BY o.createdAt DESC", OrderEntity.class)
				.getResultList();
	}

	public OrderEntity findOrderById(long id) {
		return entityManager.find(OrderEntity.class, id);
	}

	/**
	 * Updates an existing order. Parameters that are null are treated as "do not
	 * change".
	 *
	 * @param id           id of the order to update
	 * @param customerName new customer name or null
	 * @param productName  new product name or null
	 * @param quantity     new quantity or null
	 * @param status       new status or null
	 * @return updated OrderEntity
	 */

	public OrderEntity updateOrder(long id, String customerName, String productName, Integer quantity, String status) {

		OrderEntity order = entityManager.find(OrderEntity.class, id);
		if (order == null) {
			throw new IllegalArgumentException("Order with id " + id + " not found");
		}

		// Update basic fields (same as you already had)
		if (customerName != null && !customerName.isBlank()) {
			order.setCustomerName(customerName.trim());
		}

		if (productName != null && !productName.isBlank()) {
			order.setProductName(productName.trim());
		}

		if (quantity != null) {
			if (quantity <= 0) {
				throw new IllegalArgumentException("Quantity must be greater than zero");
			}
			order.setQuantity(quantity);
		}

		boolean shouldRequeue = false;

		if (status != null && !status.isBlank()) {
			String normalizedStatus = status.trim().toUpperCase();

			// Restrict to known statuses
			if (!normalizedStatus.equals("NEW") && !normalizedStatus.equals("PROCESSED")
					&& !normalizedStatus.equals("CANCELLED") && !normalizedStatus.equals("ERROR_JMS")) {
				throw new IllegalArgumentException("Unsupported status: " + status);
			}

			order.setStatus(normalizedStatus);

			// If caller sets status to NEW, we treat that as "please reprocess"
			if ("NEW".equals(normalizedStatus)) {
				shouldRequeue = true;
				order.setProcessedAt(null); // reset processed time for new processing cycle
			}
		}

		// At this point, 'order' is a managed entity.
		// On transaction commit, JPA will flush changes.

		if (shouldRequeue) {
			// Send a JMS message so the MDB can process this updated order
			jmsContext.createProducer().send(orderQueue, String.valueOf(order.getId()));
		}

		return order;
	}

	/**
	 * Deletes an order by id.
	 *
	 * @param id id of the order to delete
	 * @return true if the order existed and was removed, false otherwise
	 */
	public boolean deleteOrder(long id) {
		OrderEntity order = entityManager.find(OrderEntity.class, id);
		if (order == null) {
			return false;
		}

		entityManager.remove(order);
		return true;
	}

}
