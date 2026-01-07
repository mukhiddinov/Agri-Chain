// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

/**
 * @title Escrow
 * @dev Escrow contract for AgriChain platform order payments
 */
contract Escrow {
    
    enum OrderState { Created, Delivered, Released, Refunded }
    
    struct Order {
        address buyer;
        address seller;
        uint256 amount;
        OrderState state;
        uint256 createdAt;
    }
    
    mapping(bytes32 => Order) public orders;
    
    event OrderCreated(bytes32 indexed orderId, address indexed buyer, address indexed seller, uint256 amount);
    event OrderDelivered(bytes32 indexed orderId);
    event FundsReleased(bytes32 indexed orderId, address indexed seller, uint256 amount);
    event FundsRefunded(bytes32 indexed orderId, address indexed buyer, uint256 amount);
    
    /**
     * @dev Create a new escrow order
     * @param orderId Unique order identifier
     * @param seller Address of the seller
     */
    function createOrder(bytes32 orderId, address seller) external payable {
        require(msg.value > 0, "Amount must be greater than 0");
        require(orders[orderId].buyer == address(0), "Order already exists");
        require(seller != address(0), "Invalid seller address");
        
        orders[orderId] = Order({
            buyer: msg.sender,
            seller: seller,
            amount: msg.value,
            state: OrderState.Created,
            createdAt: block.timestamp
        });
        
        emit OrderCreated(orderId, msg.sender, seller, msg.value);
    }
    
    /**
     * @dev Mark order as delivered (only buyer can call)
     * @param orderId Order identifier
     */
    function markDelivered(bytes32 orderId) external {
        Order storage order = orders[orderId];
        require(order.buyer == msg.sender, "Only buyer can mark as delivered");
        require(order.state == OrderState.Created, "Invalid order state");
        
        order.state = OrderState.Delivered;
        emit OrderDelivered(orderId);
    }
    
    /**
     * @dev Release funds to seller (only buyer can call after delivery)
     * @param orderId Order identifier
     */
    function release(bytes32 orderId) external {
        Order storage order = orders[orderId];
        require(order.buyer == msg.sender, "Only buyer can release funds");
        require(order.state == OrderState.Delivered, "Order not delivered");
        
        order.state = OrderState.Released;
        uint256 amount = order.amount;
        
        (bool success, ) = order.seller.call{value: amount}("");
        require(success, "Transfer failed");
        
        emit FundsReleased(orderId, order.seller, amount);
    }
    
    /**
     * @dev Refund funds to buyer (only seller can call if order issues)
     * @param orderId Order identifier
     */
    function refund(bytes32 orderId) external {
        Order storage order = orders[orderId];
        require(order.seller == msg.sender, "Only seller can refund");
        require(order.state == OrderState.Created, "Invalid order state");
        
        order.state = OrderState.Refunded;
        uint256 amount = order.amount;
        
        (bool success, ) = order.buyer.call{value: amount}("");
        require(success, "Refund failed");
        
        emit FundsRefunded(orderId, order.buyer, amount);
    }
    
    /**
     * @dev Get order details
     * @param orderId Order identifier
     */
    function getOrder(bytes32 orderId) external view returns (
        address buyer,
        address seller,
        uint256 amount,
        OrderState state,
        uint256 createdAt
    ) {
        Order memory order = orders[orderId];
        return (order.buyer, order.seller, order.amount, order.state, order.createdAt);
    }
}
