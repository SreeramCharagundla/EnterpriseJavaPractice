package com.ordermanagement.web.rest;

import com.ordermanagement.entity.OrderEntity;
import com.ordermanagement.ejb.service.OrderServiceBean;
import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;

@Path("/orders")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class OrderResource {

    @EJB
    private OrderServiceBean orderService;

    // DTO for create-order request
    public static class CreateOrderRequest {
        public String customerName;
        public String productName;
        public int quantity;
    }
    
    public static class UpdateOrderRequest{
    	public String customerName;
    	public String productName;
    	public Integer quantity;
    	public String status;
    }
    
//    ------------------------------
//    CREATE
//    ------------------------------

    @POST
    public Response createOrder(CreateOrderRequest req) {
        if (req == null ||
            req.customerName == null || req.customerName.isBlank() ||
            req.productName == null || req.productName.isBlank() ||
            req.quantity <= 0) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Invalid order request")
                    .build();
        }

        OrderEntity created = orderService.createOrder(
                req.customerName,
                req.productName,
                req.quantity
        );

        return Response.status(Response.Status.CREATED)
                .entity(created)
                .build();
    }
    
//  ------------------------------
//  READ
//  ------------------------------

    @GET
    public Response listOrders() {
        List<OrderEntity> order = orderService.listAllOrders();
        return Response.ok(order).build();
    }
    
    @GET
    @Path("/{id}")
    public Response getOrderById(@PathParam("id") long id) {
    	OrderEntity order = orderService.findOrderById(id);
    	if(order==null) {
    		return Response.status(Response.Status.NOT_FOUND)
    				.encoding("Order with ID: "+id+" not found").build();
    	}
    	return Response.ok(order).build();
    }
    
    @PUT
    @Path("/{id}")
    public Response updateOrder(@PathParam("id") long id, UpdateOrderRequest req) {
    	if(req==null) {
    		return Response.status(Response.Status.BAD_REQUEST)
    				.entity("Request body is required").build();	
    	}
    	
    	OrderEntity updated = orderService.updateOrder(
    			id,
    			req.customerName,
    			req.productName,
    			req.quantity,
    			req.status
    			);
    	
    	if(updated==null) {
    		return Response.status(Response.Status.NOT_FOUND)
                    .entity("Order with id " + id + " not found")
                    .build();
    	}
    	return Response.ok(updated).build();
    }
    
    @DELETE
    @Path("/{id}")
    public Response deleteOrder(@PathParam("id") long id) {
        boolean deleted = orderService.deleteOrder(id);
        if (!deleted) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("Order with id " + id + " not found")
                    .build();
        }

        return Response.noContent().build(); // 204
    }
}
