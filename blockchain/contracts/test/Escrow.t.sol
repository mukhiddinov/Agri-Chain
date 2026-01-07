// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

import "forge-std/Test.sol";
import "../src/Escrow.sol";

contract EscrowTest is Test {
    Escrow public escrow;
    address public buyer;
    address public seller;
    bytes32 public orderId;
    uint256 public constant ORDER_AMOUNT = 1 ether;

    function setUp() public {
        escrow = new Escrow();
        buyer = address(0x1);
        seller = address(0x2);
        orderId = keccak256("ORDER_001");
        
        // Fund buyer
        vm.deal(buyer, 10 ether);
    }

    function testCreateOrder() public {
        vm.prank(buyer);
        escrow.createOrder{value: ORDER_AMOUNT}(orderId, seller);
        
        (address orderBuyer, address orderSeller, uint256 amount, Escrow.OrderState state, ) = escrow.getOrder(orderId);
        
        assertEq(orderBuyer, buyer);
        assertEq(orderSeller, seller);
        assertEq(amount, ORDER_AMOUNT);
        assertTrue(state == Escrow.OrderState.Created);
    }

    function testMarkDelivered() public {
        vm.prank(buyer);
        escrow.createOrder{value: ORDER_AMOUNT}(orderId, seller);
        
        vm.prank(buyer);
        escrow.markDelivered(orderId);
        
        (, , , Escrow.OrderState state, ) = escrow.getOrder(orderId);
        assertTrue(state == Escrow.OrderState.Delivered);
    }

    function testRelease() public {
        vm.prank(buyer);
        escrow.createOrder{value: ORDER_AMOUNT}(orderId, seller);
        
        vm.prank(buyer);
        escrow.markDelivered(orderId);
        
        uint256 sellerBalanceBefore = seller.balance;
        
        vm.prank(buyer);
        escrow.release(orderId);
        
        uint256 sellerBalanceAfter = seller.balance;
        assertEq(sellerBalanceAfter - sellerBalanceBefore, ORDER_AMOUNT);
        
        (, , , Escrow.OrderState state, ) = escrow.getOrder(orderId);
        assertTrue(state == Escrow.OrderState.Released);
    }

    function testRefund() public {
        vm.prank(buyer);
        escrow.createOrder{value: ORDER_AMOUNT}(orderId, seller);
        
        uint256 buyerBalanceBefore = buyer.balance;
        
        vm.prank(seller);
        escrow.refund(orderId);
        
        uint256 buyerBalanceAfter = buyer.balance;
        assertEq(buyerBalanceAfter - buyerBalanceBefore, ORDER_AMOUNT);
        
        (, , , Escrow.OrderState state, ) = escrow.getOrder(orderId);
        assertTrue(state == Escrow.OrderState.Refunded);
    }

    function testCannotCreateOrderWithZeroAmount() public {
        vm.prank(buyer);
        vm.expectRevert("Amount must be greater than 0");
        escrow.createOrder{value: 0}(orderId, seller);
    }

    function testCannotCreateDuplicateOrder() public {
        vm.prank(buyer);
        escrow.createOrder{value: ORDER_AMOUNT}(orderId, seller);
        
        vm.prank(buyer);
        vm.expectRevert("Order already exists");
        escrow.createOrder{value: ORDER_AMOUNT}(orderId, seller);
    }

    function testOnlyBuyerCanMarkDelivered() public {
        vm.prank(buyer);
        escrow.createOrder{value: ORDER_AMOUNT}(orderId, seller);
        
        vm.prank(seller);
        vm.expectRevert("Only buyer can mark as delivered");
        escrow.markDelivered(orderId);
    }

    function testOnlyBuyerCanRelease() public {
        vm.prank(buyer);
        escrow.createOrder{value: ORDER_AMOUNT}(orderId, seller);
        
        vm.prank(buyer);
        escrow.markDelivered(orderId);
        
        vm.prank(seller);
        vm.expectRevert("Only buyer can release funds");
        escrow.release(orderId);
    }

    function testOnlySellerCanRefund() public {
        vm.prank(buyer);
        escrow.createOrder{value: ORDER_AMOUNT}(orderId, seller);
        
        vm.prank(buyer);
        vm.expectRevert("Only seller can refund");
        escrow.refund(orderId);
    }
}
