package com.agrichain.payment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthGasPrice;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;
import org.web3j.protocol.core.methods.response.EthGetTransactionReceipt;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.utils.Convert;
import org.web3j.utils.Numeric;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final Web3j web3j;
    private final String escrowContractAddress;

    @Value("${web3.private-key:}")
    private String privateKey;

    @Value("${web3.seller.address:0x0000000000000000000000000000000000000000}")
    private String sellerAddress;

    public String processPayment(String orderId, double amount) {
        log.info("Processing payment for order: {} with amount: {}", orderId, amount);

        if (escrowContractAddress == null || escrowContractAddress.isBlank() || escrowContractAddress.endsWith("0000000000000000000000000000000000000000")) {
            throw new IllegalStateException("Escrow contract address is not configured");
        }
        if (privateKey == null || privateKey.isBlank()) {
            throw new IllegalStateException("web3.private-key is not configured");
        }
        if (sellerAddress == null || sellerAddress.isBlank() || sellerAddress.endsWith("0000000000000000000000000000000000000000")) {
            throw new IllegalStateException("web3.seller.address is not configured");
        }

        try {
            Credentials credentials = Credentials.create(privateKey);

            // Convert amount (ETH) to Wei
            BigInteger valueWei = Convert.toWei(BigDecimal.valueOf(amount), Convert.Unit.ETHER).toBigIntegerExact();

            // Get nonce
            EthGetTransactionCount txCountResponse = web3j.ethGetTransactionCount(
                    credentials.getAddress(), DefaultBlockParameterName.LATEST).send();
            BigInteger nonce = txCountResponse.getTransactionCount();

            // Gas price and limit
            EthGasPrice gasPriceResponse = web3j.ethGasPrice().send();
            BigInteger gasPrice = gasPriceResponse.getGasPrice();
            BigInteger gasLimit = BigInteger.valueOf(300_000L);

            // Prepare createOrder(orderId, seller) call data
            byte[] orderIdHash = Numeric.hexStringToByteArray(org.web3j.crypto.Hash.sha3String(orderId));
            Bytes32 orderIdBytes32 = new Bytes32(orderIdHash);
            Function function = new Function(
                    "createOrder",
                    Arrays.asList(orderIdBytes32, new Address(sellerAddress)),
                    Collections.emptyList()
            );
            String encodedFunction = FunctionEncoder.encode(function);

            RawTransaction rawTransaction = RawTransaction.createTransaction(
                    nonce,
                    gasPrice,
                    gasLimit,
                    escrowContractAddress,
                    valueWei,
                    encodedFunction
            );

            byte[] signedMessage = TransactionEncoder.signMessage(rawTransaction, 11155111L, credentials);
            String hexValue = Numeric.toHexString(signedMessage);

            EthSendTransaction sendResponse = web3j.ethSendRawTransaction(hexValue).send();
            if (sendResponse.hasError()) {
                throw new IllegalStateException("Error sending transaction: " + sendResponse.getError().getMessage());
            }

            String transactionHash = sendResponse.getTransactionHash();
            log.info("Escrow payment transaction submitted: {}", transactionHash);
            return transactionHash;
        } catch (Exception e) {
            log.error("Failed to process payment on-chain", e);
            throw new RuntimeException("Failed to process payment", e);
        }
    }

    public boolean verifyPayment(String transactionHash) {
        log.info("Verifying payment transaction: {}", transactionHash);

        try {
            EthGetTransactionReceipt receiptResponse = web3j.ethGetTransactionReceipt(transactionHash).send();
            Optional<TransactionReceipt> receiptOpt = receiptResponse.getTransactionReceipt();

            if (receiptOpt.isEmpty()) {
                log.info("No transaction receipt found yet for hash: {}", transactionHash);
                return false;
            }

            TransactionReceipt receipt = receiptOpt.get();
            boolean success = receipt.isStatusOK();
            log.info("Transaction {} success status: {}", transactionHash, success);
            return success;
        } catch (Exception e) {
            log.error("Error verifying payment transaction", e);
            return false;
        }
    }
}
