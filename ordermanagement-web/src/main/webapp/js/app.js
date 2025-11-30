const path = window.location.pathname;
// e.g. "/ordermanagement-web/index.html" or "/ordermanagement-web/"

const firstSegment = path.split("/").filter(Boolean)[0] || "";
const contextPath = firstSegment ? `/${firstSegment}` : "";
const apiBase = window.location.origin + contextPath + "/api";

//result sits in: "http://localhost:8080/ordermanagement-web/api"

let editingOrderId = null; // null = create mode, number = edit mode

function setFormModeCreate() {
    editingOrderId = null;
    document.getElementById("orderId").value = "";
    document.getElementById("formTitle").textContent = "Place New Order";
    document.getElementById("submitBtn").textContent = "Place Order";
    document.getElementById("cancelEditBtn").style.display = "none";
    document.getElementById("status").value = "";
    clearRowHighlight();
}

function setFormModeEdit(order) {
    editingOrderId = order.id;
    document.getElementById("orderId").value = order.id;
    document.getElementById("customerName").value = order.customerName || "";
    document.getElementById("productName").value = order.productName || "";
    document.getElementById("quantity").value = order.quantity || 1;
    document.getElementById("status").value = order.status || "";

    document.getElementById("formTitle").textContent = "Edit Order #" + order.id;
    document.getElementById("submitBtn").textContent = "Update Order";
    document.getElementById("cancelEditBtn").style.display = "inline-block";

    highlightRow(order.id);
}

async function fetchOrders() {
    try {
        const response = await fetch(apiBase + "/orders");
        if (!response.ok) {
            throw new Error("Failed to fetch orders");
        }
        const orders = await response.json();
        renderOrders(orders);
    } catch (err) {
        console.error(err);
        document.getElementById("message").textContent = "Error fetching orders.";
    }
}

function formatDateTime(value) {
    if (!value) return "";
    try {
        // WildFly / Jackson may send OffsetDateTime strings, just show raw or do basic formatting
        const date = new Date(value);
        if (isNaN(date.getTime())) {
            return value; // fall back to raw
        }
        return date.toLocaleString();
    } catch {
        return value;
    }
}

function clearRowHighlight() {
    document
        .querySelectorAll("#ordersTable tbody tr")
        .forEach(tr => tr.classList.remove("editing-row"));
}

function highlightRow(orderId) {
    clearRowHighlight();
    const row = document.querySelector(`#ordersTable tbody tr[data-order-id="${orderId}"]`);
    if (row) {
        row.classList.add("editing-row");
    }
}

function renderOrders(orders) {
    const tbody = document.querySelector("#ordersTable tbody");
    tbody.innerHTML = "";

    orders.forEach(order => {
        const tr = document.createElement("tr");
        tr.setAttribute("data-order-id", order.id);

        const tdId = document.createElement("td");
        tdId.textContent = order.id;

        const tdCustomer = document.createElement("td");
        tdCustomer.textContent = order.customerName;

        const tdProduct = document.createElement("td");
        tdProduct.textContent = order.productName;

        const tdQuantity = document.createElement("td");
        tdQuantity.textContent = order.quantity;

        const tdStatus = document.createElement("td");
        const spanStatus = document.createElement("span");
        const status = order.status || "";
        spanStatus.textContent = status;
        if (status) {
            spanStatus.classList.add("status-badge", "status-" + status);
        } else {
            spanStatus.classList.add("status-badge");
        }
        tdStatus.appendChild(spanStatus);

        const tdCreated = document.createElement("td");
        tdCreated.textContent = formatDateTime(order.createdAt);

        const tdProcessed = document.createElement("td");
        tdProcessed.textContent = formatDateTime(order.processedAt);

        const tdActions = document.createElement("td");
        tdActions.classList.add("actions");

        const editBtn = document.createElement("button");
        editBtn.textContent = "Edit";
        editBtn.type = "button";
        editBtn.addEventListener("click", () => {
            setFormModeEdit(order);
        });

        const deleteBtn = document.createElement("button");
        deleteBtn.textContent = "Delete";
        deleteBtn.type = "button";
        deleteBtn.addEventListener("click", async () => {
            const confirmed = window.confirm(`Delete order #${order.id}?`);
            if (!confirmed) return;

            try {
                const resp = await fetch(apiBase + "/orders/" + order.id, {
                    method: "DELETE"
                });
                if (!resp.ok) {
                    const text = await resp.text();
                    throw new Error(text || "Failed to delete order");
                }
                document.getElementById("message").textContent =
                    `Order #${order.id} deleted successfully.`;
                await fetchOrders();
                setFormModeCreate();
            } catch (err) {
                console.error(err);
                document.getElementById("message").textContent =
                    "Error deleting order: " + err.message;
            }
        });

        tdActions.appendChild(editBtn);
        tdActions.appendChild(deleteBtn);

        tr.appendChild(tdId);
        tr.appendChild(tdCustomer);
        tr.appendChild(tdProduct);
        tr.appendChild(tdQuantity);
        tr.appendChild(tdStatus);
        tr.appendChild(tdCreated);
        tr.appendChild(tdProcessed);
        tr.appendChild(tdActions);

        tbody.appendChild(tr);
    });
}

async function handleSubmit(event) {
    event.preventDefault();
    const customerName = document.getElementById("customerName").value;
    const productName = document.getElementById("productName").value;
    const quantity = parseInt(document.getElementById("quantity").value, 10);
    const status = document.getElementById("status").value || null;

    const payload = {
        customerName,
        productName,
        quantity
    };

    // For update, allow optional status
    if (status) {
        payload.status = status;
    }

    try {
        let response;
        if (editingOrderId == null) {
            // CREATE (POST /orders)
            response = await fetch(apiBase + "/orders", {
                method: "POST",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify(payload)
            });
        } else {
            // UPDATE (PUT /orders/{id})
            response = await fetch(apiBase + "/orders/" + editingOrderId, {
                method: "PUT",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify(payload)
            });
        }

        if (!response.ok) {
            const text = await response.text();
            throw new Error(text || "Request failed");
        }

        const msg = editingOrderId == null
            ? "Order created successfully!"
            : `Order #${editingOrderId} updated successfully!`;

        document.getElementById("message").textContent = msg;
        document.getElementById("orderForm").reset();
        setFormModeCreate();
        await fetchOrders();
    } catch (err) {
        console.error(err);
        document.getElementById("message").textContent =
            "Error: " + (err.message || "Request failed");
    }
}

document.addEventListener("DOMContentLoaded", () => {
    document.getElementById("orderForm").addEventListener("submit", handleSubmit);
    document.getElementById("refreshBtn").addEventListener("click", fetchOrders);
    document.getElementById("cancelEditBtn").addEventListener("click", () => {
        document.getElementById("orderForm").reset();
        setFormModeCreate();
    });

    setFormModeCreate();
    fetchOrders();
});