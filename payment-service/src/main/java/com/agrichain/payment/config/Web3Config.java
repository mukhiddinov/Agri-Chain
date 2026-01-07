package com.agrichain.payment.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;

@Configuration
@Slf4j
public class Web3Config {

    @Value("${web3.rpc.url:https://sepolia.infura.io/v3/your-project-id}")
    private String rpcUrl;

    @Value("${web3.contract.address:0x0000000000000000000000000000000000000000}")
    private String contractAddress;

    @Bean
    public Web3j web3j() {
        log.info("Initializing Web3j with RPC URL: {}", rpcUrl);
        return Web3j.build(new HttpService(rpcUrl));
    }

    @Bean
    public String escrowContractAddress() {
        log.info("Using Escrow contract address: {}", contractAddress);
        return contractAddress;
    }
}
